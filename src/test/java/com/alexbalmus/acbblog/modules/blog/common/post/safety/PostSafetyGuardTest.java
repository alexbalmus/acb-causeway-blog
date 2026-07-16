package com.alexbalmus.acbblog.modules.blog.common.post.safety;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostSafetyGuardTest
{
    @SuppressWarnings("unchecked")
    private final ObjectProvider<PostSafetyChecker> checkerProvider = mock(ObjectProvider.class);

    @Test
    void withoutAiProfileNoCheckIsPerformed()
    {
        var guard = new PostSafetyGuard(new MockEnvironment(), checkerProvider);

        assertThat(guard.check("any title", "any content")).isNull();
        verifyNoInteractions(checkerProvider);
    }

    @Test
    void aiProfileActiveButNoCheckerAvailableVetoes()
    {
        when(checkerProvider.getIfAvailable()).thenReturn(null);
        var guard = new PostSafetyGuard(aiEnvironment(), checkerProvider);

        assertThat(guard.check("title", "content"))
            .isEqualTo(PostSafetyGuard.AI_FEATURES_ARE_ACTIVE_BUT_NO_SAFETY_CHECKER_IS_AVAILABLE);
    }

    @Test
    void safeContentPasses()
    {
        when(checkerProvider.getIfAvailable())
            .thenReturn((title, content) -> SafetyAssessment.allowed());
        var guard = new PostSafetyGuard(aiEnvironment(), checkerProvider);

        assertThat(guard.check("title", "content")).isNull();
    }

    @Test
    void unsafeContentWithReasonVetoesWithReason()
    {
        when(checkerProvider.getIfAvailable())
            .thenReturn((title, content) -> SafetyAssessment.blocked("contains profanity"));
        var guard = new PostSafetyGuard(aiEnvironment(), checkerProvider);

        assertThat(guard.check("title", "content"))
            .isEqualTo(PostSafetyGuard.CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE_WITH_REASON
                + "contains profanity");
    }

    @Test
    void unsafeContentWithoutReasonVetoesWithGenericMessage()
    {
        when(checkerProvider.getIfAvailable())
            .thenReturn((title, content) -> SafetyAssessment.blocked(" "));
        var guard = new PostSafetyGuard(aiEnvironment(), checkerProvider);

        assertThat(guard.check("title", "content"))
            .isEqualTo(PostSafetyGuard.CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE);
    }

    private static MockEnvironment aiEnvironment()
    {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(PostSafetyGuard.AI_PROFILE);
        return environment;
    }
}
