package ma.expertsci.billing.repository;

import ma.expertsci.account.entities.company.Company;
import ma.expertsci.billing.entity.PaymentStatus;
import ma.expertsci.billing.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findByCompanyOrderByCreatedAtDesc(Company company);

    Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<PaymentTransaction> findByCompanyAndStatus(Company company, PaymentStatus status);

    boolean existsByTransactionIdAndStatus(String transactionId, PaymentStatus status);
}