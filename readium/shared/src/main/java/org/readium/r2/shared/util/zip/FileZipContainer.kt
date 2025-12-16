/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.zip

import java.io.File
import java.io.IOException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.enums.CompressionMethod
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.archive.ArchiveProperties
import org.readium.r2.shared.util.archive.archive
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.filename
import org.readium.r2.shared.util.toUrl

internal class FileZipContainer(
    private val archive: ZipFile,
    file: File,
) : Container<Resource> {

    private inner class Entry(private val url: Url,  private val header: FileHeader) :
        Resource {

        override val sourceUrl: AbsoluteUrl? = null

        override suspend fun properties(): Try<Resource.Properties, ReadError> =
            Try.success(
                Resource.Properties {
                    filename = url.filename
                    archive = ArchiveProperties(
                        entryLength = compressedLength
                            ?: length().getOrElse { return Try.failure(it) },
                        isEntryCompressed = compressedLength != null
                    )
                }
            )

        override suspend fun length(): Try<Long, ReadError> =
            header.uncompressedSize
                .takeUnless { it < 0 }
                ?.let { Try.success(it) }
                ?: Try.failure(
                    ReadError.UnsupportedOperation(
                        DebugError("ZIP entry doesn't provide length for entry $url.")
                    )
                )

        private val compressedLength: Long? =
            try {
                val method = header.compressionMethod ?: header.compressionMethod // getter
                // 如果是 STORE（不压缩），则视为未压缩；否则尝试返回 compressedSize
                if (method == CompressionMethod.STORE) {
                    null
                } else {
                    header.compressedSize.takeUnless { it <= 0 || it == header.uncompressedSize }
                }
            } catch (t: Throwable) {
                // 保险回退：如果无法判断，就根据 compressedSize/uncompressedSize 做简单判断
                header.compressedSize.takeUnless { it <= 0 || it == header.uncompressedSize }
            }

        override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> =
            try {
                withContext(Dispatchers.IO) {
                    val bytes =
                        if (range == null) {
                            readFully()
                        } else {
                            readRange(range)
                        }
                    Try.success(bytes)
                }
            } catch (e: ZipException) {
                Try.failure(ReadError.Decoding(e))
            } catch (e: IOException) {
                Try.failure(ReadError.Access(FileSystemError.IO(e)))
            }

        private suspend fun readFully(): ByteArray =
            withContext(Dispatchers.IO) {
                archive.getInputStream(header)
                    .use {
                        it.readFully()
                    }
            }

        private fun readRange(range: LongRange): ByteArray {
            val start = range.first
            val endExclusive = range.last + 1
            return stream(start).readRange(start until endExclusive)
        }
        /**
         * Reading an entry in chunks (e.g. from the HTTP server) can be really slow if the entry
         * is deflated in the archive, because we can't jump to an arbitrary offset in a deflated
         * stream. This means that we need to read from the start of the entry for each chunk.
         *
         * To alleviate this issue, we cache a stream which will be reused as long as the chunks are
         * requested in order.
         *
         * See this issue for more info: https://github.com/readium/r2-shared-kotlin/issues/129
         */
        private fun stream(fromIndex: Long): CountingInputStream {
            // Reuse the current stream if it didn't exceed the requested index.
            stream
                ?.takeIf { it.count <= fromIndex }
                ?.let { return it }

            stream?.close()

            return CountingInputStream(archive.getInputStream(header))
                .also { stream = it }
        }

        private var stream: CountingInputStream? = null

        @OptIn(DelicateCoroutinesApi::class)
        override fun close() {
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    tryOrLog { stream?.close() }
                }
            }
        }
    }

    override val sourceUrl: AbsoluteUrl = file.toUrl()

    override val entries: Set<Url> =
        tryOrLog { archive.fileHeaders  }
            .orEmpty()
            .filterNot { it.isDirectory }
            .mapNotNull { header -> Url.fromDecodedPath(header.fileName) }
            .toSet()

    override fun get(url: Url): Resource? =
        (url as? RelativeUrl)?.path
            ?.let {
                tryOrLog { archive.getFileHeader(it) }
            }
            ?.let { Entry(url, it) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun close() {
        GlobalScope.launch {
            tryOrLog {
                withContext(Dispatchers.IO) {
                    archive.close()
                }
            }
        }
    }
}
