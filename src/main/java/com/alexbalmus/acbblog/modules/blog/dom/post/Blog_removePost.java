package com.alexbalmus.acbblog.modules.blog.dom.post;

import com.alexbalmus.acbblog.modules.blog.types.Name;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;

import java.util.List;
import java.util.stream.Collectors;

@Action(
    semantics = SemanticsOf.IDEMPOTENT,
    commandPublishing = Publishing.ENABLED,
    executionPublishing = Publishing.ENABLED
)
@ActionLayout(associateWith = "posts", sequence = "2")
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blog_removePost
{
    private final Blog blog;
    @Inject PostsRepository postsRepository;
    @Inject RepositoryService repositoryService;

    public Blog act(@Name final String title)
    {
        postsRepository.findByBlogAndTitle(blog, title)
                .ifPresent(repositoryService::remove);
        return blog;
    }

    public String disableAct()
    {
        return postsRepository.findByBlog(blog).isEmpty() ? "No posts" : null;
    }

    public List<String> choices0Act()
    {
        return postsRepository.findByBlog(blog)
                .stream()
                .map(Post::getTitle)
                .collect(Collectors.toList());
    }

    public String default0Act()
    {
        List<String> names = choices0Act();
        return names.size() == 1 ? names.get(0) : null;
    }
}
