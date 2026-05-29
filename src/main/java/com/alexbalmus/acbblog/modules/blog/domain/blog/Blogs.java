package com.alexbalmus.acbblog.modules.blog.domain.blog;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import lombok.RequiredArgsConstructor;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.annotation.Programmatic;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.RestrictTo;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.user.UserService;

import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandle;
import com.alexbalmus.acbblog.modules.blog.domain.userhandle.UserHandlesRepository;
import com.alexbalmus.acbblog.modules.blog.types.Handle;
import com.alexbalmus.acbblog.modules.blog.types.Name;


@Named("blog.Blogs")
@DomainService()
@DomainServiceLayout(named = "Blogs")
@Priority(PriorityPrecedence.EARLY)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blogs
{
    public static final String DEV_PROFILE = "Dev";

    private final RepositoryService repositoryService;
    private final BlogsRepository blogsRepository;
    private final UserHandlesRepository userHandlesRepository;
    private final UserService userService;
    private final Environment environment;

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(
        cssClassFa = "fa-solid fa-plus",
        describedAs = "Create a new blog",
        named = "New Blog",
        promptStyle = PromptStyle.DIALOG_MODAL
    )
    public Blog create(@Name final String name, @Handle final String handle)
    {
        String validation = validateCreate(name, handle);
        if (validation != null)
        {
            throw new IllegalArgumentException(validation);
        }
        ensureCurrentUserHandle(handle);
        return repositoryService.persist(new Blog(name, handle));
    }
    @MemberSupport
    public String validateCreate(final String name, final String handle)
    {
        String currentUsername = currentUsername();

        var existingForUsername = userHandlesRepository.findByUsername(currentUsername);
        if (existingForUsername.isPresent())
        {
            String existingHandle = existingForUsername.orElseThrow().getHandle();
            if (!existingHandle.equals(handle))
            {
                return String.format(
                    "Current user '%s' is already associated with handle '%s'",
                    currentUsername,
                    existingHandle);
            }
        }

        var existingForHandle = userHandlesRepository.findByHandle(handle);
        if (existingForHandle.isPresent())
        {
            String existingUsername = existingForHandle.orElseThrow().getUsername();
            if (!existingUsername.equals(currentUsername))
            {
                return String.format("Handle '%s' is already associated with another user", handle);
            }
        }

        return blogsRepository.existsByNameAndHandle(name, handle)
            ? String.format("Blog with name '%s' already exists for handle '%s'", name, handle)
            : null;
    }
    @MemberSupport
    public String default0Create()
    {
        if (!isDevProfileActive())
        {
            return null;
        }
        return "My Blog 001";
    }
    @MemberSupport
    public String default1Create()
    {
        var existingHandle = currentUserHandle();
        if (existingHandle != null)
        {
            return existingHandle;
        }

        if (!isDevProfileActive())
        {
            return null;
        }
        return currentUsername();
    }
    @MemberSupport
    public String disable1Create()
    {
        return currentUserHandle() != null
            ? "Handle is already associated with the current user"
            : null;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout(
        cssClassFa = "fa-solid fa-at",
        describedAs = "Change the current user's handle and update all blogs owned by that handle",
        named = "Change Handle",
        promptStyle = PromptStyle.DIALOG_MODAL
    )
    public List<Blog> changeHandle(@Handle final String handle)
    {
        UserHandle userHandle = userHandlesRepository.findByUsername(currentUsername()).orElseThrow();
        String previousHandle = userHandle.getHandle();

        List<Blog> userBlogs = blogsRepository.findAllByHandleOrderByNameAsc(previousHandle);
        userBlogs.forEach(blog -> blog.setHandle(handle));
        blogsRepository.saveAll(userBlogs);

        userHandle.setHandle(handle);
        userHandlesRepository.save(userHandle);

        return userBlogs;
    }
    @MemberSupport
    public String validateChangeHandle(final String handle)
    {
        String currentHandle = currentUserHandle();
        if (currentHandle == null)
        {
            return "No handle is associated with the current user";
        }

        if (currentHandle.equals(handle))
        {
            return "This is already the current user's handle";
        }

        var existingForHandle = userHandlesRepository.findByHandle(handle);
        if (existingForHandle.isPresent())
        {
            String existingUsername = existingForHandle.orElseThrow().getUsername();
            if (!existingUsername.equals(currentUsername()))
            {
                return String.format("Handle '%s' is already associated with another user", handle);
            }
        }

        return null;
    }
    @MemberSupport
    public String default0ChangeHandle()
    {
        return currentUserHandle();
    }
    @MemberSupport
    public String disableChangeHandle()
    {
        return currentUserHandle() == null
            ? "No handle is associated with the current user"
            : null;
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(
        cssClassFa = "fa-solid fa-magnifying-glass",
        describedAs = "Find blogs by name",
        named = "Find Blogs",
        promptStyle = PromptStyle.DIALOG_SIDEBAR
    )
    public List<Blog> findByNameContaining(@Name final String name)
    {
        return blogsRepository.findByNameContainingOrderByNameAsc(name);
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(
        cssClassFa = "fa-solid fa-magnifying-glass",
        describedAs = "Find one blog by name and owner handle",
        named = "Find Blog",
        promptStyle = PromptStyle.DIALOG_SIDEBAR
    )
    public Blog findByNameAndHandle(@Name final String name, @Handle String handle)
    {
        return blogsRepository.findByNameAndHandle(name, handle);
    }

    @Action(semantics = SemanticsOf.SAFE, restrictTo = RestrictTo.PROTOTYPING)
    public List<Blog> listAll()
    {
        var handle = currentUserHandle();
        return handle != null ? blogsRepository.findAllByHandleOrderByNameAsc(handle) : List.of();
    }

    @Programmatic
    public String currentUserHandle()
    {
        return userHandlesRepository.findByUsername(currentUsername())
            .map(UserHandle::getHandle)
            .orElse(null);
    }

    private void ensureCurrentUserHandle(final String handle)
    {
        if (userHandlesRepository.findByUsername(currentUsername()).isEmpty())
        {
            repositoryService.persist(new UserHandle(currentUsername(), handle));
        }
    }

    private String currentUsername()
    {
        return userService.currentUser().orElseThrow().name();
    }

    private boolean isDevProfileActive()
    {
        return environment.acceptsProfiles(Profiles.of(DEV_PROFILE));
    }
}
