package ma.expertsci.instances.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.service.UserService;
import ma.expertsci.instances.dto.AccessURLDTO;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.entities.InstanceStatus;
import ma.expertsci.instances.services.InstanceService;
import ma.expertsci.security.JwtService;
import org.apache.tomcat.Jar;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin("*")
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final JwtService jwtService;
    private final UserService userService;

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

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/start")
    public ResponseEntity<InstanceStatus> start(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.startInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.RUNNING);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/stop")
    public ResponseEntity<InstanceStatus> stop(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.stopInstance(auth.getName(), id);
        return ResponseEntity.ok(InstanceStatus.STOPPED);
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{id}/restart")
    public ResponseEntity<String> restart(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.restartInstance(auth.getName(), id);
        return ResponseEntity.ok("Instance restarted");
    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('OWNER')")
//    public ResponseEntity<String> delete(Authentication auth, @PathVariable Long id) throws Exception {
//        instanceService.deleteInstance(auth.getName(), id);
//        return ResponseEntity.ok("Instance deleted");
//    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<List<InstanceResponseDTO>> list(Authentication auth) {
        return ResponseEntity.ok(instanceService.getInstances(auth.getName()));
    }

    @PreAuthorize("hasRole('OWNER')")
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
    public ResponseEntity<List<InstanceResponseDTO>> allInstances(Authentication auth) {
        return ResponseEntity.ok(instanceService.getInstances(auth.getName()));
    }

}
