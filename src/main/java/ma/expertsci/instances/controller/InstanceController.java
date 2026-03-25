package ma.expertsci.instances.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.services.InstanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<InstanceResponseDTO> create(
            Authentication auth,
            @RequestBody CreatedInstanceRequestDTO request
    ) {
        return ResponseEntity.ok(
                instanceService.createInstance(auth.getName(), request)
        );
    }
}
