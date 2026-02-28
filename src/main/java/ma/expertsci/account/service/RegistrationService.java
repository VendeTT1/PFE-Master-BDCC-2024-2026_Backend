package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.login.LoginRequestDTO;
import ma.expertsci.account.dto.login.LoginResponseDTO;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
import ma.expertsci.account.entities.*;
import ma.expertsci.account.exception.DataAlreadyExistException;
import ma.expertsci.account.exception.InvalidCredentialsException;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public RegisterResponseDTO register(RegisterRequestDTO request) throws DataAlreadyExistException {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DataAlreadyExistException("Email already exists");
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

    public LoginResponseDTO login(LoginRequestDTO request) throws InvalidCredentialsException {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return LoginResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }


}
