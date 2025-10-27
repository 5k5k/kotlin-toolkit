/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    if (name != "test-app") {
        apply(plugin = "org.jetbrains.dokka")
    }
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        android.set(true)
    }
}

tasks.register("cleanDocs", Delete::class).configure {
    delete("${project.rootDir}/docs/readium", "${project.rootDir}/docs/index.md", "${project.rootDir}/site")
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(false)
            skipEmptyPackages.set(false)
            skipDeprecated.set(true)
        }
    }
}

tasks.named<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaGfmMultiModule").configure {
    outputDirectory.set(file("${projectDir.path}/docs"))
}

//tasks.withType<Jar>().configureEach {
//    if (name.contains("javadoc", ignoreCase = true)) {
//        enabled = false
//    }
//}
//
//afterEvaluate {
//    tasks.findByName("javaDocReleaseJar")?.enabled = false
////    tasks.findByName("signMavenPublication")?.enabled = false
//}
//
//
//tasks.withType<Sign>().configureEach {
//    enabled = false
//}

subprojects {
    afterEvaluate {
        // 禁用所有 javadoc 任务
        tasks.matching { it.name.lowercase().contains("javadoc") }.configureEach {
            (this as org.gradle.api.Task).enabled = false
        }

        // 检查是否存在签名 key
        val hasSigningKey =
            project.hasProperty("signing.keyId") || project.hasProperty("signing.key")

        if (!hasSigningKey) {
            println("⚠️ [${project.name}] Disable signing for JitPack (no key).")
            tasks.matching { it.name.lowercase().contains("sign") }.configureEach {
                (this as org.gradle.api.Task).enabled = false
            }
        }
    }
}
