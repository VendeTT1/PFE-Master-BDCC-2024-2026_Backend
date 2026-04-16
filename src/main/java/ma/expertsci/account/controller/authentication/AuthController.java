package ma.expertsci.account.controller.authentication;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.company.UserResponseDTO;
import ma.expertsci.account.dto.login.LoginRequestDTO;
import ma.expertsci.account.dto.login.LoginResponseDTO;
import ma.expertsci.account.dto.login.RefreshTokenRequestDTO;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
import ma.expertsci.account.entities.RefreshToken;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.exception.DataAlreadyExistException;
import ma.expertsci.account.exception.InvalidCredentialsException;
import ma.expertsci.account.service.AuthService;
import ma.expertsci.security.RefreshTokenService;
import ma.expertsci.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

//@PreAuthorize("#id == authentication.principal.id") -> This allows user to access only his own resource.
//@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService registrationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;


    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request
    ) throws DataAlreadyExistException {
        return ResponseEntity.ok(registrationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request, HttpServletResponse response
    ) throws InvalidCredentialsException {
        return ResponseEntity.ok(registrationService.login(request, response));
    }

//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/admin")
//    public String adminEndpoint() {
//        return "Only admins";
//    }

//    @PreAuthorize("hasRole('STAFF')")
//    @GetMapping("/staff")
//    public String userEndpoint() {
//        return "Only users";
//    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refreshToken(
            @RequestBody RefreshTokenRequestDTO request) {

        RefreshToken refreshToken =
                refreshTokenService.verifyToken(
                        request.getRefreshToken());

        User user = refreshToken.getUser();

        String newAccessToken =
                jwtService.generateToken(
                        userDetailsService
                                .loadUserByUsername(user.getEmail()));

        return ResponseEntity.ok(
                LoginResponseDTO.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken.getToken())
                        .build()
        );
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody RefreshTokenRequestDTO request) {

        refreshTokenService.revokeToken(
                request.getRefreshToken());

        return ResponseEntity.ok("Logged out successfully");
    }


}


