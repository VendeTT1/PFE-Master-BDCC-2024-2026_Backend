package ma.expertsci.account.entities.password;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import ma.expertsci.account.entities.user.User;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @ManyToOne
    private User user;

    private Instant expiryDate;

    private boolean used;
}
