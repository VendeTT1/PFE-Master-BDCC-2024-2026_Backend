package ma.expertsci.account.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.company.UserResponseDTO;
import ma.expertsci.account.dto.login.LoginRequestDTO;
import ma.expertsci.account.dto.login.LoginResponseDTO;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
import ma.expertsci.account.entities.*;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.company.CompanyStatus;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.entities.user.UserStatus;
import ma.expertsci.account.exception.DataAlreadyExistException;
import ma.expertsci.account.exception.InvalidCredentialsException;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.security.JwtService;
import ma.expertsci.security.RefreshTokenService;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SubscriptionService subscriptionService;


    public RegisterResponseDTO register(RegisterRequestDTO request) throws DataAlreadyExistException {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DataAlreadyExistException("Email already exists");
        }

//        if (request.getUserRole() == UserRole.STAFF) {
//            throw new IllegalArgumentException("Staff cannot self register");
//        }

        Company company = Company.builder()
                .name(request.getCompanyName())
                .country(request.getCountry())
                .status(CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        company = companyRepository.save(company);

        subscriptionService.initializeTrial(company);

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

    public UserResponseDTO login(LoginRequestDTO request, HttpServletResponse response) throws InvalidCredentialsException {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow();

        String accessToken = jwtService.generateToken(
                (UserDetails) Objects.requireNonNull(authentication.getPrincipal())
        );

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        Cookie accessTokenCookie = new Cookie("JWT", accessToken);
        accessTokenCookie.setHttpOnly(true);   // Prevent access via JavaScript
        accessTokenCookie.setSecure(false);     // Ensure cookie is sent over HTTPS (set to false for dev)
        accessTokenCookie.setPath("/");        // Available across the entire application
        accessTokenCookie.setMaxAge(60 * 15); // 15 min expiration (adjust as necessary)
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("Refresh_Token", refreshToken.getToken());
        refreshTokenCookie.setHttpOnly(true);   // Prevent access via JavaScript
        refreshTokenCookie.setSecure(false);     // Ensure cookie is sent over HTTPS (set to false for dev)
        refreshTokenCookie.setPath("/");        // Available across the entire application
        refreshTokenCookie.setMaxAge(60 * 60 * 8); // 8 hours expiration (adjust as necessary)
        response.addCookie(refreshTokenCookie);

//        return LoginResponseDTO.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken.getToken())
//                .build();

        return UserResponseDTO.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }


}
