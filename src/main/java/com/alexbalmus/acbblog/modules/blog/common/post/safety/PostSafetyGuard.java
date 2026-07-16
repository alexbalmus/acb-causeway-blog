package com.alexbalmus.acbblog.modules.blog.common.post.safety;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import org.apache.logging.log4j.util.Strings;

/**
 * Central entry point for post content moderation. Applies the {@link PostSafetyChecker}
 * whenever the "Ai" profile is active, so that every path that creates or changes post
 * text (creation, inline content edit, title rename) enforces the same rules.
 */
@Service
public class PostSafetyGuard
{
    public static final String AI_PROFILE = "Ai";
    public static final String AI_FEATURES_ARE_ACTIVE_BUT_NO_SAFETY_CHECKER_IS_AVAILABLE =
        "Ai features are active but no safety checker is available.";
    public static final String CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE =
        "Post content is not appropriate for a general audience.";
    public static final String CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE_WITH_REASON =
        "Post content is not appropriate for a general audience: ";

    private final Environment environment;
    private final ObjectProvider<PostSafetyChecker> postSafetyCheckerProvider;

    public PostSafetyGuard(
        final Environment environment,
        final ObjectProvider<PostSafetyChecker> postSafetyCheckerProvider)
    {
        this.environment = environment;
        this.postSafetyCheckerProvider = postSafetyCheckerProvider;
    }

    /**
     * @return a veto message if the given title/content must not be published, or {@code null} if allowed
     */
    public String check(final String title, final String content)
    {
        if (!isAiProfileActive())
        {
            return null;
        }

        PostSafetyChecker checker = postSafetyCheckerProvider.getIfAvailable();

        if (checker == null)
        {
            return AI_FEATURES_ARE_ACTIVE_BUT_NO_SAFETY_CHECKER_IS_AVAILABLE;
        }

        SafetyAssessment assessment = checker.assess(title, content);

        if (assessment.safe())
        {
            return null;
        }

        String reason = assessment.reason();

        return Strings.isBlank(reason)
            ? CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE
            : CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE_WITH_REASON + reason;
    }

    private boolean isAiProfileActive()
    {
        return environment.acceptsProfiles(Profiles.of(AI_PROFILE));
    }
}
