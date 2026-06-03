package ma.expertsci.instances.services;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.exception.ExternalServiceException;
import ma.expertsci.exception.ForbiddenActionException;
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


    public InstanceService(InstanceRepository instanceRepository, UserRepository userRepository, DockerService dockerService) {
        this.instanceRepository = instanceRepository;
        this.userRepository = userRepository;
        this.dockerService = dockerService;
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

        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setCompany(company);
        instance.setModules(request.modules());   // persist selected modules
        instance.setStatus(InstanceStatus.CREATING);
        instanceRepository.save(instance);

        try {
            // Pass the user-selected module list down to the Docker layer
            DockerResultDTO result = dockerService.startInstance(instanceName, request.modules());

            instance.setDockerContainerId(result.getAppContainerId());
            instance.setDbContainerId(result.getDbContainerId());
            instance.setUrl(result.getUrl());
            instance.setStatus(InstanceStatus.RUNNING);

        } catch (ExternalServiceException e) {
            instance.setStatus(InstanceStatus.ERROR);
            instanceRepository.save(instance);
            throw e;
        } catch (Exception e) {
            instance.setStatus(InstanceStatus.ERROR);
            instanceRepository.save(instance);
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_START_FAILED,
                    "Failed to start Docker instance '" + instanceName + "'.",
                    e);
        }

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