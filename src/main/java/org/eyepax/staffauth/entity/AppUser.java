package org.eyepax.staffauth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cognito_id", nullable = false, unique = true)
    private String cognitoId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "locale")
    private String locale;
}