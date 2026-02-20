package com.alexbalmus.acbblog.modules.blog.ai;

import java.util.Optional;

import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alexbalmus.acbblog.modules.blog.common.post.defaults.PostDefaults;
import com.alexbalmus.acbblog.modules.blog.common.post.defaults.PostDefaultsGenerator;

@Component
@Profile("Ai")
public class SpringAiPostDefaultsGenerator implements PostDefaultsGenerator
{
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

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
                Return plain text only, no quotes, no markdown, max 80 characters.
                """;

            final String contentPrompt =
                """
                Generate content for a Java-related blog post.
                Constraints:
                - audience: developers
                - 2 to 4 short paragraphs
                - practical and accurate
                - plain test only (no markdown headings, no code fences)
                """;

            final String title =
                chatClient.prompt()
                    .user(titlePrompt)
                    .call()
                    .content();

            final String content =
                chatClient.prompt()
                    .user(contentPrompt)
                    .call()
                    .content();

            if (Strings.isBlank(title) || Strings.isBlank(content))
            {
                return Optional.empty();
            }

            return Optional.of(new PostDefaults(title, content));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }
}
