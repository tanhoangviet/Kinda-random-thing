package com.example.ui.editor

data class LuauStyleDiagnostic(
    val line: Int,
    val severity: String,
    val message: String
)

data class LuauStyleCheckResult(
    val ok: Boolean,
    val summary: String,
    val diagnostics: List<LuauStyleDiagnostic>
)

fun runStyluaStyleCheck(source: String): LuauStyleCheckResult {
    val diagnostics = mutableListOf<LuauStyleDiagnostic>()
    val lines = source.lines()

    if (lines.firstOrNull { it.isNotBlank() }?.trim() != "--!strict") {
        diagnostics += LuauStyleDiagnostic(
            line = 1,
            severity = "warn",
            message = "Add --!strict as the first non-empty line for typed Luau."
        )
    }

    lines.forEachIndexed { index, line ->
        val number = index + 1
        if (line.endsWith(" ") || line.endsWith("\t")) {
            diagnostics += LuauStyleDiagnostic(number, "style", "Trailing whitespace; StyLua removes it.")
        }
        if (line.length > 120) {
            diagnostics += LuauStyleDiagnostic(number, "style", "Line is longer than 120 characters.")
        }

        val trimmed = line.trimStart()
        if (!trimmed.startsWith("--")) {
            listOf("wait", "spawn", "delay").forEach { api ->
                if (Regex("""(?<![\w.])$api\s*\(""").containsMatchIn(trimmed)) {
                    diagnostics += LuauStyleDiagnostic(
                        number,
                        "luau",
                        "Use task.$api(...) instead of global $api(...)."
                    )
                }
            }
        }
    }

    val blockResult = estimateLuauBlockBalance(lines)
    if (blockResult != null) {
        diagnostics += blockResult
    }

    val ok = diagnostics.isEmpty()
    val summary = if (diagnostics.isEmpty()) {
        "StyLua check clean"
    } else {
        "${diagnostics.size} issue${if (diagnostics.size == 1) "" else "s"} found"
    }
    return LuauStyleCheckResult(ok = ok, summary = summary, diagnostics = diagnostics.take(24))
}

private fun estimateLuauBlockBalance(lines: List<String>): LuauStyleDiagnostic? {
    var openBlocks = 0
    var repeatBlocks = 0
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.substringBefore("--").trim()
        if (line.isBlank()) return@forEachIndexed

        val endCount = Regex("""\bend\b""").findAll(line).count()
        val untilCount = Regex("""\buntil\b""").findAll(line).count()
        val openCount =
            Regex("""\bfunction\b""").findAll(line).count() +
                Regex("""\bthen\b""").findAll(line).count() +
                Regex("""(?<![\w])do\b""").findAll(line).count()
        val repeatCount = Regex("""\brepeat\b""").findAll(line).count()

        openBlocks += openCount - endCount
        repeatBlocks += repeatCount - untilCount

        if (openBlocks < 0 || repeatBlocks < 0) {
            return LuauStyleDiagnostic(
                line = index + 1,
                severity = "error",
                message = "Block closes before it opens; check end/until structure."
            )
        }
    }

    return when {
        openBlocks > 0 -> LuauStyleDiagnostic(
            line = lines.size.coerceAtLeast(1),
            severity = "error",
            message = "Missing end for one or more function/if/do blocks."
        )
        repeatBlocks > 0 -> LuauStyleDiagnostic(
            line = lines.size.coerceAtLeast(1),
            severity = "error",
            message = "Missing until for one or more repeat blocks."
        )
        else -> null
    }
}
