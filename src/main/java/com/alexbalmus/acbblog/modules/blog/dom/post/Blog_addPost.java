package com.alexbalmus.acbblog.modules.blog.dom.post;

import javax.inject.Inject;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.types.Name;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;

import lombok.RequiredArgsConstructor;

@Action(
        semantics = SemanticsOf.IDEMPOTENT,
        commandPublishing = Publishing.ENABLED,
        executionPublishing = Publishing.ENABLED
)
@ActionLayout(associateWith = "posts", sequence = "1")
@RequiredArgsConstructor
public class Blog_addPost
{

    private final Blog blog;

    public Blog act(
            @Name final String title,
            final String content
            ) {
        repositoryService.persist(new Post(blog, title, content));
        return blog;
    }

    public String validate0Act(final String title) {
        return postsRepository.findByBlogAndTitle(blog, title).isPresent()
                ? String.format("Post with title '%s' already defined for this blog", title)
                : null;
    }

    public String default1Act() {
        return "The quick brown fox...";
    }

    @Inject PostsRepository postsRepository;
    @Inject RepositoryService repositoryService;
}
