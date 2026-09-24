package com.alexbalmus.acbblog.webapp.blog;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class BlogImagesTest {
    @Test void validatesBytesAndDimensionsBeforeDecoding() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var upload = new MockMultipartFile("picture", "test.png", "image/png", bytes.toByteArray());
        assertThatThrownBy(() -> new BlogImages(1, 1000).decode(upload)).isInstanceOf(BlogFormException.class).hasMessageContaining("size");
        assertThatThrownBy(() -> new BlogImages(10000, 50).decode(upload)).isInstanceOf(BlogFormException.class).hasMessageContaining("dimensions");
        assertThat(new BlogImages(10000, 100).decode(upload).getWidth()).isEqualTo(10);
        assertThat(new BlogImages(10000, 100).decode(null)).isNull();
    }
}
