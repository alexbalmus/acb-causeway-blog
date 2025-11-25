package com.alexbalmus.acbblog.modules.blog.domain.homepage.mixins.blog;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;

import com.alexbalmus.acbblog.modules.blog.common.ApplicationContextHelper;
import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.domain.post.Post;
import com.alexbalmus.acbblog.modules.blog.domain.post.PostsRepository;


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
    @MemberSupport
    public String disableAct()
    {
        return postsRepository.findByBlog(blog).isEmpty() ? "No posts" : null;
    }

    // Extension method for Blog:
    public static void deletePost(Blog thiz, Post post)
    {
        ApplicationContextHelper.getBean(RepositoryService.class).removeAndFlush(post);
    }
}
