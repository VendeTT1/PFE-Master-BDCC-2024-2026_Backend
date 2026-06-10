package ma.expertsci.account.service;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.dto.company.CompanyResponseDTO;
import ma.expertsci.account.dto.company.UpdateCompanyDTO;
import ma.expertsci.account.dto.user.UserResponseDTO;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserStatus;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.exception.BusinessRuleViolationException;
import ma.expertsci.exception.ForbiddenActionException;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.subscriptions.entities.Subscription;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.subscriptions.service.SubscriptionService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public Company getCurrentCompany(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User with email " + email + " not found"));

        return user.getCompany();
    }

    public CompanyResponseDTO getCompanyDetails(String email) {

        Company company = getCurrentCompany(email);

        User owner = company.getUsers()
                .stream()
                .filter(u -> u.getRole().name().equals("OWNER"))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("COMPANY_OWNER_NOT_FOUND",
                        "No owner found for company " + company.getName()));

        return CompanyResponseDTO.builder()
                .id(company.getId())
                .name(company.getName())
                .region(company.getCountry())
                .ownerEmail(owner.getEmail())
                .usersCount(company.getUsers().size())
                .build();
    }

    public CompanyResponseDTO updateCompany(String email, UpdateCompanyDTO request) {

        Company company = getCurrentCompany(email);

        if (request.getName() != null) {
            company.setName(request.getName());
        }

        if (request.getRegion() != null) {
            company.setCountry(request.getRegion());
        }

        companyRepository.save(company);

        return getCompanyDetails(email);
    }

    public List<UserResponseDTO> getCompanyUsers(String email) {

        Company company = getCurrentCompany(email);
        return company.getUsers()
                .stream()
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .status(user.getStatus())
                        .build()
                )
                .toList();
    }

    public void deactivateCompanyUser(String email, Long userId) {

        Company company = getCurrentCompany(email);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User with id " + userId + " not found"));

        if (!user.getCompany().getId().equals(company.getId())) {
            throw new ForbiddenActionException("USER_NOT_IN_COMPANY",
                    "User does not belong to your company");
        }

        if (user.getRole().name().equals("OWNER")) {
            throw new BusinessRuleViolationException("CANNOT_DEACTIVATE_OWNER",
                    "Cannot deactivate the company owner");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public UserResponseDTO getUser(String email){
        Company company = getCurrentCompany(email);

        User owner = company.getUsers().get(0);

        return UserResponseDTO.builder()
                .email(owner.getEmail())
                .firstName(owner.getFirstName())
                .lastName(owner.getLastName())
                .role(owner.getRole().name())
                .status(owner.getStatus())
                .build();


    }

}