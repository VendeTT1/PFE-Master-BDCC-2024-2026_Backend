package ma.expertsci.account.repository;

import ma.expertsci.account.entities.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

}
