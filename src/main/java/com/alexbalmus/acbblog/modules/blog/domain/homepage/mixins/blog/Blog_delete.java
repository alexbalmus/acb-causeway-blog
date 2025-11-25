package com.alexbalmus.acbblog.modules.blog.domain.homepage.mixins.blog;

import jakarta.inject.Inject;

import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;

import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.title.TitleService;

import com.alexbalmus.acbblog.modules.blog.common.ApplicationContextHelper;
import com.alexbalmus.acbblog.modules.blog.domain.post.PostsRepository;
import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;


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
@ExtensionMethod(Blog_deletePost.class)
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
        postsRepository.findByBlog(blog)
            .forEach(post -> blog.deletePost(post));
    }

    // Extension method for Blog:
    public static void delete(Blog thiz)
    {
        ApplicationContextHelper.getBean(PostsRepository.class).findByBlog(thiz)
            .forEach(post -> thiz.deletePost(post));
        ApplicationContextHelper.getBean(RepositoryService.class).removeAndFlush(thiz);
    }
}
