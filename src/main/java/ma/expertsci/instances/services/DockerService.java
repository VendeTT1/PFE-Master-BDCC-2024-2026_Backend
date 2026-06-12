package ma.expertsci.instances.services;

import ma.expertsci.account.entities.user.UserRole;
import ma.expertsci.account.entities.user.User;
import ma.expertsci.account.repository.CompanyRepository;
import ma.expertsci.account.repository.UserRepository;
import ma.expertsci.exception.ExternalServiceException;
import ma.expertsci.exception.ResourceNotFoundException;
import ma.expertsci.instances.dto.DockerResultDTO;
import ma.expertsci.instances.exception.InstanceErrorCodes;
import ma.expertsci.instances.repository.InstanceRepository;
import ma.expertsci.subscriptions.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DockerService {

    private static final String TEMPLATE_PATH = "docker/templates/";
    private static final String INSTANCE_PATH = "instances/";

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final InstanceRepository instanceRepository;

    public DockerService(UserRepository userRepository, SubscriptionRepository subscriptionRepository, CompanyRepository companyRepository, InstanceRepository instanceRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.companyRepository = companyRepository;
        this.instanceRepository = instanceRepository;
    }

    // ── Create and boot a brand-new instance ────────────────────────────────

    /**
     * @param instanceName  Unique name derived from the company name.
     * @param modules       Module keys selected by the user on the frontend
     *                      (e.g. ["sale", "purchase", "inventory"]).
     *                      "base" and "saas_sso" are always enforced by
     *                      {@link #buildModuleList(List)}.
     */
    public DockerResultDTO startInstance(String instanceName, List<String> modules) {

        // ── 1. Early validation ─────────────────────────────
        if (instanceName == null || instanceName.isBlank()) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_ERROR,
                    "Instance name is null or empty. Aborting instance creation.",
                    null
            );
        }

        // ── 2. Safety net: block if a real instance already exists ───────────
        boolean alreadyExists = !instanceRepository
                .findByCompany(companyRepository.findByName(instanceName))
                .stream()
                .filter(i -> i.getStatus() == ma.expertsci.instances.entities.InstanceStatus.RUNNING
                        || i.getStatus() == ma.expertsci.instances.entities.InstanceStatus.STOPPED)
                .toList()
                .isEmpty();
        if (alreadyExists) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_ERROR,
                    "A running instance already exists for company '" + instanceName + "'. Aborting.",
                    null
            );
        }

        try {
            // ── 3. Prepare instance parameters ───────────────
            String dbName        = instanceName + "_db";
            String dbPassword    = "123456";
            String adminPassword = "1234567";
            int port = generatePort();

            User ownerUser = userRepository
                    .findByCompanyNameAndRole(instanceName, UserRole.OWNER)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            InstanceErrorCodes.OWNER_NOT_FOUND,
                            "No OWNER user found for instance: " + instanceName
                    ));

            String ownerEmail = ownerUser.getEmail();

            // ── 4. Create instance folder and write config files ──
            Path instanceDir = Paths.get(INSTANCE_PATH + instanceName);
            Files.createDirectories(instanceDir);

            String compose = Files.readString(Paths.get(TEMPLATE_PATH + "docker-compose.yml.tpl"))
                    .replace("${INSTANCE_NAME}", instanceName)
                    .replace("${DB_NAME}", dbName)
                    .replace("${DB_PASSWORD}", dbPassword)
                    .replace("${OWNER_EMAIL}", ownerEmail);

            String odooConf = Files.readString(Paths.get(TEMPLATE_PATH + "odoo.conf.tpl"))
                    .replace("${ADMIN_PASSWORD}", adminPassword)
                    .replace("${DB_NAME}", dbName)
                    .replace("${DB_PASSWORD}", dbPassword);

            String dockerFileTemplate  = Files.readString(Paths.get(TEMPLATE_PATH + "Dockerfile.tpl"));
            String pythonFileTemplate  = Files.readString(Paths.get(TEMPLATE_PATH + "script.py.tpl"));
            String createStaffUserFile = Files.readString(Paths.get(TEMPLATE_PATH + "create_staff_user.py.tpl"));

            Files.writeString(instanceDir.resolve("docker-compose.yml"), compose);
            Files.writeString(instanceDir.resolve("odoo.conf"), odooConf);
            Files.writeString(instanceDir.resolve("Dockerfile"), dockerFileTemplate);
            Files.writeString(instanceDir.resolve("script.py"), pythonFileTemplate);
            Files.writeString(instanceDir.resolve("create_staff_user.py"), createStaffUserFile);

            // ── 5. Start Docker container ─────────────────────
            runProcess(instanceDir, "docker-compose", "up", "-d");
            Thread.sleep(30_000);

            // ── 6. Initialize Odoo with the user-selected modules ──
            initializeOdooInstance(instanceDir, dbName, instanceName, modules);
            Thread.sleep(30_000);

            runPythonScript(instanceName);

            generateNginxConfig(instanceName);
            updateHostsFile(instanceName);
            reloadNginx();

            String url = "http://" + instanceName.toLowerCase() + ".experts-itn.com";
            return new DockerResultDTO(instanceName + "_app", instanceName + "_db", url);

        } catch (ResourceNotFoundException | ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_ERROR,
                    "Docker instance creation failed for '" + instanceName + "'.",
                    e
            );
        }
    }

    // ── Start an existing instance container ─────────────────────────────────

    public void startInstanceContainer(String instanceName) {
        Path instanceDir = Paths.get(INSTANCE_PATH + instanceName);
        try {
            runProcess(instanceDir, "docker", "compose", "up", "-d", "--build");
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_START_FAILED,
                    "Failed to start container for instance '" + instanceName + "'.",
                    e);
        }
    }

    // ── Stop an existing instance container ──────────────────────────────────

    public void stopInstanceContainer(String instanceName) {
        Path instanceDir = Paths.get(INSTANCE_PATH + instanceName);
        try {
            runProcess(instanceDir, "docker", "compose", "down");
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_STOP_FAILED,
                    "Failed to stop container for instance '" + instanceName + "'.",
                    e);
        }
    }

    // ── Restart an instance container ────────────────────────────────────────

    public void restartInstance(String instanceName) {
        stopInstanceContainer(instanceName);
        startInstanceContainer(instanceName);
    }

    // ── Run the SSO Python bootstrap script ─────────────────────────────────

    public void runPythonScript(String instanceName) {
        Path instanceDir = Paths.get(INSTANCE_PATH + instanceName);
        try {
            runProcess(instanceDir,
                    "docker", "exec", instanceName + "_app",
                    "python3", "/script/script.py");
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.DOCKER_ERROR,
                    "Python SSO script failed for instance '" + instanceName + "'.",
                    e);
        }
    }

    // ── Nginx config generation ──────────────────────────────────────────────

    public void generateNginxConfig(String instanceName) {
        String safeInstanceName = instanceName.toLowerCase();
        try {
            Path nginxTemplatePath = Paths.get(TEMPLATE_PATH + "config-instance-nginx.conf.tpl");
            Path nginxConfPath     = Paths.get("nginx/conf.d/" + safeInstanceName + ".conf");
            Files.createDirectories(nginxConfPath.getParent());

            String nginxConfig = Files.readString(nginxTemplatePath)
                    .replace("${INSTANCE_NAME}", safeInstanceName);

            Files.writeString(nginxConfPath, nginxConfig);
        } catch (IOException e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.NGINX_FAILED,
                    "Failed to generate Nginx config for instance '" + instanceName + "'.",
                    e);
        }
    }

    public void updateHostsFile(String instanceName) {
        File hostsFile = new File("C:/Windows/System32/drivers/etc/hosts");
        String entry   = "127.0.0.1\t" + instanceName + ".experts-itn.com\n";

        try {
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

            if (!entryExists) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(hostsFile, true))) {
                    bw.write(entry);
                }
            }
        } catch (IOException e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.NGINX_FAILED,
                    "Failed to update hosts file for instance '" + instanceName + "'.",
                    e);
        }
    }

    public void reloadNginx() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "nginx_proxy", "nginx", "-s", "reload");
            pb.redirectErrorStream(true);
            int exitCode = pb.start().waitFor();
            if (exitCode != 0) {
                throw new ExternalServiceException(
                        InstanceErrorCodes.NGINX_FAILED,
                        "Nginx reload exited with code " + exitCode + ".", null);
            }
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.NGINX_FAILED,
                    "Nginx reload failed.",
                    e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Maps frontend module keys to their exact Odoo 18 technical names.
     *
     * Frontend keys (left) are what the instance creation form sends.
     * Odoo names (right) are what gets passed to `odoo -i`.
     *
     * Keys that are already correct Odoo names map to themselves.
     * Odoo silently ignores unknown names — this map catches mistakes early.
     */
    private static final java.util.Map<String, String> MODULE_NAME_MAP =
            java.util.Map.ofEntries(
                    // ── Frontend catalog keys (current) ──────────────────────────
                    java.util.Map.entry("sale_management",    "sale_management"),
                    java.util.Map.entry("purchase",           "purchase"),
                    java.util.Map.entry("stock",              "stock"),
                    java.util.Map.entry("point_of_sale",      "point_of_sale"),
                    java.util.Map.entry("account_accountant", "account_accountant"),
                    java.util.Map.entry("hr",                 "hr"),
                    java.util.Map.entry("saas_sso",           "saas_sso"),

                    // ── Legacy / alternative keys kept for safety ─────────────────
                    java.util.Map.entry("sale",               "sale_management"),
                    java.util.Map.entry("sales",              "sale_management"),
                    java.util.Map.entry("inventory",          "stock"),
                    java.util.Map.entry("account",            "account"),
                    java.util.Map.entry("accounting",         "account_accountant"),
                    java.util.Map.entry("invoicing",          "account"),
                    java.util.Map.entry("pos",                "point_of_sale"),

                    // ── Extended catalog (add to frontend as needed) ───────────────
                    java.util.Map.entry("crm",                "crm"),
                    java.util.Map.entry("project",            "project"),
                    java.util.Map.entry("hr_payroll",         "hr_payroll"),
                    java.util.Map.entry("payroll",            "hr_payroll"),
                    java.util.Map.entry("hr_timesheet",       "hr_timesheet"),
                    java.util.Map.entry("timesheet",          "hr_timesheet"),
                    java.util.Map.entry("hr_expense",         "hr_expense"),
                    java.util.Map.entry("expenses",           "hr_expense"),
                    java.util.Map.entry("mrp",                "mrp"),
                    java.util.Map.entry("manufacturing",      "mrp"),
                    java.util.Map.entry("helpdesk",           "helpdesk"),
                    java.util.Map.entry("website",            "website"),
                    java.util.Map.entry("website_sale",       "website_sale"),
                    java.util.Map.entry("ecommerce",          "website_sale")
            );

    /**
     * Builds the comma-separated module string to pass to {@code odoo -i}.
     * Rules:
     * <ul>
     *   <li>"base" is always the first entry.</li>
     *   <li>Each requested module key is translated via MODULE_NAME_MAP.</li>
     *   <li>Unknown keys are logged as warnings and skipped — not silently passed
     *       to Odoo where they would be ignored without feedback.</li>
     *   <li>"saas_sso" is always appended last if not already present.</li>
     * </ul>
     */
    private String buildModuleList(List<String> requestedModules) {
        List<String> modules = new ArrayList<>();
        modules.add("base");

        if (requestedModules != null) {
            for (String raw : requestedModules) {
                if (raw == null || raw.isBlank() || raw.equals("base")) continue;

                String normalized = raw.trim().toLowerCase();
                String odooName = MODULE_NAME_MAP.getOrDefault(normalized, null);

                if (odooName == null) {
                    // Unknown module — log and skip rather than passing garbage to Odoo
                    System.err.println("[DockerService] WARNING: Unknown module key '" + raw +
                            "' — skipping. Add it to MODULE_NAME_MAP if valid.");
                    continue;
                }

                if (!modules.contains(odooName)) {
                    modules.add(odooName);
                }
            }
        }

        if (!modules.contains("saas_sso")) {
            modules.add("saas_sso");
        }

        String result = String.join(",", modules);
        System.out.println("[DockerService] Installing modules: " + result);
        return result;
    }

    /**
     * Runs {@code odoo --stop-after-init} inside the compose stack to seed the
     * database with the selected modules.
     */
    private void initializeOdooInstance(Path instanceDir,
                                        String dbName,
                                        String instanceName,
                                        List<String> modules) {
        String moduleList = buildModuleList(modules);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "compose", "run", "--rm",
                    instanceName + "_app",
                    "odoo",
                    "-c", "/etc/odoo/odoo.conf",
                    "-d", dbName,
                    "-i", moduleList,
                    "--without-demo=all",
                    "--stop-after-init");

            pb.directory(instanceDir.toFile());
            pb.inheritIO();

            int exitCode = pb.start().waitFor();
            if (exitCode != 0) {
                throw new ExternalServiceException(
                        InstanceErrorCodes.ODOO_INIT_FAILED,
                        "Odoo initialization failed for instance '" + instanceName
                                + "' (exit code " + exitCode + ").", null);
            }
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    InstanceErrorCodes.ODOO_INIT_FAILED,
                    "Odoo initialization failed for instance '" + instanceName + "'.",
                    e);
        }
    }

    /**
     * Convenience wrapper: builds a {@link ProcessBuilder}, runs it in the given
     * directory, and waits for it to finish.
     */
    private void runProcess(Path workingDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        pb.start().waitFor();
    }

    private int generatePort() {
        return 8000 + new Random().nextInt(1000);
    }
}