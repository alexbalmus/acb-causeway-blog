package com.alexbalmus.acbblog.modules.blog.common;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.causeway.applib.value.Blob;

public final class ImageSupport
{
    private static final int PREVIEW_MAX_WIDTH = 900;
    private static final int PREVIEW_MAX_HEIGHT = 520;

    private ImageSupport() {}

    public static Blob toPngBlob(final BufferedImage image, final String name)
    {
        if (image == null)
        {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            ImageIO.write(image, "png", output);
            return new Blob(name, "image/png", output.toByteArray());
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException("Could not read uploaded image", ex);
        }
    }

    public static BufferedImage boundedPreview(final Blob blob)
    {
        if (blob == null)
        {
            return null;
        }

        return blob.asImage()
            .map(image -> scaleToFit(image, PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT))
            .orElse(null);
    }

    private static BufferedImage scaleToFit(final BufferedImage image, final int maxWidth, final int maxHeight)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= maxWidth && height <= maxHeight)
        {
            return image;
        }

        double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        }
        finally
        {
            graphics.dispose();
        }
        return scaled;
    }
}
