package com.alexbalmus.acbblog.modules.blog.domain.userhandle;

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

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.ProxyUtils;

import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.persistence.jpa.applib.integration.CausewayEntityListener;

import com.alexbalmus.acbblog.modules.blog.types.Handle;

@Entity
@Table(
    schema = "blog",
    name = "UserHandle",
    uniqueConstraints = {
        @UniqueConstraint(name = "UserHandle__username__UNQ", columnNames = {"username"}),
        @UniqueConstraint(name = "UserHandle__handle__UNQ", columnNames = {"handle"})
    }
)
@EntityListeners(CausewayEntityListener.class)
@Named("blog.UserHandle")
@DomainObject()
@SuppressWarnings("unused")
public class UserHandle
{
    public static final int USERNAME_MAX_LEN = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false, name = "id")
    private Long id;

    @Version
    @Column(nullable = false, name = "version")
    private int version;

    @Getter
    @Column(length = USERNAME_MAX_LEN, nullable = false, name = "username")
    private String username;

    @Getter
    @Setter
    @Column(length = Handle.MAX_LEN, nullable = false, name = "handle")
    private String handle;

    protected UserHandle() {}

    public UserHandle(final String username, final String handle)
    {
        this.username = username;
        this.handle = handle;
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

        UserHandle userHandle = (UserHandle) o;

        return id != null && Objects.equals(id, userHandle.id);
    }

    @Override
    public final int hashCode()
    {
        return ProxyUtils.getUserClass(this).hashCode();
    }
}
