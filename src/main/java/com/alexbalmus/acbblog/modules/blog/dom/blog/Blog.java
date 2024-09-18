package com.alexbalmus.acbblog.modules.blog.dom.blog;

import java.util.Comparator;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.alexbalmus.acbblog.modules.blog.types.Name;

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
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.title.TitleService;
import org.apache.causeway.persistence.jpa.applib.integration.CausewayEntityListener;

import lombok.Setter;

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
    @Inject @Transient RepositoryService repositoryService;
    @Inject @Transient TitleService titleService;
    @Inject @Transient MessageService messageService;

    protected Blog(){}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false, name = "id")
    private Long id;

    @Version
    @Column(nullable = false, name = "version")
    private int version;


    public Blog(final String name, final String handle)
    {
        this.name = name;
        this.handle = handle;
    }

    @Setter @Column(length = Name.MAX_LEN, nullable = false, name = "name")
    private String name;

    @Title(prepend = "Blog: ")
    @Name
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.IDENTITY, sequence = "1")
    public String getName()
    {
        return name;
    }

    @Setter @Column(length = Name.MAX_LEN, nullable = false, name = "handle")
    private String handle;

    @Name
    @PropertyLayout(fieldSetId = LayoutConstants.FieldSetId.IDENTITY, sequence = "2")
    public String getHandle()
    {
        return handle;
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
    @MemberSupport public String default0UpdateName()
    {
        return getName();
    }

    @Action(
        semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE
    )
    @ActionLayout(
        fieldSetId = LayoutConstants.FieldSetId.IDENTITY,
        describedAs = "Deletes this object from the database",
        position = ActionLayout.Position.PANEL
    )
    public void delete()
    {
        final String title = titleService.titleOf(this);
        messageService.informUser(String.format("'%s' deleted", title));
        repositoryService.removeAndFlush(this);
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
}
