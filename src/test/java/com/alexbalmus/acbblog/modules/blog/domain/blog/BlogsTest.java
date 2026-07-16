package com.alexbalmus.acbblog.modules.blog.domain.blog;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.user.UserMemento;
import org.apache.causeway.applib.services.user.UserService;

import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandle;
import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandlesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlogsTest
{
    private static final String CURRENT_USER = "sven";

    private final RepositoryService repositoryService = mock(RepositoryService.class);
    private final BlogsRepository blogsRepository = mock(BlogsRepository.class);
    private final UserHandlesRepository userHandlesRepository = mock(UserHandlesRepository.class);
    private final UserService userService = mock(UserService.class);

    private final Blogs blogs = new Blogs(
        repositoryService, blogsRepository, userHandlesRepository, userService, new MockEnvironment());

    @BeforeEach
    void setUp()
    {
        when(userService.currentUser()).thenReturn(Optional.of(UserMemento.ofName(CURRENT_USER)));
        when(userHandlesRepository.findByUsername(CURRENT_USER)).thenReturn(Optional.empty());
    }

    @Test
    void createIsValidForNewUserWithFreeHandle()
    {
        when(userHandlesRepository.findByHandle("sven_h")).thenReturn(Optional.empty());
        when(blogsRepository.existsByNameAndHandle("My Blog", "sven_h")).thenReturn(false);

        assertThat(blogs.validateCreate("My Blog", "sven_h")).isNull();
    }

    @Test
    void createIsVetoedWhenUserAlreadyHasDifferentHandle()
    {
        when(userHandlesRepository.findByUsername(CURRENT_USER))
            .thenReturn(Optional.of(new UserHandle(CURRENT_USER, "existing_h")));

        assertThat(blogs.validateCreate("My Blog", "other_h"))
            .contains("already associated with handle 'existing_h'");
    }

    @Test
    void createIsVetoedWhenHandleBelongsToAnotherUser()
    {
        when(userHandlesRepository.findByHandle("taken_h"))
            .thenReturn(Optional.of(new UserHandle("someoneElse", "taken_h")));

        assertThat(blogs.validateCreate("My Blog", "taken_h"))
            .contains("already associated with another user");
    }

    @Test
    void createIsVetoedForDuplicateBlogName()
    {
        when(userHandlesRepository.findByUsername(CURRENT_USER))
            .thenReturn(Optional.of(new UserHandle(CURRENT_USER, "sven_h")));
        when(userHandlesRepository.findByHandle("sven_h"))
            .thenReturn(Optional.of(new UserHandle(CURRENT_USER, "sven_h")));
        when(blogsRepository.existsByNameAndHandle("My Blog", "sven_h")).thenReturn(true);

        assertThat(blogs.validateCreate("My Blog", "sven_h"))
            .contains("already exists for handle 'sven_h'");
    }

    @Test
    void changeHandleIsVetoedWhenUserHasNoHandleYet()
    {
        assertThat(blogs.validateChangeHandle("new_h"))
            .isEqualTo("No handle is associated with the current user");
    }

    @Test
    void changeHandleIsVetoedForUnchangedHandle()
    {
        when(userHandlesRepository.findByUsername(CURRENT_USER))
            .thenReturn(Optional.of(new UserHandle(CURRENT_USER, "sven_h")));

        assertThat(blogs.validateChangeHandle("sven_h"))
            .isEqualTo("This is already the current user's handle");
    }

    @Test
    void changeHandleIsVetoedWhenHandleBelongsToAnotherUser()
    {
        when(userHandlesRepository.findByUsername(CURRENT_USER))
            .thenReturn(Optional.of(new UserHandle(CURRENT_USER, "sven_h")));
        when(userHandlesRepository.findByHandle("taken_h"))
            .thenReturn(Optional.of(new UserHandle("someoneElse", "taken_h")));

        assertThat(blogs.validateChangeHandle("taken_h"))
            .contains("already associated with another user");
    }

    @Test
    void changeHandleIsValidForFreeHandle()
    {
        when(userHandlesRepository.findByUsername(CURRENT_USER))
            .thenReturn(Optional.of(new UserHandle(CURRENT_USER, "sven_h")));
        when(userHandlesRepository.findByHandle("new_h")).thenReturn(Optional.empty());

        assertThat(blogs.validateChangeHandle("new_h")).isNull();
    }
}
