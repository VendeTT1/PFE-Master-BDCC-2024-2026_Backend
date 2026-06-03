package ma.expertsci.instances.entities;

import jakarta.persistence.*;
import lombok.Data;
import ma.expertsci.account.entities.company.Company;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "instances")
@Data
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String subdomain;

    @Enumerated(EnumType.STRING)
    private InstanceStatus status;

    private String dockerContainerId;

    private String dbContainerId;

    private String url;

    @OneToOne //for now, we need to master one instance per company then we can expand
    private Company company;

    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(
            name = "instance_modules",
            joinColumns = @JoinColumn(name = "instance_id")
    )
    @Column(name = "module")
    private List<String> modules;
}
