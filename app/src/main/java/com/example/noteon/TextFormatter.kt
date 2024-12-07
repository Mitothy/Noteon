package com.example.noteon

import android.widget.EditText

class TextFormatter(private val editText: EditText) {

    fun toggleBold() {
        wrapSelection("**")
    }

    fun toggleItalic() {
        wrapSelection("*")
    }

    fun toggleStrikethrough() {
        wrapSelection("~~")
    }

    fun toggleUnderline() {
        wrapSelection("__")
    }

    fun addBulletPoint() {
        val start = editText.selectionStart
        val text = editText.text.toString()
        val lineStart = findLineStart(text, start)

        if (!text.substring(lineStart).startsWith("* ")) {
            editText.text.insert(lineStart, "* ")
        }
    }

    fun addQuote() {
        val start = editText.selectionStart
        val text = editText.text.toString()
        val lineStart = findLineStart(text, start)

        if (!text.substring(lineStart).startsWith("> ")) {
            editText.text.insert(lineStart, "> ")
        }
    }

    fun addHeading() {
        val start = editText.selectionStart
        val text = editText.text.toString()
        val lineStart = findLineStart(text, start)

        val line = text.substring(lineStart).takeWhile { it != '\n' }
        val headingLevel = line.takeWhile { it == '#' }.length

        when {
            headingLevel == 0 -> editText.text.insert(lineStart, "# ")
            headingLevel < 3 -> {
                // Add one more '#'
                editText.text.insert(lineStart + headingLevel, "#")
            }
            else -> {
                // Reset to level 1
                val endOfHashes = lineStart + headingLevel
                editText.text.replace(lineStart, endOfHashes, "#")
                if (editText.text.getOrNull(lineStart + 1)?.isWhitespace() == false) {
                    editText.text.insert(lineStart + 1, " ")
                }
            }
        }
    }

    fun addNumberedList() {
        val start = editText.selectionStart
        val text = editText.text
        val fullText = text.toString()
        val lineStart = findLineStart(fullText, start)
        val lineEnd = fullText.indexOf('\n', lineStart).let { if (it == -1) fullText.length else it }
        val line = fullText.substring(lineStart, lineEnd)

        val match = Regex("^(\\d+)\\.\\s").find(line)
        if (match == null) {
            // No existing number, start with "1. "
            text.insert(lineStart, "1. ")
        } else {
            // Increment the existing number
            val oldNumber = match.groupValues[1].toInt()
            val newNumber = oldNumber + 1
            val oldNumberStr = "$oldNumber. "
            val newNumberStr = "$newNumber. "
            text.replace(lineStart, lineStart + oldNumberStr.length, newNumberStr)
        }
    }

    private fun wrapSelection(wrapper: String) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text

        if (start == end) {
            // No selection, just insert the wrappers
            text.insert(start, wrapper + wrapper)
            editText.setSelection(start + wrapper.length)
        } else {
            val beforeSelection = text.substring(maxOf(0, start - wrapper.length), start)
            val afterSelection = text.substring(end, minOf(text.length, end + wrapper.length))

            val isAlreadyWrapped = beforeSelection == wrapper && afterSelection == wrapper
            if (isAlreadyWrapped) {
                // Remove the wrappers
                text.delete(end, end + wrapper.length)
                text.delete(start - wrapper.length, start)
                editText.setSelection(start - wrapper.length, end - wrapper.length)
            } else {
                // Add the wrappers
                text.insert(end, wrapper)
                text.insert(start, wrapper)
                editText.setSelection(start + wrapper.length, end + wrapper.length)
            }
        }
    }

    private fun findLineStart(text: String, start: Int): Int {
        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        return lineStart
    }
}