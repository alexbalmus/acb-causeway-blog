package com.alexbalmus.acbblog.modules.blog.dom;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.dom.blog.Blogs;

import com.alexbalmus.acbblog.modules.blog.types.Handle;
import com.alexbalmus.acbblog.modules.blog.types.Name;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainObjectLayout;
import org.apache.causeway.applib.annotation.HomePage;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.PromptStyle;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.annotation.TableDecorator;
import org.apache.causeway.applib.services.user.UserService;

@Named("blog.BlogsHomePage")
@DomainObject(nature = Nature.VIEW_MODEL)
@HomePage
@DomainObjectLayout()
public class BlogsHomePage
{
    @ObjectSupport
    public String title()
    {
        return getObjects().size() + " objects";
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
    public String default0Create()
    {
        return "My Blog 001";
    }
    public String default1Create()
    {
        return userService.currentUser().get().getName();
    }

    @Inject Blogs blogs;
    @Inject UserService userService;
}
