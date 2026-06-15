package ma.expertsci.account.controller.admin;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.user.UserDTO;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // Create a new user, accessible only by Admin role
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping
        public ResponseEntity<User> createUser(@RequestBody User user) {
            User createdUser = userService.createUser(user);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        }

        // Get all users, accessible only by Admin role
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/allUsers")
        public ResponseEntity<Page<UserDTO>> getAllUsers(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            size = Math.min(size, 50);

            Pageable pageable = PageRequest.of(page, size);
            Page<UserDTO> users = userService.getAllUsers(pageable);

            return ResponseEntity.ok(users);
        }

        // Get user by ID, accessible only by Admin role
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/user/{id}")
        public ResponseEntity<User> getUserById(@PathVariable Long id) {
            User user = userService.getUserById(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }

        // Delete user by ID, accessible only by Admin role
        @PreAuthorize("hasRole('ADMIN')")
        @DeleteMapping("/inactive/user/{id}")
        public ResponseEntity<Void> setUserInactive(@PathVariable Long id) {
            userService.setUserInactive(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @PatchMapping("/activate/user/{id}")
        public ResponseEntity<Void> setUserActive(@PathVariable Long id) {
            userService.setUserActive(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

}
