package ma.expertsci.account.controller.authentication;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.password.ForgotPasswordRequestDTO;
import ma.expertsci.account.dto.password.MessageResponseDTO;
import ma.expertsci.account.dto.password.ResetPasswordRequestDTO;
import ma.expertsci.account.dto.user.ChangePasswordDTO;
import ma.expertsci.account.dto.user.UserResponseDTO;
import ma.expertsci.account.dto.login.LoginRequestDTO;
import ma.expertsci.account.dto.login.LoginResponseDTO;
import ma.expertsci.account.dto.login.RefreshTokenRequestDTO;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
import ma.expertsci.account.entities.RefreshToken;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.service.AuthService;
import ma.expertsci.account.service.PasswordResetService;
import ma.expertsci.security.CookieUtils;
import ma.expertsci.security.RefreshTokenService;
import ma.expertsci.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService registrationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordResetService passwordResetService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request
    ) {
        return ResponseEntity.ok(registrationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request, HttpServletResponse response
    ) {
        return ResponseEntity.ok(registrationService.login(request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshTokenValue = null;
        for (Cookie cookie : cookies) {
            if ("Refresh_Token".equals(cookie.getName())) {
                refreshTokenValue = cookie.getValue();
                break;
            }
        }

        if (refreshTokenValue == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken refreshToken = refreshTokenService.verifyToken(refreshTokenValue);
        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(
                userDetailsService.loadUserByUsername(user.getEmail())
        );

        Cookie accessCookie = new Cookie("JWT", newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // true in production with HTTPS
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);

        response.addCookie(accessCookie);

        return ResponseEntity.ok().build();
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = CookieUtils.extractRefreshTokenFromCookie(request);

        if (refreshTokenValue != null) {
            refreshTokenService.revokeToken(refreshTokenValue);
        }

        Cookie accessCookie = new Cookie("JWT", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);// true in production with HTTPS
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie("Refresh_Token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);// true in production with HTTPS
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(
                @RequestBody ForgotPasswordRequestDTO request) {

            passwordResetService.requestPasswordReset(request.getEmail());

            return ResponseEntity.ok(
                    new MessageResponseDTO("If the email exists, a reset link has been sent")
            );
        }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(
                @RequestBody ResetPasswordRequestDTO request) {

            passwordResetService.resetPassword(
                    request.getToken(),
                    request.getNewPassword()
            );

            return ResponseEntity.ok(
                    new MessageResponseDTO("Password updated successfully")
            );
        }


    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/updatePassword")
    public ResponseEntity<MessageResponseDTO> updatePassword(Authentication auth, @RequestBody ChangePasswordDTO newPassword) {

        authService.updatePassword(auth.getName(), newPassword);

        return  ResponseEntity.ok(
                new MessageResponseDTO("Password updated successfully")
        );
    }
}


