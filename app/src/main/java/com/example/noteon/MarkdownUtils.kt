package com.example.noteon

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.TextView

class MarkdownUtils {
    companion object {
        // Regex patterns for block-level elements
        private val PATTERN_HEADING = Regex("^(#{1,3})\\s+(.*?)$", RegexOption.MULTILINE)
        private val PATTERN_BULLET = Regex("^\\*\\s+(.*?)$", RegexOption.MULTILINE)
        private val PATTERN_QUOTE = Regex("^>\\s+(.*?)$", RegexOption.MULTILINE)
        private val PATTERN_ORDERED_LIST = Regex("^(\\d+)\\.\\s+(.*?)$", RegexOption.MULTILINE)

        // Regex patterns for inline elements
        private val PATTERN_BOLD = Regex("\\*\\*(.*?)\\*\\*")
        private val PATTERN_ITALIC = Regex("(?<!\\*)\\*(?!\\*)(.*?)\\*(?!\\*)")
        private val PATTERN_STRIKETHROUGH = Regex("~~(.*?)~~")
        private val PATTERN_UNDERLINE = Regex("__(.*?)__")

        fun render(textView: TextView, markdown: String) {
            val builder = SpannableStringBuilder()

            val lines = markdown.split("\n")
            var inOrderedList = false
            var currentListNumber = 0

            // Process block-level formatting line by line
            for ((index, line) in lines.withIndex()) {
                // Heading?
                val headingMatch = PATTERN_HEADING.matchEntire(line)
                if (headingMatch != null) {
                    val (hashes, content) = headingMatch.destructured
                    appendHeading(builder, content, hashes.length)
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                }

                // Bullet?
                val bulletMatch = PATTERN_BULLET.matchEntire(line)
                if (bulletMatch != null) {
                    val (content) = bulletMatch.destructured
                    appendBullet(builder, content)
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                }

                // Quote?
                val quoteMatch = PATTERN_QUOTE.matchEntire(line)
                if (quoteMatch != null) {
                    val (content) = quoteMatch.destructured
                    appendQuote(builder, content)
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                }

                // Numbered list?
                val orderedMatch = PATTERN_ORDERED_LIST.matchEntire(line)
                if (orderedMatch != null) {
                    val (_, content) = orderedMatch.destructured
                    // If already in a list, increment the number
                    if (inOrderedList) {
                        currentListNumber += 1
                    } else {
                        // Starting a new ordered list
                        currentListNumber = 1
                        inOrderedList = true
                    }
                    appendNumberedList(builder, currentListNumber, content)
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                } else {
                    // Not an ordered list line; reset state
                    inOrderedList = false
                }

                // Normal line
                builder.append(line)
                if (index < lines.size - 1) builder.append("\n")
            }

            applyInlineFormatting(builder)

            textView.text = builder
        }

        fun renderPreview(textView: TextView, markdown: String) {
            val builder = SpannableStringBuilder()

            val lines = markdown.split("\n")
            var inOrderedList = false
            var currentListNumber = 0

            for ((index, line) in lines.withIndex()) {
                val headingMatch = PATTERN_HEADING.matchEntire(line)
                if (headingMatch != null) {
                    val (_, content) = headingMatch.destructured
                    appendHeadingPreview(builder, content)
                    inOrderedList = false
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                }

                val bulletMatch = PATTERN_BULLET.matchEntire(line)
                if (bulletMatch != null) {
                    val (content) = bulletMatch.destructured
                    appendBullet(builder, content)
                    inOrderedList = false
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                }

                val quoteMatch = PATTERN_QUOTE.matchEntire(line)
                if (quoteMatch != null) {
                    val (content) = quoteMatch.destructured
                    appendQuote(builder, content)
                    inOrderedList = false
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                }

                val orderedMatch = PATTERN_ORDERED_LIST.matchEntire(line)
                if (orderedMatch != null) {
                    val (_, content) = orderedMatch.destructured
                    if (inOrderedList) {
                        currentListNumber += 1
                    } else {
                        currentListNumber = 1
                        inOrderedList = true
                    }
                    appendNumberedList(builder, currentListNumber, content)
                    if (index < lines.size - 1) builder.append("\n")
                    continue
                } else {
                    inOrderedList = false
                }

                builder.append(line)
                if (index < lines.size - 1) builder.append("\n")
            }

            applyInlineFormatting(builder)
            textView.text = builder
        }

        fun stripMarkdown(text: String): String {
            var result = text

            // Headings
            result = PATTERN_HEADING.replace(result) { matchResult ->
                matchResult.groupValues[2] + "\n"
            }

            // Bullets
            result = PATTERN_BULLET.replace(result) { matchResult ->
                matchResult.groupValues[1] + "\n"
            }

            // Quotes
            result = PATTERN_QUOTE.replace(result) { matchResult ->
                matchResult.groupValues[1] + "\n"
            }

            // Ordered lists
            result = PATTERN_ORDERED_LIST.replace(result) { matchResult ->
                matchResult.groupValues[2] + "\n"
            }

            // Inline formats
            result = PATTERN_BOLD.replace(result) { it.groupValues[1] }
            result = PATTERN_ITALIC.replace(result) { it.groupValues[1] }
            result = PATTERN_STRIKETHROUGH.replace(result) { it.groupValues[1] }
            result = PATTERN_UNDERLINE.replace(result) { it.groupValues[1] }

            // Clean extra newlines
            result = result.replace(Regex("\n{3,}"), "\n\n")

            return result.trim()
        }

        private fun appendHeading(builder: SpannableStringBuilder, content: String, level: Int) {
            val start = builder.length
            builder.append(content)
            val end = builder.length

            val size = when (level) {
                1 -> 1.5f
                2 -> 1.3f
                else -> 1.1f
            }

            builder.setSpan(RelativeSizeSpan(size), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun appendHeadingPreview(builder: SpannableStringBuilder, content: String) {
            val start = builder.length
            builder.append(content)
            val end = builder.length
            // Only apply bold, no size change
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun appendBullet(builder: SpannableStringBuilder, content: String) {
            val start = builder.length
            builder.append(content)
            val end = builder.length
            builder.setSpan(LeadingMarginSpan.Standard(20), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(BulletSpan(20), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun appendQuote(builder: SpannableStringBuilder, content: String) {
            val start = builder.length
            builder.append(content)
            val end = builder.length
            builder.setSpan(QuoteSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun appendNumberedList(builder: SpannableStringBuilder, number: Int, content: String) {
            val start = builder.length
            builder.append("$number. $content")
            val end = builder.length
            builder.setSpan(LeadingMarginSpan.Standard(20), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun applyInlineFormatting(builder: SpannableStringBuilder) {
            applySpanWithRemoval(builder, PATTERN_STRIKETHROUGH) { StrikethroughSpan() }
            applySpanWithRemoval(builder, PATTERN_UNDERLINE) { UnderlineSpan() }
            applySpanWithRemoval(builder, PATTERN_BOLD) { StyleSpan(Typeface.BOLD) }
            applySpanWithRemoval(builder, PATTERN_ITALIC) { StyleSpan(Typeface.ITALIC) }
        }

        private fun applySpanWithRemoval(
            builder: SpannableStringBuilder,
            pattern: Regex,
            spanCreator: () -> Any
        ) {
            val matches = pattern.findAll(builder).toList().reversed()

            for (match in matches) {
                val fullStart = match.range.first
                val fullEnd = match.range.last + 1
                val innerText = match.groups[1] ?: continue

                val innerStart = innerText.range.first
                val innerEnd = innerText.range.last + 1

                val leadingRemoval = innerStart - fullStart
                val trailingRemoval = fullEnd - innerEnd

                // Remove trailing and leading markdowns
                builder.delete(fullEnd - trailingRemoval, fullEnd)
                builder.delete(fullStart, fullStart + leadingRemoval)

                builder.setSpan(
                    spanCreator(),
                    fullStart,
                    fullStart + (innerEnd - innerStart),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}