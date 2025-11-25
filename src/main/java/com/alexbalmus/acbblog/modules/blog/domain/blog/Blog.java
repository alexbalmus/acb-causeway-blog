package com.alexbalmus.acbblog.modules.blog.domain.blog;

import java.util.Comparator;
import java.util.Objects;

import jakarta.inject.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.springframework.data.util.ProxyUtils;

import lombok.Setter;

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

import com.alexbalmus.acbblog.modules.blog.types.Name;
import com.alexbalmus.acbblog.modules.blog.types.Handle;


@Entity
@Table(
    schema="blog",
    name = "Blog",
    uniqueConstraints = {
        @UniqueConstraint(name = "Blog__name__UNQ", columnNames = {"name"})
    }
)
@EntityListeners(CausewayEntityListener.class)
@Named("blog.Blog")
@DomainObject()
@DomainObjectLayout()
@SuppressWarnings("unused")
public class Blog implements Comparable<Blog>
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false, name = "id")
    private Long id;

    @Version
    @Column(nullable = false, name = "version")
    private int version;

    @Setter
    @Column(length = Name.MAX_LEN, nullable = false, name = "name")
    private String name;

    @Setter
    @Column(length = Name.MAX_LEN, nullable = false, name = "handle")
    private String handle;


    protected Blog() {}

    public Blog(final String name, final String handle)
    {
        this.name = name;
        this.handle = handle;
    }


    @Title(prepend = "Blog: ")
    @Name
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.IDENTITY, sequence = "1")
    public String getName()
    {
        return name;
    }

    @Action(
        semantics = SemanticsOf.IDEMPOTENT,
        executionPublishing = Publishing.ENABLED
    )
    @ActionLayout(
        associateWith = "name",
        describedAs = "Updates the object's name"
    )
    public Blog updateName(@Name final String name)
    {
        setName(name);
        return this;
    }
    @MemberSupport
    public String default0UpdateName()
    {
        return getName();
    }

    @Handle
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.IDENTITY, sequence = "2")
    public String getHandle()
    {
        return handle;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public int compareTo(final Blog other)
    {
        return Comparator.comparing(Blog::getName).compare(this, other);
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

        Blog blog = (Blog) o;

        return id != null && Objects.equals(id, blog.id);
    }

    @Override
    public final int hashCode()
    {
        return ProxyUtils.getUserClass(this).hashCode();
    }
}
