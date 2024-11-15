package com.alexbalmus.acbblog.modules.blog.dom.homepage.blogcontributions;

import jakarta.inject.Inject;

import lombok.RequiredArgsConstructor;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.services.repository.RepositoryService;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.types.Content;
import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.dom.post.Post;
import com.alexbalmus.acbblog.modules.blog.dom.post.PostsRepository;


@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT,
    commandPublishing = Publishing.ENABLED,
    executionPublishing = Publishing.ENABLED
)
@ActionLayout(
    associateWith = "blog",
    sequence = "1",
    promptStyle = PromptStyle.DIALOG_MODAL
)
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blog_createPost
{
    private final Blog blog;
    @Inject PostsRepository postsRepository;
    @Inject RepositoryService repositoryService;

    public Blog act(@Name final String title, @Content final String content)
    {
        repositoryService.persist(new Post(blog, title, content));
        return blog;
    }
    public String validate0Act(final String title)
    {
        return postsRepository.findByBlogAndTitle(blog, title).isPresent()
                ? String.format("Post with title '%s' already defined for this blog", title)
                : null;
    }
    @MemberSupport
    public String default0Act()
    {
        return "My Post 001";
    }
    @MemberSupport
    public String default1Act()
    {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud " +
                "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
                "nulla pariatur. Excepteur sint occaecat cupidatat non proident, " +
                "sunt in culpa qui officia deserunt mollit anim id est laborum.";
    }
}
