package ma.expertsci.instances.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.service.UserService;
import ma.expertsci.instances.dto.AccessURLDTO;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.entities.InstanceStatus;
import ma.expertsci.instances.services.DockerService;
import ma.expertsci.instances.services.InstanceService;
import ma.expertsci.security.JwtService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final JwtService jwtService;
    private final UserService userService;
    private final DockerService dockerService;

    // ── Create instance ──────────────────────────────────────────────────────

    /**
     * POST /api/instances/create
     *
     * Body: { "name": "acme-prod", "modules": ["sale", "purchase", "inventory"] }
     *
     * The authenticated user must have ROLE_OWNER.
     * "base" and "saas_sso" are always enforced server-side regardless of the
     * module list supplied here.
     */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/create")
    public ResponseEntity<InstanceResponseDTO> create(
            Authentication auth,
            @Valid @RequestBody CreatedInstanceRequestDTO request
    ) {
        return ResponseEntity.ok(
                instanceService.createInstance(auth.getName(), request)
        );
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/start")
    public ResponseEntity<InstanceStatus> start(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.startInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.RUNNING);
    }

//    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/stop")
    public ResponseEntity<InstanceStatus> stop(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.stopInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.STOPPED);
    }

//    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/restart")
    public ResponseEntity<InstanceStatus> restart(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.restartInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.RUNNING);
    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('OWNER')")
//    public ResponseEntity<String> delete(Authentication auth, @PathVariable Long id) throws Exception {
//        instanceService.deleteInstance(auth.getName(), id);
//        return ResponseEntity.ok("Instance deleted");
//    }

    // ── User-facing queries ──────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @GetMapping("userInstance")
    public ResponseEntity<InstanceResponseDTO> getUserInstanceOnly(Authentication auth) throws Exception {
        return ResponseEntity.ok(instanceService.getUserInstanceOnly(auth.getName()));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @GetMapping("/{id}/access")
    public ResponseEntity<AccessURLDTO> accessInstance(
            Authentication auth,
            @PathVariable Long id
    ) {
        Instance instance = instanceService.getInstanceForUser(auth.getName(), id);

        String role = auth.getAuthorities()
                .stream()
                .findFirst()
                .map(ga -> ga.getAuthority().replace("ROLE_", ""))
                .orElse("STAFF");

        String token = jwtService.generateOdooToken(
                auth.getName(),
                role,
                instance.getName()
        );

        String url = instance.getUrl() + "/saas-login?token=" + token;
        return ResponseEntity.ok(new AccessURLDTO(url));
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allInstances")
    public ResponseEntity<Page<InstanceResponseDTO>> getInstances(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(instanceService.getInstances(auth.getName(), pageable));
    }

    // ── Nginx utilities ──────────────────────────────────────────────────────

    @GetMapping("/generate-nginx-config/{instanceName}")
    public String generateNginxConfig(@PathVariable String instanceName) {
        try {
            dockerService.generateNginxConfig(instanceName);
            dockerService.updateHostsFile(instanceName);
            dockerService.reloadNginx();
            return "Nginx config generated for instance: " + instanceName;
        } catch (Exception e) {
            return "Error generating Nginx config: " + e.getMessage();
        }
    }
}