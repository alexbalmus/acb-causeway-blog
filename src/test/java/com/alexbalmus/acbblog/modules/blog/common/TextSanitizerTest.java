package com.alexbalmus.acbblog.modules.blog.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSanitizerTest
{
    private final TextSanitizer sanitizer = new TextSanitizer();

    @Test
    void blankOrNullTextBecomesEmpty()
    {
        assertThat(sanitizer.sanitize(null)).isEmpty();
        assertThat(sanitizer.sanitize("")).isEmpty();
        assertThat(sanitizer.sanitize("   ")).isEmpty();
    }

    @Test
    void stripsControlCharacters()
    {
        // BEL (category Cc) and zero-width space (category Cf) are both \p{C}
        String bel = String.valueOf((char) 0x07);
        String zeroWidthSpace = String.valueOf((char) 0x200B);
        assertThat(sanitizer.sanitize("he" + bel + "llo wor" + zeroWidthSpace + "ld"))
            .isEqualTo("hello world");
    }

    @Test
    void preservesLineBreaksAndTabs()
    {
        String multiParagraph = "First paragraph.\n\nSecond paragraph.\r\n\tIndented line.";
        assertThat(sanitizer.sanitize(multiParagraph)).isEqualTo(multiParagraph);
    }

    @Test
    void trimsSurroundingWhitespace()
    {
        assertThat(sanitizer.sanitize("  hello  ")).isEqualTo("hello");
    }
}
