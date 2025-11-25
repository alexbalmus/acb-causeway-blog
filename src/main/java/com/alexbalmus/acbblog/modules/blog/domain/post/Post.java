package com.alexbalmus.acbblog.modules.blog.domain.post;

import java.util.Comparator;
import java.util.Objects;

import jakarta.inject.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.util.ProxyUtils;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainObjectLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.PropertyLayout;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.annotation.SemanticsOf;
import org.apache.causeway.applib.annotation.Title;
import org.apache.causeway.applib.layout.LayoutConstants;
import org.apache.causeway.persistence.jpa.applib.integration.CausewayEntityListener;

import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.types.Content;
import com.alexbalmus.acbblog.modules.blog.types.Name;


@Entity
@Table(
    schema="blog",
    name = "Post"
)
@EntityListeners(CausewayEntityListener.class)
@Named("blog.Post")
@DomainObject()
@DomainObjectLayout()
@SuppressWarnings("unused")
public class Post implements Comparable<Post>
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false, name = "id")
    private Long id;

    @Version
    @Column(nullable = false, name = "version")
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id")
    @PropertyLayout(fieldSetId =  LayoutConstants.FieldSetId.IDENTITY, sequence = "1")
    @Getter @Setter
    private Blog blog;

    @Setter
    @Column(length = Name.MAX_LEN, nullable = false, name = "title")
    private String title;

    @Setter
    @Column(length = Content.MAX_LEN, name = "content")
    private String content;


    protected Post(){}

    public Post(final Blog blog, final String title, final String content)
    {
        this.blog = blog;
        this.title = title;
        this.content = content;
    }


    @Title(prepend = "Object: ")
    @Name
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.IDENTITY, sequence = "2")
    public String getTitle()
    {
        return title;
    }

    @Content
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.DETAILS, sequence = "1")
    public String getContent()
    {
        return content;
    }

    @Action(
        semantics = SemanticsOf.IDEMPOTENT,
        executionPublishing = Publishing.ENABLED
    )
    @ActionLayout(
        associateWith = "title",
        describedAs = "Updates the object's title"
    )
    public Post updateTitle(@Name final String name)
    {
        setTitle(name);
        return this;
    }
    @MemberSupport
    public String default0UpdateTitle()
    {
        return getTitle();
    }

    @Override
    public final boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(o))
        {
            return false;
        }

        Post post = (Post) o;

        return id != null && Objects.equals(id, post.id);
    }

    @Override
    public final int hashCode()
    {
        return ProxyUtils.getUserClass(this).hashCode();
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    @Override
    public int compareTo(final Post other)
    {
        return Comparator.comparing(Post::getTitle).compare(this, other);
    }
}
