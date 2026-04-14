package ma.expertsci.account.controller.company;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.company.CompanyResponseDTO;
import ma.expertsci.account.dto.company.UpdateCompanyDTO;
import ma.expertsci.account.dto.company.UserResponseDTO;
import ma.expertsci.account.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin("*")
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CompanyResponseDTO> getCompany(Authentication auth) {

        return ResponseEntity.ok(
                companyService.getCompanyDetails(auth.getName())
        );
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CompanyResponseDTO> updateCompany(
            Authentication auth,
            @RequestBody UpdateCompanyDTO request
    ) {

        return ResponseEntity.ok(
                companyService.updateCompany(auth.getName(), request)
        );
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('OWNER')") //super admin should also be able to list the same
    public ResponseEntity<List<UserResponseDTO>> getUsers(Authentication auth) {

        return ResponseEntity.ok(
                companyService.getCompanyUsers(auth.getName())
        );
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> deactivateCompanyUser(
            Authentication auth,
            @PathVariable Long id
    ) {

        companyService.deactivateCompanyUser(auth.getName(), id);

        return ResponseEntity.ok("User deactivated successfully");
    }
}