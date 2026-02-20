package com.alexbalmus.acbblog.modules.blog.ai;

import org.apache.logging.log4j.util.Strings;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alexbalmus.acbblog.modules.blog.common.post.safety.PostSafetyChecker;
import com.alexbalmus.acbblog.modules.blog.common.post.safety.SafetyAssessment;

@Component
@Profile("Ai")
public class SpringAiPostSafetyChecker implements PostSafetyChecker
{
    public static final String UNABLE_TO_ASSESS_CONTENT_SAFETY = "Unable to assess content safety.";
    public static final String SAFE = "SAFE";
    public static final String UNSAFE = "UNSAFE";
    public static final String CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE = "Content is not appropriate for a general audience.";

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public SpringAiPostSafetyChecker(final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider)
    {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public SafetyAssessment assess(String title, String content)
    {
        try
        {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder == null)
            {
                return SafetyAssessment.blocked(UNABLE_TO_ASSESS_CONTENT_SAFETY);
            }

            ChatClient chatClient = builder.build();

            final String prompt =
                """
                You are a strict safety moderator for a general audience blog.
                Evaluate whether the following title and content contain inappropriate content such as (but not limited to) profanity, references to drugs etc.
                
                Return exactly one line:
                SAFE
                or
                UNSAFE: <short reason>
                
                Title:
                %s
                
                Content:
                %s
                """.formatted(sanitize(title), sanitize(content));

            final String response =
                chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (Strings.isBlank(response))
            {
                return SafetyAssessment.blocked(UNABLE_TO_ASSESS_CONTENT_SAFETY);
            }


            final String trimmedResponse = response.trim();

            if (trimmedResponse.equalsIgnoreCase(SAFE))
            {
                return SafetyAssessment.allowed();
            }

            if (trimmedResponse.contains(UNSAFE))
            {
                final int separator = trimmedResponse.indexOf(':');

                final String reason = separator >= 0 && separator + 1 < trimmedResponse.length()
                    ? trimmedResponse.substring(separator + 1).trim()
                    : CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE;

                return SafetyAssessment.blocked(reason);
            }

            return SafetyAssessment.blocked(UNABLE_TO_ASSESS_CONTENT_SAFETY);
        }
        catch (Exception e)
        {
            return SafetyAssessment.blocked(UNABLE_TO_ASSESS_CONTENT_SAFETY);
        }
    }

    public static String sanitize(final String value)
    {
        return value == null ? "" : value.trim();
    }
}
