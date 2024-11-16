package com.alexbalmus.acbblog.modules.blog.dom.homepage;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.dom.blog.Blogs;
import com.alexbalmus.acbblog.modules.blog.types.Handle;
import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.dom.homepage.blogcontributions.Blog_delete;

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

@Named("blog.BlogsHomePage")
@DomainObject(nature = Nature.VIEW_MODEL)
@HomePage
@DomainObjectLayout()
@SuppressWarnings("unused")
public class BlogsHomePage
{
    @Inject Blogs blogs;
    @Inject UserService userService;
    @Inject FactoryService factoryService;

    @ObjectSupport
    public String title()
    {
        return getObjects().size() + " blogs";
    }

    @Collection
    @CollectionLayout(tableDecorator = TableDecorator.DatatablesNet.class)
    public List<Blog> getObjects()
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
        return userService.currentUser().orElseThrow().getName();
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
            userService.currentUser().orElseThrow().getName()))
            .ifPresent(blog ->
            {
                var blog_delete = factoryService.mixin(Blog_delete.class, blog);
                blog_delete.act();
            });
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
}
