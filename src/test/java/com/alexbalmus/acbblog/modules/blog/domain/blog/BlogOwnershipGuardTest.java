package com.alexbalmus.acbblog.modules.blog.domain.blog;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.apache.causeway.applib.services.user.UserMemento;
import org.apache.causeway.applib.services.user.UserService;

import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandle;
import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandlesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlogOwnershipGuardTest
{
    private final UserService userService = mock(UserService.class);
    private final UserHandlesRepository userHandlesRepository = mock(UserHandlesRepository.class);
    private final BlogOwnershipGuard guard = new BlogOwnershipGuard(userService, userHandlesRepository);

    private final Blog blog = new Blog("My Blog", "sven_h");

    @Test
    void ownerIsAllowed()
    {
        currentUserIs("sven");
        when(userHandlesRepository.findByUsername("sven"))
            .thenReturn(Optional.of(new UserHandle("sven", "sven_h")));

        assertThat(guard.isOwnedByCurrentUser(blog)).isTrue();
        assertThat(guard.vetoUnlessOwnedByCurrentUser(blog)).isNull();
    }

    @Test
    void userWithDifferentHandleIsVetoed()
    {
        currentUserIs("dick");
        when(userHandlesRepository.findByUsername("dick"))
            .thenReturn(Optional.of(new UserHandle("dick", "dick_h")));

        assertThat(guard.isOwnedByCurrentUser(blog)).isFalse();
        assertThat(guard.vetoUnlessOwnedByCurrentUser(blog))
            .isEqualTo(BlogOwnershipGuard.ONLY_THE_BLOG_OWNER_CAN_DO_THIS);
    }

    @Test
    void userWithoutHandleIsVetoed()
    {
        currentUserIs("joe");
        when(userHandlesRepository.findByUsername("joe")).thenReturn(Optional.empty());

        assertThat(guard.vetoUnlessOwnedByCurrentUser(blog))
            .isEqualTo(BlogOwnershipGuard.ONLY_THE_BLOG_OWNER_CAN_DO_THIS);
    }

    @Test
    void anonymousUserIsVetoed()
    {
        when(userService.currentUser()).thenReturn(Optional.empty());

        assertThat(guard.vetoUnlessOwnedByCurrentUser(blog))
            .isEqualTo(BlogOwnershipGuard.ONLY_THE_BLOG_OWNER_CAN_DO_THIS);
    }

    @Test
    void nullBlogIsVetoed()
    {
        currentUserIs("sven");
        when(userHandlesRepository.findByUsername("sven"))
            .thenReturn(Optional.of(new UserHandle("sven", "sven_h")));

        assertThat(guard.vetoUnlessOwnedByCurrentUser(null))
            .isEqualTo(BlogOwnershipGuard.ONLY_THE_BLOG_OWNER_CAN_DO_THIS);
    }

    private void currentUserIs(final String username)
    {
        when(userService.currentUser()).thenReturn(Optional.of(UserMemento.ofName(username)));
    }
}
