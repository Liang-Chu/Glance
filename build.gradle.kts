plugins {
    // AGP 9 carries Kotlin itself; a separate kotlin.android plugin is an error now.
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}

/**
 * CONSTRAINTS.md "C3 — Keep files readable: 500 lines is a gate" is a gate, so it
 * has to fail a build rather than be remembered. CONSTRAINTS.md owns the number;
 * the literal below is the enforcement of it, and the two move together.
 */
val fileMaxLines = 500

tasks.register("checkFileLength") {
    group = "verification"
    description = "Fails if any source file is longer than " + fileMaxLines + " lines (C3)."
    doLast {
        val offenders = fileTree("app/src") {
            include("**/*.kt", "**/*.java")
        }.files.map { it to it.readLines().size }
            .filter { (_, lines) -> lines > fileMaxLines }
            .map { (file, lines) -> file.relativeTo(projectDir).path + ": " + lines + " lines" }

        if (offenders.isNotEmpty()) {
            throw GradleException(
                "C3 breach — split these, or record a decision moving the number:\n  " +
                    offenders.joinToString("\n  ")
            )
        }
    }
}
