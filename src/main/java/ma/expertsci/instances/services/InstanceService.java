package ma.expertsci.instances.services;

import lombok.RequiredArgsConstructor;
import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.instances.dto.CreatedInstanceRequestDTO;
import ma.expertsci.instances.dto.DockerResultDTO;
import ma.expertsci.instances.dto.InstanceResponseDTO;
import ma.expertsci.instances.entities.Instance;
import ma.expertsci.instances.entities.InstanceStatus;
import ma.expertsci.instances.repository.InstanceRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final UserRepository userRepository;
    private final DockerService dockerService;

    public InstanceResponseDTO createInstance(String email, CreatedInstanceRequestDTO request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Company company = user.getCompany();

        String instanceName = company.getName();

        Instance instance = new Instance();
        instance.setName(request.name());
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
            instance.setStatus(InstanceStatus.ERROR);
        }

        instanceRepository.save(instance);

        return mapToResponse(instance);
    }

    private InstanceResponseDTO mapToResponse(Instance instance) {
        return InstanceResponseDTO.builder()
                .id(instance.getId())
                .name(instance.getName())
                .url(instance.getUrl())
                .status(instance.getStatus().name())
                .build();
    }
}
