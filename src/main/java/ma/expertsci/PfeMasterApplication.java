package ma.expertsci;

import ma.expertsci.account.entities.*;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.company.CompanyStatus;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.entities.user.UserStatus;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class PfeMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PfeMasterApplication.class, args);
    }
    @Bean
    CommandLineRunner initData(
            CompanyRepository companyRepository,
            UserRepository userRepository
    ) {
        return args -> {

            // Create Company
            Company company = Company.builder()
                    .name("Experts ITN")
                    .country("Morocco")
                    .domainName("experts-itn")
                    .status(CompanyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            company = companyRepository.save(company);

            // Create User 1
            User user1 = User.builder()
                    .email("admin@experts-itn.com")
                    .password("123456") // plain for now (we'll hash later)
                    .firstName("Admin")
                    .lastName("User")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .company(company)
                    .build();

//            // Create User 2
//            User user2 = User.builder()
//                    .email("client@experts-itn.com")
//                    .password("123456")
//                    .firstName("Client")
//                    .lastName("User")
//                    .role(UserRole.OWNER)
//                    .status(UserStatus.ACTIVE)
//                    .createdAt(LocalDateTime.now())
//                    .company(company)
//                    .build();
//
            userRepository.save(user1);
//            userRepository.save(user2);

            System.out.println("Company and users created successfully!");
        };
    }
}
