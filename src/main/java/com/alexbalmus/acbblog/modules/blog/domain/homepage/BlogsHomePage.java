package com.alexbalmus.acbblog.modules.blog.domain.homepage;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import lombok.experimental.ExtensionMethod;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainObjectLayout;
import org.apache.causeway.applib.annotation.HomePage;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.annotation.TableDecorator;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.user.UserService;

import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.domain.blog.Blogs;
import com.alexbalmus.acbblog.modules.blog.types.Handle;
import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.domain.homepage.mixins.blog.Blog_delete;


@Named("blog.BlogsHomePage")
@DomainObject(nature = Nature.VIEW_MODEL)
@HomePage
@DomainObjectLayout()
@ExtensionMethod(Blog_delete.class)
@SuppressWarnings("unused")
public class BlogsHomePage
{
    @Inject Blogs blogs;
    @Inject UserService userService;
    @Inject FactoryService factoryService;

    @ObjectSupport
    public String title()
    {
        return getBlogs().size() + " blogs";
    }

    @Collection
    @CollectionLayout(tableDecorator = TableDecorator.DatatablesNet.class)
    public List<Blog> getBlogs()
    {
        return blogs.listAll();
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(promptStyle = PromptStyle.DIALOG_MODAL)
    public Blog create(@Name final String name, @Handle final String handle)
    {
        return blogs.create(name, handle);
    }
    @MemberSupport
    public String default0Create()
    {
        return "My Blog 001";
    }
    @MemberSupport
    public String default1Create()
    {
        return userService.currentUser().orElseThrow().name();
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE)
    @ActionLayout(
        fieldSetId = LayoutConstants.FieldSetId.IDENTITY,
        describedAs = "Deletes this blog and all its posts from the database",
        position = ActionLayout.Position.PANEL
    )
    public void deleteBlog(@Name final String name)
    {
        Optional.ofNullable(blogs.findByNameAndHandle(name,
            userService.currentUser().orElseThrow().name()))
            .ifPresent(blog -> blog.delete());
    }
    @MemberSupport
    public List<String> choices0DeleteBlog()
    {
        return blogs.listAll().stream()
            .map(Blog::getName)
            .toList();
    }
    @MemberSupport
    public String default0DeleteBlog()
    {
        List<String> names = choices0DeleteBlog();
        return names.size() == 1 ? names.get(0) : null;
    }
    @MemberSupport
    public String disableDeleteBlog()
    {
        return choices0DeleteBlog().isEmpty() ? "No blogs" : null;
    }
}
