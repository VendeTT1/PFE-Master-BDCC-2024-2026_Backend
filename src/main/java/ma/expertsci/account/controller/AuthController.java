package ma.expertsci.account.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.login.LoginRequestDTO;
import ma.expertsci.account.dto.login.LoginResponseDTO;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
import ma.expertsci.account.exception.DataAlreadyExistException;
import ma.expertsci.account.exception.InvalidCredentialsException;
import ma.expertsci.account.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private  final RegistrationService registrationService;



    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request
    ) throws DataAlreadyExistException {
        return ResponseEntity.ok(registrationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) throws InvalidCredentialsException {
        return ResponseEntity.ok(registrationService.login(request));
    }

}
