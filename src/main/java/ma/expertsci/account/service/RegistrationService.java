package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
import ma.expertsci.account.entities.*;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

//    public void register(
//            String companyName,
//            String country,
//            String email,
//            String password,
//            String firstName,
//            String lastName
//    ) {
//
//        Company company = Company.builder()
//                .name(companyName)
//                .country(country)
//                .status(CompanyStatus.ACTIVE)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        company = companyRepository.save(company);
//
//        User user = User.builder()
//                .email(email)
//                .password(password) // plain for now
//                .firstName(firstName)
//                .lastName(lastName)
//                .role(UserRole.OWNER)
//                .status(UserStatus.ACTIVE)
//                .createdAt(LocalDateTime.now())
//                .company(company)
//                .build();
//
//        userRepository.save(user);
//    }

    public RegisterResponseDTO register(RegisterRequestDTO request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Company company = Company.builder()
                .name(request.getCompanyName())
                .country(request.getCountry())
                .status(CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        company = companyRepository.save(company);

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .company(company)
                .build();

        user = userRepository.save(user);

        return RegisterResponseDTO.builder()
                .companyId(company.getId())
                .userId(user.getId())
                .message("Registration successful")
                .build();
    }


}
