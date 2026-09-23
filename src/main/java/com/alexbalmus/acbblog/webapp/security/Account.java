package com.alexbalmus.acbblog.webapp.security;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;
import org.apache.causeway.applib.annotation.Programmatic;

/** Authentication data is deliberately excluded from all Causeway viewers. */
@Entity
@Table(schema = "blog", name = "SecurityAccount")
@Programmatic
public class Account {
    @Id
    @Column(nullable = false, length = 255)
    private String username;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false)
    private boolean enabled;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "blog", name = "SecurityAccountRole",
            joinColumns = @JoinColumn(name = "username"))
    @Column(name = "authority", nullable = false)
    private Set<String> authorities = new HashSet<>();

    protected Account() {}

    public Account(String username, String passwordHash, boolean enabled, Set<String> authorities) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = new HashSet<>(authorities);
    }

    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public boolean enabled() { return enabled; }
    public Set<String> authorities() { return Set.copyOf(authorities); }
}
