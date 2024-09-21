package com.alexbalmus.acbblog.modules.blog.dom.blog;

import com.alexbalmus.acbblog.modules.blog.dom.post.PostsRepository;

import jakarta.inject.Inject;

import lombok.RequiredArgsConstructor;

import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.title.TitleService;

@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE
)
@ActionLayout(
    fieldSetId = LayoutConstants.FieldSetId.IDENTITY,
    describedAs = "Deletes this blog and all its posts from the database",
    position = ActionLayout.Position.PANEL
)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blog_delete
{
    private final Blog blog;
    @Inject TitleService titleService;
    @Inject MessageService messageService;
    @Inject RepositoryService repositoryService;
    @Inject PostsRepository postsRepository;

    public void act()
    {
        var posts = postsRepository.findByBlog(blog);
        posts.forEach(post -> postsRepository.findByBlogAndTitle(blog, post.getTitle())
                .ifPresent(repositoryService::removeAndFlush));

        final String title = titleService.titleOf(blog);
        messageService.informUser(String.format("'%s' and its posts have been deleted", title));
        repositoryService.removeAndFlush(blog);
    }
}
