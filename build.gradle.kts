import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.intellij.platform.gradle.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val platformType = providers.gradleProperty("platformType").get()
val platformVersion = providers.gradleProperty("platformVersion").get()
val localPlatformPath = providers.gradleProperty("localPlatformPath").orNull
val verifierProxyUri = providers.gradleProperty("verifierProxy")
    .orElse(providers.environmentVariable("HTTPS_PROXY"))
    .orNull
    ?.let(URI::create)

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        if (localPlatformPath == null) {
            create(platformType, platformVersion)
        } else {
            local(localPlatformPath)
        }
        pluginVerifier()
    }

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = "242"
            untilBuild = "262.*"
        }

        vendor {
            name = "Project Directory Grouper Contributors"
        }
    }

    pluginVerification {
        ides {
            if (localPlatformPath == null) {
                create(platformType, platformVersion)
            } else {
                local(localPlatformPath)
            }
        }
    }
}

tasks {
    named<BuildSearchableOptionsTask>("buildSearchableOptions") {
        val searchableOptionsConfig = layout.buildDirectory.dir("searchableOptionsConfig")
        val searchableOptionsSystem = layout.buildDirectory.dir("searchableOptionsSystem")

        sandboxConfigDirectory.set(searchableOptionsConfig)
        sandboxSystemDirectory.set(searchableOptionsSystem)
        doFirst {
            searchableOptionsConfig.get().asFile.mkdirs()
            searchableOptionsSystem.get().asFile.mkdirs()
        }
    }

    named<VerifyPluginTask>("verifyPlugin") {
        failureLevel.set(
            listOf(
                FailureLevel.COMPATIBILITY_PROBLEMS,
                FailureLevel.OVERRIDE_ONLY_API_USAGES,
            ),
        )
        verifierProxyUri?.let { proxy ->
            val proxyPort = proxy.port.takeIf { it >= 0 } ?: 80
            jvmArgs(
                "-Dhttp.proxyHost=${proxy.host}",
                "-Dhttp.proxyPort=$proxyPort",
                "-Dhttps.proxyHost=${proxy.host}",
                "-Dhttps.proxyPort=$proxyPort",
            )
        }

        doLast {
            val reports = verificationReportsDirectory.get().asFile
                .walkTopDown()
                .filter { it.isFile && it.name == "report.html" }
                .toList()
            check(reports.isNotEmpty()) { "Plugin Verifier did not produce an HTML report" }

            val internalUsagePattern = Regex(
                """<div class="shortDescription">\s+(Internal (?!API usages)[^<]+)\s+<a href="#" class="detailsLink">details</a>""",
            )
            val allowedUsages = listOf(
                "Internal class com.intellij.ide.UpdatesInfoProviderManager reference",
                "Internal class com.intellij.ide.UpdatesInfoProviderManager.Companion reference",
                "Internal class com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetAction reference",
                "Internal constructor com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetAction" +
                    ".&lt;init&gt;() invocation",
                "Internal field com.intellij.ide.UpdatesInfoProviderManager.Companion access",
                "Internal interface com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable reference",
                "Internal method com.intellij.ide.RecentProjectListActionProvider.getActions(Project) invocation",
                "Internal method com.intellij.ide.RecentProjectListActionProvider" +
                    ".getActionsWithoutGroups(boolean, Project) invocation",
                "Internal method com.intellij.ide.UpdatesInfoProviderManager.Companion.getInstance() invocation",
                "Internal method com.intellij.ide.UpdatesInfoProviderManager.getUpdateActions() invocation",
                "Internal method com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetAction" +
                    ".createPopup(AnActionEvent) is overridden",
                "Internal method com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable" +
                    ".getBranchName() invocation",
                "Internal method com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable" +
                    ".getProjectIcon() invocation",
                "Internal method com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable" +
                    ".getProjectNameToDisplay() invocation",
                "Internal method com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable" +
                    ".getProjectPathToDisplay() invocation",
                "Internal method com.intellij.openapi.wm.impl.headertoolbar.ProjectToolbarWidgetPresentable" +
                    ".getProviderPathToDisplay() invocation",
            )

            reports.forEach { report ->
                val usages = internalUsagePattern.findAll(report.readText())
                    .map { match -> match.groupValues[1].trim() }
                    .toList()
                check(usages == allowedUsages) {
                    "Unexpected internal API usages in ${report.absolutePath}: $usages"
                }
            }
        }
    }

    withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_21.toString()
        targetCompatibility = JavaVersion.VERSION_21.toString()
        options.release.set(21)
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    test {
        useJUnitPlatform()
    }

    wrapper {
        gradleVersion = "9.6.1"
        distributionType = Wrapper.DistributionType.BIN
    }
}
