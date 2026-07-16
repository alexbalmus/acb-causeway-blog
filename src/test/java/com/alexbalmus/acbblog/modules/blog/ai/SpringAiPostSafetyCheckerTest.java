package com.alexbalmus.acbblog.modules.blog.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import com.alexbalmus.acbblog.modules.blog.common.post.safety.SafetyAssessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiPostSafetyCheckerTest
{
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ChatClient.Builder> builderProvider = mock(ObjectProvider.class);
    private final ChatClient.Builder builder = mock(ChatClient.Builder.class);
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

    private final SpringAiPostSafetyChecker checker = new SpringAiPostSafetyChecker(builderProvider);

    @BeforeEach
    void setUp()
    {
        when(builderProvider.getIfAvailable()).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
    }

    @Test
    void noChatClientAvailableBlocks()
    {
        when(builderProvider.getIfAvailable()).thenReturn(null);

        SafetyAssessment assessment = checker.assess("title", "content");

        assertThat(assessment.safe()).isFalse();
        assertThat(assessment.reason()).isEqualTo(SpringAiPostSafetyChecker.UNABLE_TO_ASSESS_CONTENT_SAFETY);
    }

    @Test
    void safeResponseAllows()
    {
        stubResponse("SAFE");

        assertThat(checker.assess("title", "content").safe()).isTrue();
    }

    @Test
    void safeResponseIsCaseInsensitiveAndTrimmed()
    {
        stubResponse("  safe  ");

        assertThat(checker.assess("title", "content").safe()).isTrue();
    }

    @Test
    void unsafeResponseWithReasonBlocksWithReason()
    {
        stubResponse("UNSAFE: contains profanity");

        SafetyAssessment assessment = checker.assess("title", "content");

        assertThat(assessment.safe()).isFalse();
        assertThat(assessment.reason()).isEqualTo("contains profanity");
    }

    @Test
    void unsafeResponseWithoutReasonBlocksWithGenericReason()
    {
        stubResponse("UNSAFE");

        SafetyAssessment assessment = checker.assess("title", "content");

        assertThat(assessment.safe()).isFalse();
        assertThat(assessment.reason())
            .isEqualTo(SpringAiPostSafetyChecker.CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE);
    }

    @Test
    void blankResponseBlocks()
    {
        stubResponse("   ");

        SafetyAssessment assessment = checker.assess("title", "content");

        assertThat(assessment.safe()).isFalse();
        assertThat(assessment.reason()).isEqualTo(SpringAiPostSafetyChecker.UNABLE_TO_ASSESS_CONTENT_SAFETY);
    }

    @Test
    void unparseableResponseBlocks()
    {
        stubResponse("As an AI model I think this is fine.");

        SafetyAssessment assessment = checker.assess("title", "content");

        assertThat(assessment.safe()).isFalse();
        assertThat(assessment.reason()).isEqualTo(SpringAiPostSafetyChecker.UNABLE_TO_ASSESS_CONTENT_SAFETY);
    }

    @Test
    void exceptionDuringCallBlocks()
    {
        when(chatClient.prompt().user(anyString()).call().content())
            .thenThrow(new RuntimeException("connection refused"));

        SafetyAssessment assessment = checker.assess("title", "content");

        assertThat(assessment.safe()).isFalse();
        assertThat(assessment.reason()).isEqualTo(SpringAiPostSafetyChecker.UNABLE_TO_ASSESS_CONTENT_SAFETY);
    }

    private void stubResponse(final String response)
    {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(response);
    }
}
