package com.alexbalmus.acbblog.modules.blog.domain.homepage.mixins.blog;

import jakarta.inject.Inject;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import org.apache.logging.log4j.util.Strings;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;

import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.types.Content;
import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.domain.post.Post;
import com.alexbalmus.acbblog.modules.blog.domain.post.PostsRepository;
import com.alexbalmus.acbblog.modules.blog.common.post.defaults.PostDefaults;
import com.alexbalmus.acbblog.modules.blog.common.post.defaults.PostDefaultsGenerator;
import com.alexbalmus.acbblog.modules.blog.common.post.safety.PostSafetyChecker;
import com.alexbalmus.acbblog.modules.blog.common.post.safety.SafetyAssessment;


@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT,
    commandPublishing = Publishing.ENABLED,
    executionPublishing = Publishing.ENABLED
)
@ActionLayout(
    associateWith = "blog",
    sequence = "1",
    promptStyle = PromptStyle.DIALOG_MODAL
)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blog_createPost
{
    private static final PostDefaults FALLBACK_DEFAULTS = new PostDefaults(
        "My Post 001",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud " +
            "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
            "nulla pariatur. Excepteur sint occaecat cupidatat non proident, " +
            "sunt in culpa qui officia deserunt mollit anim id est laborum."
    );
    public static final String DEV_PROFILE = "Dev";
    public static final String AI_PROFILE = "Ai";
    public static final String AI_FEATURES_ARE_ACTIVE_BUT_NO_SAFETY_CHECKER_IS_AVAILABLE = "Ai features are active but no safety checker is available.";
    public static final String CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE = "Post content is not appropriate for a general audience.";
    public static final String CONTENT_IS_NOT_APPROPRIATE_FOR_A_GENERAL_AUDIENCE_WITH_REASON = "Post content is not appropriate for a general audience: ";

    private final Blog blog;
    private PostDefaults resolvedDefaults;


    @Inject PostsRepository postsRepository;
    @Inject RepositoryService repositoryService;
    @Inject Environment environment;
    @Inject ObjectProvider<PostDefaultsGenerator> postDefaultsGeneratorProvider;
    @Inject ObjectProvider<PostSafetyChecker> postSafetyCheckerProvider;

    public Blog act(@Name final String title, @Content final String content)
    {
        String safetyCheckResult = performSafetyCheck(title, content);
        if (safetyCheckResult != null)
        {
            throw new IllegalArgumentException(safetyCheckResult);
        }
        repositoryService.persist(new Post(blog, title, content));
        return blog;
    }
    public String validate0Act(final String title)
    {
        return postsRepository.findByBlogAndTitle(blog, title).isPresent()
            ? String.format("Post with title '%s' already defined for this blog", title)
            : null;
    }
    @MemberSupport
    public String default0Act()
    {
        PostDefaults defaults = resolveDefaults();
        return defaults != null ? defaults.title() : null;
    }
    @MemberSupport
    public String default1Act()
    {
        PostDefaults defaults = resolveDefaults();
        return defaults != null ? defaults.content() : null;
    }

    private String performSafetyCheck(final String title, final String content)
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

    private PostDefaults resolveDefaults()
    {
        if (!isDevProfileActive())
        {
            return null;
        }

        if (resolvedDefaults != null)
        {
            return resolvedDefaults;
        }

        if (!isAiProfileActive())
        {
            resolvedDefaults = FALLBACK_DEFAULTS;
            return resolvedDefaults;
        }

        PostDefaultsGenerator generator = postDefaultsGeneratorProvider.getIfAvailable();

        if (generator == null)
        {
            resolvedDefaults = FALLBACK_DEFAULTS;
            return resolvedDefaults;
        }

        resolvedDefaults = generator.generateForJavaBlogPost().orElse(FALLBACK_DEFAULTS);
        return resolvedDefaults;
    }

    private boolean isDevProfileActive()
    {
        return environment.acceptsProfiles(Profiles.of(DEV_PROFILE));
    }

    private boolean isAiProfileActive()
    {
        return environment.acceptsProfiles(Profiles.of(AI_PROFILE));
    }
}
