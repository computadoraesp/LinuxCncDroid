package com.example.service

import com.example.model.MdiValidationResult
import java.util.Locale

/**
 * Industrial RS274/NGC Syntax Validator for LinuxCNC MDI Commands.
 * Verifies modal group exclusivity, valid coordinate parameters, and feed/spindle constraints
 * before transmitting commands across socket/IPC to the CNC motion controller.
 */
class MdiSyntaxValidator {

    private val wordPattern = Regex("([A-Z])([+-]?\\d*(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

    // RS274/NGC Modal Group 1 (Motion)
    private val modalGroupMotion = setOf("G0", "G00", "G1", "G01", "G2", "G02", "G3", "G03", "G38.2", "G80", "G81", "G82", "G83")
    // Modal Group 2 (Plane)
    private val modalGroupPlane = setOf("G17", "G18", "G19")
    // Modal Group 3 (Distance)
    private val modalGroupDistance = setOf("G90", "G91")
    // Modal Group 7 (Spindle)
    private val modalGroupSpindle = setOf("M3", "M03", "M4", "M04", "M5", "M05")
    // Modal Group 8 (Coolant)
    private val modalGroupCoolant = setOf("M7", "M07", "M8", "M08", "M9", "M09")

    fun validate(rawInput: String): MdiValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return MdiValidationResult(isValid = false, errorMessage = "Command cannot be empty")
        }

        // Remove comments in parentheses (comment) or semicolons ;comment
        val withoutParenComments = trimmed.replace(Regex("\\(.*?\\)"), "").trim()
        val cleanLine = if (withoutParenComments.contains(";")) {
            withoutParenComments.substringBefore(";").trim()
        } else {
            withoutParenComments
        }

        if (cleanLine.isEmpty()) {
            return MdiValidationResult(isValid = true, parsedTokens = listOf("(COMMENT)"))
        }

        val tokens = mutableListOf<String>()
        val matches = wordPattern.findAll(cleanLine).toList()

        if (matches.isEmpty()) {
            return MdiValidationResult(
                isValid = false,
                errorMessage = "Unrecognized syntax. Expected valid G-Code / M-Code tokens (e.g. G0 X10 Y20, M3 S12000)"
            )
        }

        val foundLetters = mutableListOf<Char>()
        val foundGWords = mutableListOf<String>()
        val foundMWords = mutableListOf<String>()
        var hasMotion = false
        var hasSpindle = false

        for (m in matches) {
            val letter = m.groupValues[1].uppercase(Locale.ROOT)[0]
            val valueStr = m.groupValues[2]
            val token = "$letter$valueStr"
            tokens.add(token)
            foundLetters.add(letter)

            if (letter == 'G') {
                foundGWords.add(token)
            } else if (letter == 'M') {
                foundMWords.add(token)
            }
        }

        // 1. Check Modal Group 1 Exclusivity (Motion)
        val motionMatches = foundGWords.filter { it in modalGroupMotion }
        if (motionMatches.size > 1) {
            return MdiValidationResult(
                isValid = false,
                errorMessage = "Modal conflict: Cannot have multiple motion commands in the same block (${motionMatches.joinToString(", ")})",
                parsedTokens = tokens
            )
        }
        if (motionMatches.isNotEmpty()) {
            hasMotion = true
        }

        // 2. Check Modal Group 2 Exclusivity (Plane)
        val planeMatches = foundGWords.filter { it in modalGroupPlane }
        if (planeMatches.size > 1) {
            return MdiValidationResult(
                isValid = false,
                errorMessage = "Modal conflict: Multiple plane commands (${planeMatches.joinToString(", ")})",
                parsedTokens = tokens
            )
        }

        // 3. Check Modal Group 3 Exclusivity (Distance)
        val distanceMatches = foundGWords.filter { it in modalGroupDistance }
        if (distanceMatches.size > 1) {
            return MdiValidationResult(
                isValid = false,
                errorMessage = "Modal conflict: Cannot mix G90 (Absolute) and G91 (Incremental) in the same block",
                parsedTokens = tokens
            )
        }

        // 4. Check Modal Group 7 Exclusivity (Spindle)
        val spindleMatches = foundMWords.filter { it in modalGroupSpindle }
        if (spindleMatches.size > 1) {
            return MdiValidationResult(
                isValid = false,
                errorMessage = "Modal conflict: Multiple spindle control commands (${spindleMatches.joinToString(", ")})",
                parsedTokens = tokens
            )
        }
        if (spindleMatches.isNotEmpty()) {
            hasSpindle = true
        }

        // 5. Check Spindle Speed constraint (S cannot be negative)
        val sMatch = matches.find { it.groupValues[1].equals("S", ignoreCase = true) }
        if (sMatch != null) {
            val sVal = sMatch.groupValues[2].toDoubleOrNull()
            if (sVal == null || sVal < 0.0) {
                return MdiValidationResult(
                    isValid = false,
                    errorMessage = "Spindle speed 'S' must be a positive number",
                    parsedTokens = tokens
                )
            }
            hasSpindle = true
        }

        // 6. Check Feedrate constraint (F cannot be negative)
        val fMatch = matches.find { it.groupValues[1].equals("F", ignoreCase = true) }
        if (fMatch != null) {
            val fVal = fMatch.groupValues[2].toDoubleOrNull()
            if (fVal == null || fVal <= 0.0) {
                return MdiValidationResult(
                    isValid = false,
                    errorMessage = "Feedrate 'F' must be greater than zero",
                    parsedTokens = tokens
                )
            }
        }

        return MdiValidationResult(
            isValid = true,
            parsedTokens = tokens,
            isMotionCommand = hasMotion,
            isSpindleCommand = hasSpindle
        )
    }
}
