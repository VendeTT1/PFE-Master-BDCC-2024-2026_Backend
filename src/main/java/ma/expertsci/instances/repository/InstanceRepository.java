package ma.expertsci.instances.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.instances.entities.Instance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstanceRepository extends JpaRepository<Instance, Long> {
    List<Instance> findByCompany(Company company);

    Optional<Instance> findByName(String instanceName);
}
