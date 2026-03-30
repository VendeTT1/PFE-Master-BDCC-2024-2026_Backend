package ma.expertsci.instances.controller;

import lombok.RequiredArgsConstructor;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.services.InstanceService;
import ma.expertsci.security.JwtService;
import org.apache.tomcat.Jar;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;
    private final JwtService jwtService;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/create")
    public ResponseEntity<InstanceResponseDTO> create(
            Authentication auth,
            @RequestBody CreatedInstanceRequestDTO request
    ) {
        return ResponseEntity.ok(
                instanceService.createInstance(auth.getName(), request)
        );
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> start(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.startInstance(auth.getName(), id);
        return ResponseEntity.ok("Instance started");
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> stop(Authentication auth, @PathVariable Long id) throws Exception {
        instanceService.stopInstance(auth.getName(), id);
        return ResponseEntity.ok("Instance stopped");
    }

    @PostMapping("/{id}/restart")
    @PreAuthorize("hasRole('OWNER')")
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

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<InstanceResponseDTO>> list(Authentication auth) {
        return ResponseEntity.ok(instanceService.getInstances(auth.getName()));
    }

    @GetMapping("/{id}/access")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> accessInstance(
            Authentication auth,
            @PathVariable Long id
    ) {

        Instance instance = instanceService.getInstanceForUser(auth.getName(), id);

        String token = jwtService.generateOdooToken(
                auth.getName(),
                instance.getName()
        );

        String url = instance.getUrl() + "/saas-login?token=" + token;

        return ResponseEntity.ok(url);
    }
}
