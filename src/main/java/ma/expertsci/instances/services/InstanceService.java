package ma.expertsci.instances.services;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.exception.BusinessRuleViolationException;
import ma.expertsci.exception.ExternalServiceException;
import ma.expertsci.exception.ForbiddenActionException;
import ma.expertsci.instances.exception.InstanceErrorCodes;
import ma.expertsci.subscriptions.entities.SubscriptionStatus;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.DockerResultDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.entities.InstanceStatus;
import ma.expertsci.instances.exception.InstanceErrorCodes;
import ma.expertsci.instances.repository.InstanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
//@RequiredArgsConstructor
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    private final DockerService dockerService;
    private final SubscriptionRepository subscriptionRepository;

    public InstanceService(InstanceRepository instanceRepository, UserRepository userRepository, DockerService dockerService, SubscriptionRepository subscriptionRepository) {
        this.instanceRepository = instanceRepository;
        this.userRepository = userRepository;
        this.dockerService = dockerService;
        this.subscriptionRepository = subscriptionRepository;
    }
    // ── Create instance ──────────────────────────────────────────────────────

    /**
     * Creates a new Odoo instance for the authenticated user's company.
     *
     * @param email   Email of the authenticated OWNER.
     * @param request DTO containing the instance name and the list of module
     *                keys selected by the user on the frontend.
     */
    public InstanceResponseDTO createInstance(String email, CreatedInstanceRequestDTO request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        InstanceErrorCodes.USER_NOT_FOUND,
                        "No user found with email: " + email));

        Company company = user.getCompany();
        String instanceName = company.getName();

        // ── Rule 1: One instance per company ─────────────────────────────────
        // Only RUNNING or STOPPED instances count — CREATING and ERROR are
        // failed/incomplete attempts and must not block a retry.
        boolean hasActiveInstance = instanceRepository.findByCompany(company)
                .stream()
                .anyMatch(i -> i.getStatus() == InstanceStatus.RUNNING
                        || i.getStatus() == InstanceStatus.STOPPED);

        if (hasActiveInstance) {
            throw new BusinessRuleViolationException(
                    InstanceErrorCodes.INSTANCE_ALREADY_EXISTS,
                    "Company '" + instanceName + "' already has an instance. " +
                            "Only one instance per company is allowed.");
        }

        // Clean up any stale CREATING or ERROR records from previous failed attempts
        // so they don't accumulate in the DB
        instanceRepository.deleteByCompanyAndStatusIn(
                company,
                java.util.List.of(InstanceStatus.CREATING, InstanceStatus.ERROR));

        // ── Rule 2: Subscription must be active to create an instance ─────────
        subscriptionRepository.findByCompany(company).ifPresent(sub -> {
            if (sub.getStatus() == SubscriptionStatus.SUSPENDED) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "Your subscription is suspended. Please contact support.");
            }
            if (sub.getStatus() == SubscriptionStatus.EXPIRED
                    || (sub.getEndDate() != null && sub.getEndDate().isBefore(java.time.LocalDateTime.now()))) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "Your subscription has expired. Please renew to create an instance.");
            }
            if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "An active subscription is required to create an instance.");
            }
        });

        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setCompany(company);
        instance.setModules(request.modules());
        instance.setStatus(InstanceStatus.CREATING);
        // NOTE: NOT saving to DB yet — we only persist once Docker succeeds
        // This prevents stale CREATING records from blocking future retries

        try {
            DockerResultDTO result = dockerService.startInstance(instanceName, request.modules());

            instance.setDockerContainerId(result.getAppContainerId());
            instance.setDbContainerId(result.getDbContainerId());
            instance.setUrl(result.getUrl());
            instance.setStatus(InstanceStatus.RUNNING);

        } catch (ExternalServiceException e) {
            // Do NOT save — let the user retry cleanly
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_START_FAILED,
                    "Failed to start Docker instance '" + instanceName + "'.",
                    e);
        }

        // Only reaches here on full success — all required fields are populated
        instanceRepository.save(instance);
        return mapToResponse(instance);
    }

    // ── Retrieve a single instance (with ownership check) ───────────────────

    public Instance getInstanceForUser(String email, Long instanceId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        InstanceErrorCodes.USER_NOT_FOUND,
                        "No user found with email: " + email));

        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        InstanceErrorCodes.INSTANCE_NOT_FOUND,
                        "No instance found with id: " + instanceId));

        if (user.getRole() == UserRole.ADMIN) {
            return instance;
        }

        if (!instance.getCompany().getId().equals(user.getCompany().getId())) {
            throw new ForbiddenActionException(
                    InstanceErrorCodes.INSTANCE_ACCESS_DENIED,
                    "You are not allowed to access instance with id: " + instanceId);
        }

        return instance;
    }

    // ── Retrieve the instance that belongs to the user's own company ─────────

    public InstanceResponseDTO getUserInstanceOnly(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        InstanceErrorCodes.USER_NOT_FOUND,
                        "No user found with email: " + email));

        // ── Rule 2: Block access if subscription is expired ───────────────────
        checkSubscriptionActive(user.getCompany());

        Instance instance = instanceRepository.findByName(user.getCompany().getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        InstanceErrorCodes.INSTANCE_NOT_FOUND,
                        "No instance found for company: " + user.getCompany().getName()));

        if (!instance.getCompany().getId().equals(user.getCompany().getId())) {
            throw new ForbiddenActionException(
                    InstanceErrorCodes.INSTANCE_ACCESS_DENIED,
                    "You are not allowed to access this instance.");
        }

        return mapToResponse(instance);
    }

    // ── Lifecycle operations ─────────────────────────────────────────────────

    public void startInstance(String email, Long instanceId) {

        Instance instance = getInstanceForUser(email, instanceId);

        try {
            dockerService.startInstanceContainer(instance.getName());
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_START_FAILED,
                    "Failed to start container for instance '" + instance.getName() + "'.",
                    e);
        }

        instance.setStatus(InstanceStatus.RUNNING);
        instanceRepository.save(instance);
    }

    public void stopInstance(String email, Long instanceId) {

        Instance instance = getInstanceForUser(email, instanceId);

        try {
            dockerService.stopInstanceContainer(instance.getName());
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_STOP_FAILED,
                    "Failed to stop container for instance '" + instance.getName() + "'.",
                    e);
        }

        instance.setStatus(InstanceStatus.STOPPED);
        instanceRepository.save(instance);
    }

    public void restartInstance(String email, Long instanceId) {

        Instance instance = getInstanceForUser(email, instanceId);

        try {
            dockerService.restartInstance(instance.getName());
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_RESTART_FAILED,
                    "Failed to restart container for instance '" + instance.getName() + "'.",
                    e);
        }

        instance.setStatus(InstanceStatus.RUNNING);
        instanceRepository.save(instance);
    }

    // ── Admin: list all instances ────────────────────────────────────────────

    public Page<InstanceResponseDTO> getInstances(String email, Pageable pageable) {

        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        InstanceErrorCodes.USER_NOT_FOUND,
                        "No user found with email: " + email));

        return instanceRepository.findAll(pageable).map(instance -> InstanceResponseDTO.builder()
                .id(instance.getId())
                .region(instance.getCompany().getCountry())
                .userEmail(instance.getCompany().getUsers().get(0).getEmail())
                .nameInstance(instance.getName())
                .status(instance.getStatus())
                .firstName(instance.getCompany().getUsers().get(0).getFirstName())
                .lastName(instance.getCompany().getUsers().get(0).getLastName())
                .modules(instance.getModules())
                .build());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Throws BusinessRuleViolationException if the company's subscription
     * is expired, suspended, or otherwise inactive.
     * Called before any operation that gives the user access to their instance.
     */
    private void checkSubscriptionActive(Company company) {
        subscriptionRepository.findByCompany(company).ifPresent(sub -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            if (sub.getStatus() == SubscriptionStatus.SUSPENDED) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "Your subscription is suspended. Please contact support.");
            }
            if (sub.getEndDate() != null && sub.getEndDate().isBefore(now)) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "Your subscription has expired. Please renew to access your instance.");
            }
            if (sub.getStatus() == SubscriptionStatus.EXPIRED) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "Your subscription has expired. Please renew to access your instance.");
            }
            if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
                throw new BusinessRuleViolationException(
                        InstanceErrorCodes.SUBSCRIPTION_REQUIRED,
                        "An active subscription is required to access your instance.");
            }
        });
    }

    private InstanceResponseDTO mapToResponse(Instance instance) {
        return InstanceResponseDTO.builder()
                .id(instance.getId())
                .nameInstance(instance.getName())
                .url(instance.getUrl())
                .region(instance.getCompany().getCountry())
                .userEmail(instance.getCompany().getUsers().get(0).getEmail())
                .firstName(instance.getCompany().getUsers().get(0).getFirstName())
                .lastName(instance.getCompany().getUsers().get(0).getLastName())
                .status(instance.getStatus())
                .modules(instance.getModules())   // include installed modules in every response
                .build();
    }
}