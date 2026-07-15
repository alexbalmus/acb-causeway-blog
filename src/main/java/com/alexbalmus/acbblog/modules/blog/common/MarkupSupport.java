package com.alexbalmus.acbblog.modules.blog.common;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;

import org.apache.causeway.applib.value.Markup;

public final class MarkupSupport
{
    private MarkupSupport() {}

    /**
     * Renders plain text as HTML paragraphs: blank lines separate paragraphs,
     * single line breaks become {@code <br/>}. The text is HTML-escaped.
     */
    public static Markup toHtmlParagraphs(final String text)
    {
        if (Strings.isBlank(text))
        {
            return Markup.valueOf("");
        }

        String paragraphs = Arrays.stream(escape(text.trim()).split("\\R{2,}"))
            .map(paragraph -> "<p>" + paragraph.replaceAll("\\R", "<br/>") + "</p>")
            .collect(Collectors.joining());

        return Markup.valueOf("<div class=\"acb-post-content\">" + paragraphs + "</div>");
    }

    public static String escape(final String text)
    {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
