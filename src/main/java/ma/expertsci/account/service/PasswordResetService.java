package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.password.PasswordResetToken;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.PasswordResetTokenRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.emailconf.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final long EXPIRATION = 1000 * 60 * 30; // 30 minutes

    public void requestPasswordReset(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(Instant.now().plusMillis(EXPIRATION))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            String resetLink = "http://localhost:5173/reset-password?token=" + token;

            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });

        // Always return success → security best practice
    }

    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token already used");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}