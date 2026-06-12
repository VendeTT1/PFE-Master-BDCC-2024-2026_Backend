package ma.expertsci.instances.repository;

import ma.expertsci.account.entities.company.Company;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.entities.InstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    Optional<Instance> findByName(String name);

    List<Instance> findByCompany(Company company);

    /**
     * Deletes all failed/incomplete instance records for a company.
     * Called before each creation attempt to clean up stale CREATING or ERROR rows,
     * ensuring retries are never blocked by a previous failed attempt.
     */
    @Modifying
    @Transactional
    void deleteByCompanyAndStatusIn(Company company, List<InstanceStatus> statuses);
}