package ma.expertsci.instances.services;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.DockerResultDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.entities.InstanceStatus;
import ma.expertsci.instances.repository.InstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    private final DockerService dockerService;

    public InstanceResponseDTO createInstance(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Company company = user.getCompany();

        String instanceName = company.getName();

        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setCompany(company);
        instance.setStatus(InstanceStatus.CREATING);

        instanceRepository.save(instance);

        try {
            DockerResultDTO result = dockerService.startInstance(instanceName);

            instance.setDockerContainerId(result.getAppContainerId());
            instance.setDbContainerId(result.getDbContainerId());
            instance.setUrl(result.getUrl());
            instance.setStatus(InstanceStatus.RUNNING);

        } catch (Exception e) {
            e.printStackTrace();
            instance.setStatus(InstanceStatus.ERROR);
        }

        instanceRepository.save(instance);

        return mapToResponse(instance);
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
                .build();
    }

    public Instance getInstanceForUser(String email, Long instanceId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow();

        if (user.getRole() == UserRole.ADMIN) {
            return instance;
        }
        if (!instance.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Unauthorized access to instance");
        }

        return instance;
    }

    public InstanceResponseDTO getUserInstanceOnly(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Instance instance = instanceRepository.findByName(user.getCompany().getName())
                .orElseThrow();

        if (!instance.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Unauthorized access to instance");
        }

        return mapToResponse(instance);


    }

    public void startInstance(String email, Long instanceId) throws Exception {

        Instance instance = getInstanceForUser(email, instanceId);

        dockerService.startInstanceContainer(instance.getName());

        instance.setStatus(InstanceStatus.RUNNING);
        instanceRepository.save(instance);
    }
    public void stopInstance(String email, Long instanceId) throws Exception {

        Instance instance = getInstanceForUser(email, instanceId);

        dockerService.stopInstanceContainer(instance.getName());

        instance.setStatus(InstanceStatus.STOPPED);
        instanceRepository.save(instance);
    }
    public void restartInstance(String email, Long instanceId) throws Exception {

        Instance instance = getInstanceForUser(email, instanceId);

        dockerService.restartInstance(instance.getName());

        instance.setStatus(InstanceStatus.RUNNING);
        instanceRepository.save(instance);
    }
    public Page<InstanceResponseDTO> getInstances(String email, Pageable pageable) {

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Page<Instance> instancesPage = instanceRepository.findAll(pageable);

        return instancesPage.map(instance -> InstanceResponseDTO.builder()
                .id(instance.getId())
                .region(instance.getCompany().getCountry())
                .userEmail(instance.getCompany().getUsers().get(0).getEmail())
                .nameInstance(instance.getName())
                .status(instance.getStatus())
                .firstName(instance.getCompany().getUsers().get(0).getFirstName())
                .lastName(instance.getCompany().getUsers().get(0).getLastName())
                .build()
        );
    }
}
