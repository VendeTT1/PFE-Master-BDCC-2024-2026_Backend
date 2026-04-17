package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.user.UserDTO;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserStatus;
import ma.expertsci.account.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public List<UserDTO> getAllUsers() {

        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            throw new RuntimeException("No users found");
        }
        List<UserDTO> userDTOs = users.stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .userRole(user.getRole().name())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .status(user.getStatus().name())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());

        return userDTOs;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        User userToInactive = userRepository.getReferenceById(id);
        userToInactive.setStatus(UserStatus.INACTIVE);
        userRepository.save(userToInactive);
    }
}
