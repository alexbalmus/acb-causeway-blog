package com.alexbalmus.acbblog.modules.blog.dom.post;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.types.Content;
import com.alexbalmus.acbblog.modules.blog.types.Name;

import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.services.repository.RepositoryService;

@Named("blog.Posts")
@DomainService(nature = NatureOfService.VIEW)
@Priority(PriorityPrecedence.EARLY)
public class Posts
{

    private final RepositoryService repositoryService;
    private final PostsRepository postsRepository;

    @Inject
    public Posts(
            final RepositoryService repositoryService,
            final PostsRepository postsRepository) {
        this.repositoryService = repositoryService;
        this.postsRepository = postsRepository;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_MODAL)
    public Post create(
            final Blog blog,
            @Name final String title,
            @Content final String content) {
        return repositoryService.persist(new Post(blog, title, content));
    }
    public String default1Create() {
        return "My Post 001";
    }
    public String default2Create() {
        return "The quick brown fox jumps over the lazy dog.";
    }


    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_SIDEBAR)
    public List<Post> findByTitle(
            @Name final String title) {
        return postsRepository.findByTitleContaining(title);
    }

    @Action(semantics = SemanticsOf.SAFE, restrictTo = RestrictTo.PROTOTYPING)
    public List<Post> listAll() {
        return postsRepository.findAll();
    }


}
