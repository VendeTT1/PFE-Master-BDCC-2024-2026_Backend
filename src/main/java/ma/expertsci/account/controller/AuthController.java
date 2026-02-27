package ma.expertsci.account.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.registration.RegisterResponseDTO;
import ma.expertsci.account.dto.registration.RegisterRequestDTO;
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
    ) {
        return ResponseEntity.ok(registrationService.register(request));
    }


}
