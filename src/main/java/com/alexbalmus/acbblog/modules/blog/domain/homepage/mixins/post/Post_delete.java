package com.alexbalmus.acbblog.modules.blog.domain.homepage.mixins.post;

import jakarta.inject.Inject;

import lombok.RequiredArgsConstructor;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.title.TitleService;

import com.alexbalmus.acbblog.modules.blog.domain.post.Post;

@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE
)
@ActionLayout(
    fieldSetId = LayoutConstants.FieldSetId.IDENTITY,
    describedAs = "Deletes this post from the database",
    position = ActionLayout.Position.PANEL
)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Post_delete
{
    private final Post post;

    @Inject TitleService titleService;
    @Inject MessageService messageService;
    @Inject RepositoryService repositoryService;

    public void act()
    {
        final String title = titleService.titleOf(post);
        messageService.informUser(String.format("'%s' deleted", title));
        repositoryService.removeAndFlush(post);
    }
}
