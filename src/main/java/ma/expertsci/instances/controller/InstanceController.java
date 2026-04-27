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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final JwtService jwtService;
    private final UserService userService;
    private final DockerService dockerService;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/create")
    public ResponseEntity<InstanceResponseDTO> create(
            Authentication auth
//            @RequestBody CreatedInstanceRequestDTO request
    ) {
        return ResponseEntity.ok(
                instanceService.createInstance(auth.getName())
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/{id}/start")
    public ResponseEntity<InstanceStatus> start(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.startInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.RUNNING);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @PostMapping("/{id}/stop")
    public ResponseEntity<InstanceStatus> stop(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.stopInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.STOPPED);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
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

        //Extract user role from Spring Security
        String role = auth.getAuthorities()
                .stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("STAFF"); // fallback safety

        String token = jwtService.generateOdooToken(
                auth.getName(),
                role,
                instance.getName()
        );

//        String url = instance.getUrl() + "/saas-login?db=" + instance.getName() + "_db&token=" + token;
        String url = instance.getUrl() + "/saas-login?token=" + token;

        AccessURLDTO accessURLDTO = new AccessURLDTO(url);
        return ResponseEntity.ok(accessURLDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/allInstances")
    public ResponseEntity<Page<InstanceResponseDTO>> getInstances(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50); // max 50

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                instanceService.getInstances(auth.getName(), pageable)
        );
    }

    // Endpoint to trigger Nginx config generation
    @GetMapping("/generate-nginx-config/{instanceName}")
    public String generateNginxConfig(@PathVariable String instanceName) {
        try {
            dockerService.generateNginxConfig(instanceName); // Call the service method to generate the config
            // After generating the Nginx config
            dockerService.updateHostsFile(instanceName); // Update hosts file with the new domain
            return "Nginx config generated for instance: " + instanceName;
        } catch (Exception e) {
            return "Error generating Nginx config: " + e.getMessage();
        }
    }

}
