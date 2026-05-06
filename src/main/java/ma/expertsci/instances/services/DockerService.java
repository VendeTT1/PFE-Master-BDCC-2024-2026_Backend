package ma.expertsci.instances.services;

import ma.expertsci.account.entities.company.Company;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.account.service.CompanyService;
import ma.expertsci.instances.dto.DockerResultDTO;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;

@Service
public class DockerService {

    private static final String TEMPLATE_PATH = "docker/templates/";
    private static final String INSTANCE_PATH = "instances/";
    private final UserRepository userRepository;

    public DockerService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public DockerResultDTO startInstance(String instanceName) {
        try {
            // 1️⃣ Generate values
            String dbName = instanceName + "_db";
            String dbPassword = "123456"; //UUID.randomUUID().toString();
            String adminPassword = "1234567"; //UUID.randomUUID().toString();

            int port = generatePort();

//
//            // 4️⃣ Retrieve the owner email based on instance name
            // Fetch the user (owner) based on instance name and 'owner' role
            User ownerUser = userRepository.findByCompanyNameAndRole(instanceName, UserRole.OWNER )
                    .orElseThrow(() -> new RuntimeException("Owner user not found for instance name: " + instanceName));
            String ownerEmail = ownerUser.getEmail(); // Get the email of the owner
            String ownerCompany = ownerUser.getCompany().getName();

            // 2️⃣ Create instance folder
            Path instanceDir = Paths.get(INSTANCE_PATH + instanceName);
            Files.createDirectories(instanceDir);

            // 3️⃣ Load templates
            String dockerComposeTemplate = Files.readString(
                    Paths.get(TEMPLATE_PATH + "docker-compose.yml.tpl"));

            String odooConfigTemplate = Files.readString(
                    Paths.get(TEMPLATE_PATH + "odoo.conf.tpl"));

            // to install jwt python package for sso
            String dockerFileTemplate = Files.readString(
                    Paths.get(TEMPLATE_PATH + "Dockerfile.tpl"));

            String pythonFileTemplate = Files.readString(
                    Paths.get(TEMPLATE_PATH + "script.py.tpl")
            );

            String createStaffUserFile = Files.readString(
                    Paths.get(TEMPLATE_PATH + "create_staff_user.py.tpl")
            );


            // 5️⃣ Replace variables in the templates
            String compose = dockerComposeTemplate
                    .replace("${INSTANCE_NAME}", instanceName)
                    .replace("${DB_NAME}", dbName)
                    .replace("${DB_PASSWORD}", dbPassword)
                    .replace("${OWNER_EMAIL}",ownerEmail);
//                    .replace("${PORT}", String.valueOf(port)


            String odooConf = odooConfigTemplate
                    .replace("${ADMIN_PASSWORD}", adminPassword)
                    .replace("${DB_NAME}", dbName)
                    .replace("${DB_PASSWORD}", dbPassword);

            // 6️⃣ Write files
            Files.writeString(instanceDir.resolve("docker-compose.yml"), compose);
            Files.writeString(instanceDir.resolve("odoo.conf"), odooConf);
            Files.writeString(instanceDir.resolve("Dockerfile"), dockerFileTemplate);
            Files.writeString(instanceDir.resolve("script.py"), pythonFileTemplate);
            Files.writeString(instanceDir.resolve("create_staff_user.py"), createStaffUserFile);

            // 7️⃣ Run docker-compose
            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose", "up", "-d"
            );

            pb.directory(instanceDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            process.waitFor();

            Thread.sleep(30000); // Waiting to ensure the container is fully up and running

            // 8️⃣ Initialize Odoo instance
            initializeOdooInstance(instanceDir, dbName, instanceName);

            Thread.sleep(30000);

            runPythonScript(instanceName);
            // Nginx/domain setup
            generateNginxConfig(instanceName);
            updateHostsFile(instanceName);
            reloadNginx();

            // Build domain URL
            String safeInstanceName = instanceName.toLowerCase();
            String url = "http://" + safeInstanceName + ".experts-itn.com";

            return new DockerResultDTO(
                    instanceName + "_app",
                    instanceName + "_db",
                    url
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Docker instance creation failed", e);
        }
    }
    // Simple port generator (later we improve)
    private int generatePort() {
        return 8000 + new Random().nextInt(1000);
    }

    private void initializeOdooInstance(Path instanceDir, String dbName, String instanceName) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "run", "--rm",
                instanceName+"_app",
                "odoo",
                "-c", "/etc/odoo/odoo.conf",
                "-d", dbName,
                "-i", "base,sale,purchase,inventory,point_of_sale,saas_sso",
                "--without-demo=all",
                "--stop-after-init"
        );

        pb.directory(instanceDir.toFile());
        pb.inheritIO(); // shows logs in console

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Odoo initialization failed");
        }
    }

    public void startInstanceContainer(String instanceName) throws Exception {
        Path instanceDir = Paths.get("instances/" + instanceName);

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "up", "-d", "--build"
        );

        pb.directory(instanceDir.toFile());
        pb.inheritIO();

        Process process = pb.start();
        process.waitFor();
    }

    public void stopInstanceContainer(String instanceName) throws Exception {

        Path instanceDir = Paths.get("instances/" + instanceName);

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose", "down"
        );

        pb.directory(instanceDir.toFile());
        pb.inheritIO();

        Process process = pb.start();
        process.waitFor();
    }

    public void restartInstance(String instanceName) throws Exception {

        stopInstanceContainer(instanceName);
        startInstanceContainer(instanceName);
    }

    public void runPythonScript(String instanceName) throws Exception {

        Path instanceDir = Paths.get("instances/" + instanceName);

        ProcessBuilder pbPython = new ProcessBuilder(
                "docker","exec",instanceName + "_app",
                        "python3","/script/script.py"
        );

        pbPython.directory(instanceDir.toFile());
        pbPython.redirectErrorStream(true);

        Process processPython = pbPython.start();
        processPython.waitFor();

    }

    public void generateNginxConfig(String instanceName) throws IOException {
        String safeInstanceName = instanceName.toLowerCase();

        Path nginxTemplatePath = Paths.get(TEMPLATE_PATH + "config-instance-nginx.conf.tpl");

        Path nginxConfPath = Paths.get(
                "nginx/conf.d/" + safeInstanceName + ".conf"
        );

        Files.createDirectories(nginxConfPath.getParent());

        String nginxTemplate = Files.readString(nginxTemplatePath);

        String nginxConfig = nginxTemplate
                .replace("${INSTANCE_NAME}", safeInstanceName);

        Files.writeString(nginxConfPath, nginxConfig);

        System.out.println("Nginx config generated at: " + nginxConfPath.toAbsolutePath());
    }

    public void updateHostsFile(String instanceName) throws IOException {
        // Define the hosts file path
        File hostsFile = new File("C:/Windows/System32/drivers/etc/hosts");

        // Define the entry we need to add (pointing to localhost)
        String entry = "127.0.0.1\t" + instanceName + ".experts-itn.com\n";

        // Check if the entry already exists
        boolean entryExists = false;
        try (BufferedReader br = new BufferedReader(new FileReader(hostsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(instanceName + ".experts-itn.com")) {
                    entryExists = true;
                    break;
                }
            }
        }

        // If the entry doesn't exist, append it to the hosts file
        if (!entryExists) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(hostsFile, true))) {
                bw.write(entry);
            }
            System.out.println("Hosts file updated with: " + entry);
//            return entry;
        } else {
            System.out.println("Entry already exists in hosts file.");
//            return "Entry already exists in hosts file.";
        }
    }

    public void reloadNginx() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", "nginx_proxy", "nginx", "-s", "reload"
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Nginx reload failed");
        }

        System.out.println("Nginx reloaded successfully");
    }
}