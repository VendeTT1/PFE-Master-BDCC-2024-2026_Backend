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
import ma.expertsci.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


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
                .password(passwordEncoder.encode(request.getPassword()))
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

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtService.generateToken(request.getEmail());


        return LoginResponseDTO.builder().token(token).build();
    }


}
