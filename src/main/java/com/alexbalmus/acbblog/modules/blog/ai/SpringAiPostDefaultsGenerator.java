package com.alexbalmus.acbblog.modules.blog.ai;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alexbalmus.acbblog.modules.blog.common.post.defaults.PostDefaults;
import com.alexbalmus.acbblog.modules.blog.common.post.defaults.PostDefaultsGenerator;
import com.alexbalmus.acbblog.modules.blog.common.TextSanitizer;

@Component
@Profile("Ai")
public class SpringAiPostDefaultsGenerator implements PostDefaultsGenerator
{
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Inject
    TextSanitizer textSanitizer;

    public SpringAiPostDefaultsGenerator(final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider)
    {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public Optional<PostDefaults> generateForJavaBlogPost()
    {
        try
        {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null)
            {
                return Optional.empty();
            }

            ChatClient chatClient = builder.build();

            final String titlePrompt =
                """
                Generate a concise title for a technical Java blog post.
                Constraints:
                - plain text only
                - no quotes
                - IMPORTANT: title must have maximum 50 characters
                - IMPORTANT: offer JUST ONE title, no confirmations, no alternatives, nothing else
                """;

            final String contentPrompt =
                """
                Generate content for a Java-related blog post having the title %s.
                Constraints:
                - audience: developers
                - 2 to 4 short paragraphs
                - practical and accurate
                - plain test only (no markdown headings, no code fences)
                - IMPORTANT: offer JUST the blog content, no confirmations, nothing else
                """;

            final String title =
                chatClient.prompt()
                    .user(titlePrompt)
                    .call()
                    .content();

            if (Strings.isBlank(title))
            {
                return Optional.empty();
            }

            final String content =
                chatClient.prompt()
                    .user(contentPrompt.formatted(title))
                    .call()
                    .content();

            if (Strings.isBlank(content))
            {
                return Optional.empty();
            }

            return Optional.of(new PostDefaults(textSanitizer.sanitize(title), textSanitizer.sanitize(content)));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }
}
