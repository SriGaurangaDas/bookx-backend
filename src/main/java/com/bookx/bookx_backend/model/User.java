package com.bookx.bookx_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;

    private Double latitude;

    private Double longitude;

    @Column(length = 512)
    private String profileImageUrl;

    private Instant registeredAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
}
