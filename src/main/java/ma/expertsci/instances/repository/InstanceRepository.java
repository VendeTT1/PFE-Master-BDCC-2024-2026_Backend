package ma.expertsci.instances.repository;

import ma.expertsci.account.entities.company.Company;
import ma.expertsci.instances.entities.Instance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstanceRepository extends JpaRepository<Instance, Long> {
    List<Instance> findByCompany(Company company);
}
