package ma.expertsci.instances.services;

import ma.expertsci.instances.dto.DockerResultDTO;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;

@Service
public class DockerService {

    private static final String TEMPLATE_PATH = "docker/templates/";
    private static final String INSTANCE_PATH = "instances/";

    public DockerResultDTO startInstance(String instanceName) {

        try {
            // 1️⃣ Generate values
            String dbName = instanceName +"_db";
            String dbPassword = "123456";//UUID.randomUUID().toString();
            String adminPassword = "1234567";//UUID.randomUUID().toString();
            int port = generatePort();

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

            // 4️⃣ Replace variables
            String compose = dockerComposeTemplate
                    .replace("${INSTANCE_NAME}", instanceName)
                    .replace("${DB_NAME}", dbName)
                    .replace("${DB_PASSWORD}", dbPassword)
                    .replace("${PORT}", String.valueOf(port));

            String odooConf = odooConfigTemplate
                    .replace("${ADMIN_PASSWORD}", adminPassword)
                    .replace("${DB_NAME}", dbName)
                    .replace("${DB_PASSWORD}", dbPassword);

            String dockerFile = dockerFileTemplate;

            // 5️⃣ Write files
            Files.writeString(instanceDir.resolve("docker-compose.yml"), compose);
            Files.writeString(instanceDir.resolve("odoo.conf"), odooConf);
            Files.writeString(instanceDir.resolve("Dockerfile"), dockerFile);

            // 6️⃣ Run docker-compose
            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose", "up", "-d"
            );

            pb.directory(instanceDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            process.waitFor();

            Thread.sleep(30000);

            initializeOdooInstance(instanceDir, dbName, instanceName);

            // 7️⃣ Build URL
            String url = "http://localhost:" + port;

            return new DockerResultDTO(
                    instanceName + "_odoo",
                    instanceName + "_db",
                    url
            );

        } catch (Exception e) {
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
                "-i", "base,sale,purchase,inventory,point_of_sale",
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
                "docker", "compose", "up", "-d"
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

}