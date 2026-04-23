package ma.expertsci.emailconf.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.login-url}")
    private String loginUrl;

    public void sendStaffInvite(String to, String firstName, String temporaryPassword) {
        String greetingName = (firstName != null && !firstName.isBlank()) ? firstName : "there";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Your ExpertSci staff account");
        message.setText(
                "Hello " + greetingName + ",\n\n" +
                        "Your staff account has been created.\n\n" +
                        "Username: " + to + "\n" +
                        "Temporary password: " + temporaryPassword + "\n\n" +
                        "Sign in here: " + loginUrl + "\n\n" +
                        "Please change your password after logging in.\n\n" +
                        "Best regards,\n" +
                        "ExpertSci"
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String link) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset your password");
        message.setText(
                "Click the link below to reset your password:\n\n" +
                        link + "\n\n" +
                        "This link will expire in 30 minutes."
        );

        mailSender.send(message);
    }

}