package com.example.demo.account;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"))
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 24)
    private Role role;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    protected UserAccount() {}

    public UserAccount(String displayName, String email, String passwordHash, Role role) {
        this.displayName = displayName.strip();
        this.email = EmailAddresses.normalize(email);
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Role getRole() { return role; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public long getAuthVersion() { return authVersion; }
}
