/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import timber.log.Timber

/**
 * Injects the Readium CSS files and scripts in the HTML [Resource] receiver.
 *
 * @param baseHref Base URL where the Readium CSS and scripts are served.
 */
@OptIn(ExperimentalReadiumApi::class)
internal fun Resource.injectHtml(
    publication: Publication,
    mediaType: MediaType,
    css: ReadiumCss,
    baseHref: AbsoluteUrl,
    disableSelectionWhenProtected: Boolean,
    isLandscape: Boolean = false,
    doubleLeft: Boolean? = null,
    topMargin: Int = 0,
    bottomMargin: Int = 0
): Resource =
    TransformingResource(this) { bytes ->
        if (!mediaType.isHtml && mediaType != MediaType.SVG) {
            return@TransformingResource Try.success(bytes)
        }

        if (mediaType == MediaType.SVG) {
            var svgContent = bytes.toString(mediaType.charset ?: Charsets.UTF_8).trim()
            var content = svgContent
            if (!isLandscape) {
                var fixedSvg = content
                fixedSvg = fixedSvg.replace("""<\?xml[^>]*>""".toRegex(), "")
                content = """
            <html>
            <head>
            <meta charset="UTF-8"/>
            <title></title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>
            <body>
                $fixedSvg
            </body>
            </html>
        """.trimIndent()
            }
            return@TransformingResource  Try.success(content.toByteArray())
        }

        var content = bytes.toString(mediaType.charset ?: Charsets.UTF_8).trim()
        val injectables = mutableListOf<String>()

        if (publication.metadata.presentation.layout == EpubLayout.FIXED) {
            if (isLandscape) {
                css.userProperties.backgroundColor?.let {
                    injectables.add(
                        """
                            <style>
                                :root[style*="--USER__backgroundColor"] {
                                    background-color: var(--USER__backgroundColor) !important
                                }
                                
                                :root[style*="--USER__backgroundColor"] *{
                                    background-color: var(--USER__backgroundColor) !important
                                }
                            </style>
                        """
                    )
                }
            }
            injectables.add(
                script(baseHref.resolve(Url("readium/scripts/readium-fixed.js")!!))
            )
            if (isLandscape) {
                injectables.add(
                    script(baseHref.resolve(Url("readium/scripts/fixed_fix.js")!!))
                )
                doubleLeft?.let {
                    if (css.layout.readingProgression == ReadingProgression.LTR) {
                        if (!it) {
                            injectables.add(
                                script(baseHref.resolve(Url("readium/scripts/fixed_fix_left.js")!!))
                            )
                        } else {
                            injectables.add(
                                script(baseHref.resolve(Url("readium/scripts/fixed_fix_right.js")!!))
                            )
                        }
                    } else {
                        if (it) {
                            injectables.add(
                                script(baseHref.resolve(Url("readium/scripts/fixed_fix_left.js")!!))
                            )
                        } else {
                            injectables.add(
                                script(baseHref.resolve(Url("readium/scripts/fixed_fix_right.js")!!))
                            )
                        }
                    }
                }
            }
        } else {
            content = try {
                css.injectHtml(content)
            } catch (e: Exception) {
                return@TransformingResource Try.failure(ReadError.Decoding(e))
            }

            content = try {
                insertBodyPadding(content, topMargin, bottomMargin)
            } catch (e: Exception) {
                return@TransformingResource Try.failure(ReadError.Decoding(e))
            }

            injectables.add(
                script(
                    baseHref.resolve(Url("readium/scripts/readium-reflowable.js")!!)
                )
            )

            injectables.add(
                """
                <style>
                    @media (orientation: landscape) {
                        img {
                            max-height: calc(100vh - """ + (topMargin + bottomMargin) + """px) !important;
                            width: auto !important;
                        }
                    }
                </style>
            """
            )
        }

        // Disable the text selection if the publication is protected.
        // FIXME: This is a hack until proper LCP copy is implemented, see https://github.com/readium/kotlin-toolkit/issues/221
        if (disableSelectionWhenProtected && publication.isProtected) {
            injectables.add(
                """
                <style>
                *:not(input):not(textarea) {
                    user-select: none;
                    -webkit-user-select: none;
                }
                </style>
            """
            )
        }

        val headEndIndex = content.indexOf("</head>", 0, true)
        if (headEndIndex == -1) {
            Timber.e("</head> closing tag not found in resource with href: $sourceUrl")
        } else {
            content = StringBuilder(content)
                .insert(headEndIndex, "\n" + injectables.joinToString("\n") + "\n")
                .toString()
        }

        Try.success(content.toByteArray())
    }

private fun insertBodyPadding(html: String, topMargin: Int, bottomMargin: Int): String {
    val top = """<div style="height:""" + topMargin + """px;"></div>"""
    val bottom = """<div style="height:""" + bottomMargin + """px;"></div>"""

    var result = html

    result = result.replaceFirst(
        Regex("<body([^>]*)>"),
        "<body$1>$top"
    )

    result = result.replace(
        "</body>",
        "$bottom</body>"
    )

    return result
}

private fun script(src: Url): String =
    """<script type="text/javascript" src="$src"></script>"""
