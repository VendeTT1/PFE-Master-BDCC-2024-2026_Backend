package ma.expertsci.account.entities;
import jakarta.persistence.*;
import lombok.*;
import ma.expertsci.account.entities.user.User;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private Instant expiryDate;

    private boolean revoked;

    @ManyToOne
    private User user;
}
