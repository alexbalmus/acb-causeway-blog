package com.alexbalmus.acbblog.modules.blog.dom.homepage.blogcontributions;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.Programmatic;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;

import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.dom.post.Post;
import com.alexbalmus.acbblog.modules.blog.dom.post.PostsRepository;


@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE,
    commandPublishing = Publishing.ENABLED,
    executionPublishing = Publishing.ENABLED
)
@ActionLayout(associateWith = "blog", sequence = "2")
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blog_deletePost
{
    private final Blog blog;
    @Inject PostsRepository postsRepository;
    @Inject RepositoryService repositoryService;
    @Inject MessageService messageService;

    public Blog act(@Name final String title)
    {
        postsRepository.findByBlogAndTitle(blog, title)
            .ifPresent(
                post ->
                {
                    messageService.informUser(String.format("'%s' deleted", title));
                    repositoryService.removeAndFlush(post);
                });
        return blog;
    }
    @MemberSupport
    public List<String> choices0Act()
    {
        return postsRepository.findByBlog(blog)
                .stream()
                .map(Post::getTitle)
                .collect(Collectors.toList());
    }
    @MemberSupport
    public String default0Act()
    {
        List<String> names = choices0Act();
        return names.size() == 1 ? names.get(0) : null;
    }

    public String disableAct()
    {
        return postsRepository.findByBlog(blog).isEmpty() ? "No posts" : null;
    }

    @Programmatic
    public void act(Post post)
    {
        repositoryService.removeAndFlush(post);
    }
}
