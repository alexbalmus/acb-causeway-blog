package com.alexbalmus.acbblog.modules.blog.dom.blog;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import com.alexbalmus.acbblog.modules.blog.types.Handle;
import com.alexbalmus.acbblog.modules.blog.types.Name;

import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.user.UserService;

@Named("blog.Blogs")
@DomainService(nature = NatureOfService.VIEW)
@Priority(PriorityPrecedence.EARLY)
public class Blogs
{
    private final RepositoryService repositoryService;
    private final BlogsRepository blogsRepository;
    private final UserService userService;

    @Inject
    public Blogs(
        final RepositoryService repositoryService,
        final BlogsRepository blogsRepository,
        final UserService userService)
    {
        this.repositoryService = repositoryService;
        this.blogsRepository = blogsRepository;
        this.userService = userService;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_MODAL)
    public Blog create(@Name final String name, @Handle final String handle)
    {
        return repositoryService.persist(new Blog(name, handle));
    }
    public String default0Create()
    {
        return "My Blog 001";
    }
    public String default1Create()
    {
        return userService.currentUser().get().getName();
    }


    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public List<Blog> findByName(@Name final String name)
    {
        return blogsRepository.findByNameContaining(name);
    }

    @Action(semantics = SemanticsOf.SAFE, restrictTo = RestrictTo.PROTOTYPING)
    public List<Blog> listAll()
    {
        return blogsRepository.findAllByHandle(userService.currentUser().get().getName());
    }

}
