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
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.RestrictTo;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.user.UserService;

import com.alexbalmus.acbblog.modules.blog.types.Handle;
import com.alexbalmus.acbblog.modules.blog.types.Name;


@Named("blog.Blogs")
@DomainService()
@Priority(PriorityPrecedence.EARLY)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blogs
{
    public static final String DEV_PROFILE = "Dev";

    private final RepositoryService repositoryService;
    private final BlogsRepository blogsRepository;
    private final UserService userService;
    private final Environment environment;

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_MODAL)
    public Blog create(@Name final String name, @Handle final String handle)
    {
        return repositoryService.persist(new Blog(name, handle));
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
        if (!isDevProfileActive())
        {
            return null;
        }
        return userService.currentUser().orElseThrow().name();
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public List<Blog> findByNameContaining(@Name final String name)
    {
        return blogsRepository.findByNameContaining(name);
    }

    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public Blog findByNameAndHandle(@Name final String name, @Handle String handle)
    {
        return blogsRepository.findByNameAndHandle(name, handle);
    }

    @Action(semantics = SemanticsOf.SAFE, restrictTo = RestrictTo.PROTOTYPING)
    public List<Blog> listAll()
    {
        return blogsRepository.findAllByHandle(userService.currentUser().orElseThrow().name());
    }

    private boolean isDevProfileActive()
    {
        return environment.acceptsProfiles(Profiles.of(DEV_PROFILE));
    }
}
