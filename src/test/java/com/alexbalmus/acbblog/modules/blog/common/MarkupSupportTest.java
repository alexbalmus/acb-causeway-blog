package com.alexbalmus.acbblog.modules.blog.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkupSupportTest
{
    @Test
    void blankTextRendersAsEmptyMarkup()
    {
        assertThat(MarkupSupport.toHtmlParagraphs(null).html()).isEmpty();
        assertThat(MarkupSupport.toHtmlParagraphs("  ").html()).isEmpty();
    }

    @Test
    void blankLinesSeparateParagraphs()
    {
        String html = MarkupSupport.toHtmlParagraphs("First paragraph.\n\nSecond paragraph.").html();
        assertThat(html).contains("<p>First paragraph.</p><p>Second paragraph.</p>");
    }

    @Test
    void singleLineBreaksBecomeBrTags()
    {
        String html = MarkupSupport.toHtmlParagraphs("line one\nline two").html();
        assertThat(html).contains("<p>line one<br/>line two</p>");
    }

    @Test
    void htmlIsEscaped()
    {
        String html = MarkupSupport.toHtmlParagraphs("<script>alert(\"x\")</script> & more").html();
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; more");
    }

    @Test
    void escapeHandlesAllSpecialCharacters()
    {
        assertThat(MarkupSupport.escape("a & b < c > d \" e")).isEqualTo("a &amp; b &lt; c &gt; d &quot; e");
    }
}
