package com.alexbalmus.acbblog.webapp;

import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.causeway.commons.io.JaxbUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CausewayXmlTest {
    @XmlRootElement
    public static class TextDto {
        public String content;
    }

    @Test void causewayXmlPreservesBrowserLineEndings() {
        var dto = new TextDto();
        dto.content = "First paragraph\r\n\r\nSecond\tparagraph & <text>";
        String xml = JaxbUtils.toStringUtf8(dto);
        assertThat(JaxbUtils.tryRead(TextDto.class, xml).ifFailureFail().getValue().orElseThrow().content)
                .isEqualTo(dto.content);
    }
}
