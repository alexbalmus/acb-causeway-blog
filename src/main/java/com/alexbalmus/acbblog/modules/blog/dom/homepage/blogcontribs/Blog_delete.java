package com.alexbalmus.acbblog.modules.blog.dom.homepage.blogcontribs;

import jakarta.inject.Inject;

import lombok.RequiredArgsConstructor;

import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.title.TitleService;

import com.alexbalmus.acbblog.modules.blog.dom.post.PostsRepository;
import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;


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
    @Inject FactoryService factoryService;

    public void act()
    {
        deletePosts();

        final String title = titleService.titleOf(blog);
        messageService.informUser(String.format("'%s' and its posts have been deleted", title));
        repositoryService.removeAndFlush(blog);
    }

    private void deletePosts()
    {
        var blog_deletePost = factoryService.mixin(Blog_deletePost.class, blog);
        postsRepository.findByBlog(blog)
            .forEach(blog_deletePost::act);
    }
}
