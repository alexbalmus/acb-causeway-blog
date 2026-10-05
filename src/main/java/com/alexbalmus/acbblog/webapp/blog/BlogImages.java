package com.alexbalmus.acbblog.webapp.blog;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class BlogImages {
    private final long maxBytes;
    private final long maxPixels;
    public BlogImages(@Value("${acb.blog.max-image-bytes:5242880}") long maxBytes,
            @Value("${acb.blog.max-image-pixels:20000000}") long maxPixels) {
        this.maxBytes = maxBytes;
        this.maxPixels = maxPixels;
    }
    public BufferedImage decode(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > maxBytes) throw invalid("Picture exceeds the upload size limit.");
        try (var input = file.getInputStream(); var images = ImageIO.createImageInputStream(input)) {
            if (images == null) throw invalid("Could not read the selected picture.");
            var readers = ImageIO.getImageReaders(images);
            if (!readers.hasNext()) throw invalid("Choose a supported image, such as PNG or JPEG.");
            var reader = readers.next();
            try {
                reader.setInput(images, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > maxPixels)
                    throw invalid("Picture dimensions exceed the image limit.");
                var image = reader.read(0);
                if (image == null) throw invalid("Could not read the selected picture.");
                return image;
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException ex) {
            throw invalid("Could not read the selected picture.");
        }
    }
    private BlogFormException invalid(String message) { return new BlogFormException("picture", message); }
}
