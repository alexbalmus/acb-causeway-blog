package com.alexbalmus.acbblog.modules.blog.domain.blog;

import org.springframework.stereotype.Service;

import org.apache.causeway.applib.services.user.UserMemento;
import org.apache.causeway.applib.services.user.UserService;

import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandle;
import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandlesRepository;

/**
 * Answers whether the current user owns a given {@link Blog}, i.e. whether the blog's
 * handle is the handle associated with the current user. Mutating actions on blogs and
 * posts use {@link #vetoUnlessOwnedByCurrentUser(Blog)} in their {@code disable...()}
 * supporting methods, so ownership is enforced uniformly across all viewers
 * (Wicket, REST, GraphQL) while blogs remain readable by everyone.
 */
@Service
public class BlogOwnershipGuard
{
    public static final String ONLY_THE_BLOG_OWNER_CAN_DO_THIS = "Only the blog owner can do this";

    private final UserService userService;
    private final UserHandlesRepository userHandlesRepository;

    public BlogOwnershipGuard(
        final UserService userService,
        final UserHandlesRepository userHandlesRepository)
    {
        this.userService = userService;
        this.userHandlesRepository = userHandlesRepository;
    }

    public boolean isOwnedByCurrentUser(final Blog blog)
    {
        return blog != null
            && blog.getHandle() != null
            && blog.getHandle().equals(currentUserHandle());
    }

    /**
     * @return a veto message if the current user does not own the given blog, or {@code null} if they do
     */
    public String vetoUnlessOwnedByCurrentUser(final Blog blog)
    {
        return isOwnedByCurrentUser(blog) ? null : ONLY_THE_BLOG_OWNER_CAN_DO_THIS;
    }

    private String currentUserHandle()
    {
        return userService.currentUser()
            .map(UserMemento::name)
            .flatMap(userHandlesRepository::findByUsername)
            .map(UserHandle::getHandle)
            .orElse(null);
    }
}
