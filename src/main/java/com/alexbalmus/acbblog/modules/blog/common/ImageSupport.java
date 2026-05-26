package com.alexbalmus.acbblog.modules.blog.common;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Markup;

public final class ImageSupport
{
    private static final int PREVIEW_MAX_WIDTH = 680;
    private static final int PREVIEW_MAX_HEIGHT = 385;

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

    public static Markup imagePreview(final Blob blob)
    {
        if (blob == null)
        {
            return Markup.valueOf("");
        }

        String mimeType = blob.mimeType().getBaseType();
        if (!mimeType.startsWith("image/"))
        {
            return Markup.valueOf("");
        }

        String encoded = Base64.getEncoder().encodeToString(blob.bytes());
        return Markup.valueOf(String.format(
            "<img alt=\"Post picture\" src=\"data:%s;base64,%s\" " +
                "style=\"display:block;width:100%%;max-width:%dpx;max-height:%dpx;" +
                "height:auto;object-fit:contain;margin:0 auto;\"/>",
            mimeType,
            encoded,
            PREVIEW_MAX_WIDTH,
            PREVIEW_MAX_HEIGHT));
    }

}
