package ma.expertsci.account.entities.invitation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.expertsci.account.entities.company.Company;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String token;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    private LocalDateTime expirationDate;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}