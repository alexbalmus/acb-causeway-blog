package com.alexbalmus.acbblog.modules.blog.ai;

import java.util.Optional;

import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import org.apache.causeway.applib.value.Blob;

import com.alexbalmus.acbblog.modules.blog.common.TextSanitizer;
import com.alexbalmus.acbblog.modules.blog.common.post.picture.PictureDescriptionGenerator;
import com.alexbalmus.acbblog.modules.blog.types.PictureDescription;

@Component
@Profile("Ai")
public class SpringAiPictureDescriptionGenerator implements PictureDescriptionGenerator
{
    private static final String PROMPT =
        """
        Generate a concise, relevant description for this blog post picture.
        Constraints:
        - one sentence
        - plain text only
        - no quotes
        - maximum %d characters
        - describe what is visible, without inventing context that is not evident
        - IMPORTANT: return only the description
        """.formatted(PictureDescription.MAX_LEN);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final TextSanitizer textSanitizer;

    public SpringAiPictureDescriptionGenerator(
        final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
        final TextSanitizer textSanitizer)
    {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.textSanitizer = textSanitizer;
    }

    @Override
    public Optional<String> generateFor(final Blob picture)
    {
        try
        {
            if (picture == null || picture.bytes() == null || picture.bytes().length == 0)
            {
                return Optional.empty();
            }

            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null)
            {
                return Optional.empty();
            }

            MimeType mimeType = MimeTypeUtils.parseMimeType(picture.mimeType().getBaseType());
            ByteArrayResource resource = new ByteArrayResource(picture.bytes())
            {
                @Override
                public String getFilename()
                {
                    return picture.name();
                }
            };

            String response = builder.build()
                .prompt()
                .user(user -> user
                    .text(PROMPT)
                    .media(mimeType, resource))
                .call()
                .content();

            String description = normalize(response);

            return Strings.isBlank(description) ? Optional.empty() : Optional.of(description);
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private String normalize(final String response)
    {
        String sanitized = textSanitizer.sanitize(response);
        if (Strings.isBlank(sanitized))
        {
            return "";
        }

        String description = sanitized.replaceAll("\\s+", " ").trim();
        if (description.length() <= PictureDescription.MAX_LEN)
        {
            return description;
        }

        return description.substring(0, PictureDescription.MAX_LEN).trim();
    }
}
