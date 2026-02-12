package com.gym.fitness;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DevLauncher {
    private static final int BACKEND_PORT = 8080;
    private static final int FRONTEND_PORT = 3000;
    private static final long BACKEND_WAIT_TIMEOUT_MS = 90_000;
    private static final long FRONTEND_WAIT_TIMEOUT_MS = 60_000;
    private static final int REQUIRED_JAVA_MAJOR = 17;
    private static boolean browserOpened = false;

    public static void main(String[] args) {
        Path backendDir = Paths.get(System.getProperty("user.dir")).normalize();
        Path projectRoot = backendDir.getParent() != null ? backendDir.getParent() : backendDir;
        Path frontendDir = projectRoot.resolve("frontend").normalize();

        boolean autoSetupOnly = hasArg(args, "--auto-setup-only");
        boolean stopOnly = hasArg(args, "--stop-only");
        if (autoSetupOnly) {
            System.out.println("[1/1] Auto setup only mode...");
            runAutoSetup(projectRoot);
            return;
        }
        if (stopOnly) {
            System.out.println("[1/1] Stop only mode...");
            stopExistingServices(true);
            return;
        }

        System.out.println("[1/9] Running auto setup (if needed)...");
        if (shouldRunAutoSetup()) {
            runAutoSetup(projectRoot);
        } else {
            System.out.println("Auto setup skipped (environment looks ready).");
        }

        System.out.println("[2/9] Checking environment...");
        String mvnExecutable = resolveMavenExecutable();
        if (mvnExecutable == null) {
            System.err.println("Maven not found in PATH or IDEA. Set MAVEN_HOME or add mvn to PATH.");
            return;
        }
        List<String> npmCommand = resolveNpmCommand();
        if (npmCommand == null) {
            System.err.println("Node.js/npm not found in PATH or system locations. Set NODE_HOME or add npm to PATH.");
            return;
        }

        System.out.println("[3/9] Stopping existing frontend/backend processes...");
        stopExistingServices(false);

        System.out.println("[4/9] Ensuring database service...");
        ensureDatabaseRunning();

        System.out.println("[5/9] Building backend...");
        if (!runMavenCommandOrFail(backendDir, mvnExecutable, "-q", "-DskipTests", "package")) {
            return;
        }
        System.out.println("[6/9] Building frontend...");
        if (!ensureFrontendDeps(frontendDir, npmCommand)) {
            return;
        }
        if (!runNpmCommandOrFail(frontendDir, npmCommand, "run", "build")) {
            return;
        }

        System.out.println("[7/10] Starting backend service...");
        BackendProcess backendProcess = startBackendWithRetry(backendDir, mvnExecutable);
        if (backendProcess == null) {
            System.out.println("Backend failed to start. Please check logs.");
            return;
        }

        System.out.println("[8/10] Starting frontend dev server...");
        FrontendProcess frontendProcess = startFrontendWithRetry(frontendDir, npmCommand);
        if (frontendProcess == null) {
            System.out.println("Frontend failed to start. Please check logs.");
            stopManagedProcess(backendProcess.process);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopManagedProcess(frontendProcess.process);
            stopManagedProcess(backendProcess.process);
            forceStopPort(FRONTEND_PORT, "frontend", false);
            forceStopPort(BACKEND_PORT, "backend", false);
        }));

        System.out.println("[9/10] Waiting for frontend and opening browser...");
        if (frontendProcess.startedByUs) {
            openBrowserWhenReady(frontendProcess.url, frontendProcess.port, FRONTEND_WAIT_TIMEOUT_MS);
        } else {
            System.out.println("Frontend already running at " + frontendProcess.url);
        }

        System.out.println("[10/10] Services are running. Stop this run configuration to exit.");
        keepLauncherAlive(backendProcess, frontendProcess);
    }

    private static void runAutoSetup(Path projectRoot) {
        if (!isWindows()) {
            System.out.println("Auto setup skipped (non-Windows).");
            return;
        }
        boolean hasMissing = printMissingRequirements();
        if (!hasMissing) {
            System.out.println("Auto setup skipped (environment looks ready).");
            return;
        }
        Path batScript = projectRoot.resolve("auto-setup.bat");
        Path psScript = projectRoot.resolve("auto-setup.ps1");
        if (!Files.exists(batScript) && !Files.exists(psScript)) {
            System.out.println("Auto setup script not found: " + batScript);
            return;
        }
        List<String> command = new ArrayList<>();
        if (Files.exists(batScript)) {
            command.add("cmd");
            command.add("/c");
            command.add(batScript.toString());
        } else {
            command.add("powershell");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(psScript.toString());
        }
        CommandResult result = runCommand(projectRoot, command.toArray(new String[0]));
        if (result == null || result.exitCode != 0) {
            System.out.println("Auto setup finished with warnings. Continue with manual setup if needed.");
        }
    }

    private static boolean shouldRunAutoSetup() {
        if (!isWindows()) {
            return false;
        }
        String autoSetup = System.getenv("AUTO_SETUP");
        if ("0".equals(autoSetup)) {
            return false;
        }
        if (!meetsJavaRequirement()) {
            return true;
        }
        if (resolveMavenExecutable() == null) {
            return true;
        }
        if (resolveNodeExecutable() == null) {
            return true;
        }
        if (resolveNpmCommand() == null) {
            return true;
        }
        return !isMysqlAvailable();
    }

    private static boolean hasArg(String[] args, String target) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (String arg : args) {
            if (target.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean meetsJavaRequirement() {
        int major = getJavaMajorVersion();
        return major >= REQUIRED_JAVA_MAJOR;
    }

    private static int getJavaMajorVersion() {
        CommandResult result = runCommand(null, "java", "-version");
        if (result == null || result.exitCode != 0) {
            return -1;
        }
        return parseJavaMajorFromText(result.output);
    }

    private static int parseJavaMajorFromText(String text) {
        if (text == null) {
            return -1;
        }
        Pattern pattern = Pattern.compile("\"(\\d+)(?:\\.(\\d+))?.*\"");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            if (major == 1 && matcher.group(2) != null) {
                major = Integer.parseInt(matcher.group(2));
            }
            return major;
        }
        return -1;
    }

    private static boolean isMysqlAvailable() {
        if (!isWindows()) {
            CommandResult result = runCommand(null, "mysql", "--version");
            return result != null && result.exitCode == 0;
        }
        CommandResult result = runCommand(null, "cmd", "/c", "mysql", "--version");
        return result != null && result.exitCode == 0;
    }

    private static void stopExistingServices(boolean aggressiveFallback) {
        if (!isWindows()) {
            System.out.println("Port cleanup skipped (non-Windows).");
            return;
        }
        boolean backendStopped = forceStopPort(BACKEND_PORT, "backend", false);
        boolean frontendStopped = forceStopPort(FRONTEND_PORT, "frontend", false);
        if (aggressiveFallback && (!backendStopped || !frontendStopped)) {
            System.out.println("Port cleanup incomplete; falling back to process-name stop...");
            stopByImageName("java.exe", true);
            stopByImageName("node.exe", false);
            backendStopped = backendStopped || isPortAvailable(BACKEND_PORT);
            frontendStopped = frontendStopped || isPortAvailable(FRONTEND_PORT);
        }
        if (backendStopped && frontendStopped) {
            System.out.println("Frontend/backend ports are free.");
        } else {
            if (!backendStopped) {
                System.out.println("Backend port still in use: " + BACKEND_PORT);
            }
            if (!frontendStopped) {
                System.out.println("Frontend port still in use: " + FRONTEND_PORT);
            }
        }
    }

    private static Set<Integer> findPidsByPort(int port) {
        return findPidsByPort(port, true);
    }

    private static Set<Integer> findPidsByPort(int port, boolean requireListening) {
        Set<Integer> pids = new HashSet<>();
        CommandResult result = runCommand(null, "cmd", "/c", "netstat", "-ano", "-p", "tcp");
        if (result == null || result.exitCode != 0 || result.output == null) {
            return pids;
        }
        String portToken = ":" + port;
        String[] lines = result.output.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("TCP")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 5) {
                continue;
            }
            String localAddress = parts[1];
            String state = parts[3];
            String pidText = parts[4];
            if (requireListening && !"LISTENING".equalsIgnoreCase(state)) {
                continue;
            }
            if (!localAddress.endsWith(portToken) && !localAddress.contains("]:" + port)) {
                continue;
            }
            try {
                pids.add(Integer.parseInt(pidText));
            } catch (NumberFormatException ignored) {
                // ignore invalid pid
            }
        }
        return pids;
    }

    private static long getCurrentPid() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        if (jvmName == null) {
            return -1;
        }
        int atIndex = jvmName.indexOf('@');
        if (atIndex <= 0) {
            return -1;
        }
        try {
            return Long.parseLong(jvmName.substring(0, atIndex));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean ensureFrontendDeps(Path frontendDir, List<String> npmCommand) {
        File nodeModules = frontendDir.resolve("node_modules").toFile();
        if (nodeModules.exists()) {
            return true;
        }
        System.out.println("Installing frontend dependencies...");
        return runNpmCommandOrFail(frontendDir, npmCommand, "install");
    }

    private static void ensureDatabaseRunning() {
        if (!isWindows()) {
            System.out.println("Database service check skipped (non-Windows).");
            return;
        }
        String serviceName = System.getenv("DB_SERVICE_NAME");
        if (serviceName == null || serviceName.trim().isEmpty()) {
            serviceName = "MySQL80";
        }
        CommandResult queryResult = runCommand(null, "cmd", "/c", "sc", "query", serviceName);
        if (queryResult == null || queryResult.exitCode != 0) {
            System.err.println("Failed to query database service: " + serviceName);
            return;
        }
        if (queryResult.output.contains("RUNNING")) {
            System.out.println("Database service is running: " + serviceName);
            return;
        }
        System.out.println("Starting database service: " + serviceName);
        runCommand(null, "cmd", "/c", "net", "start", serviceName);
    }

    private static FrontendProcess startFrontendWithRetry(Path frontendDir, List<String> npmCommand) {
        if (!ensurePortAvailable(FRONTEND_PORT)) {
            System.out.println("Failed to free frontend port " + FRONTEND_PORT + ". Try running IDEA as Administrator.");
            return null;
        }
        Process process = startFrontendProcess(frontendDir, npmCommand, FRONTEND_PORT);
        if (process == null) {
            return null;
        }
        if (waitForPortOpen(FRONTEND_PORT, 5000)) {
            String url = buildFrontendUrl(FRONTEND_PORT);
            System.out.println("Frontend started on " + url);
            return new FrontendProcess(process, FRONTEND_PORT, url, true);
        }
        if (process.isAlive()) {
            process.destroy();
        }
        return null;
    }

    private static BackendProcess startBackendWithRetry(Path backendDir, String mvnExecutable) {
        if (!ensurePortAvailable(BACKEND_PORT)) {
            System.out.println("Failed to free backend port " + BACKEND_PORT + ". Try running IDEA as Administrator.");
            return null;
        }
        Process process = startBackendProcess(backendDir, mvnExecutable);
        if (process == null) {
            return null;
        }
        if (waitForPortOpen(BACKEND_PORT, BACKEND_WAIT_TIMEOUT_MS)) {
            System.out.println("Backend started on http://localhost:" + BACKEND_PORT);
            return new BackendProcess(process, true);
        }
        if (process.isAlive()) {
            process.destroy();
        }
        return null;
    }

    private static Process startBackendProcess(Path backendDir, String mvnExecutable) {
        List<String> command = wrapExecutable(mvnExecutable);
        command.add("spring-boot:run");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(backendDir.toFile());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            streamOutput(process.getInputStream());
            return process;
        } catch (IOException e) {
            System.err.println("Failed to start backend: " + e.getMessage());
            return null;
        }
    }

    private static Process startFrontendProcess(Path frontendDir, List<String> npmCommand, int port) {
        List<String> command = new ArrayList<>(npmCommand);
        command.addAll(Arrays.asList(
                "run", "dev", "--", "--port", String.valueOf(port), "--strictPort"
        ));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(frontendDir.toFile());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            streamOutput(process.getInputStream());
            return process;
        } catch (IOException e) {
            System.err.println("Failed to start frontend: " + e.getMessage());
            return null;
        }
    }

    private static void openBrowserWhenReady(String url, int port, long timeoutMs) {
        if (browserOpened) {
            return;
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isPortOpenLocal(port, 500)) {
                System.out.println("Frontend is ready: " + url);
                openBrowser(url);
                browserOpened = true;
                return;
            }
            sleep(1000);
        }
        System.out.println("Frontend not ready; open manually: " + url);
    }

    private static String buildFrontendUrl(int port) {
        return "http://localhost:" + port;
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port), 1);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean waitForPortOpen(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isPortOpenLocal(port, 500)) {
                return true;
            }
            sleep(250);
        }
        return false;
    }

    private static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isPortOpenLocal(int port, int timeoutMs) {
        if (isPortOpen("127.0.0.1", port, timeoutMs)) {
            return true;
        }
        return isPortOpen("::1", port, timeoutMs);
    }

    private static boolean ensurePortAvailable(int port) {
        if (!isWindows()) {
            return isPortAvailable(port);
        }
        return forceStopPort(port, "port", false);
    }

    private static boolean forceStopPort(int port, String label, boolean logFailure) {
        if (isPortAvailable(port)) {
            System.out.println("No running " + label + " process on port " + port + ".");
            return true;
        }
        long currentPid = getCurrentPid();
        for (int attempt = 1; attempt <= 3; attempt++) {
            Set<Integer> pids = findPidsByPort(port, false);
            if (currentPid > 0) {
                pids.remove((int) currentPid);
            }
            if (pids.isEmpty()) {
                if (isPortAvailable(port)) {
                    System.out.println(label + " port " + port + " is free.");
                    return true;
                }
                sleep(500);
                continue;
            }
            for (int pid : pids) {
                System.out.println("Stopping " + label + " process on port " + port + ": PID " + pid);
                CommandResult result = runCommand(null, "cmd", "/c", "taskkill", "/PID", String.valueOf(pid), "/F", "/T");
                if (result == null || result.exitCode != 0) {
                    System.out.println("Failed to stop PID " + pid + ". Try running IDEA as Administrator.");
                }
            }
            sleep(500);
            if (isPortAvailable(port)) {
                System.out.println(label + " port " + port + " is free.");
                return true;
            }
        }
        if (logFailure) {
            System.out.println("Failed to free " + label + " port " + port + ". Try running IDEA as Administrator.");
        }
        return false;
    }

    private static void stopByImageName(String imageName, boolean skipCurrentPid) {
        Set<Integer> pids = findPidsByImageName(imageName);
        if (pids.isEmpty()) {
            System.out.println("No running " + imageName + " processes found.");
            return;
        }
        long currentPid = getCurrentPid();
        boolean stoppedAny = false;
        for (int pid : pids) {
            if (skipCurrentPid && currentPid > 0 && pid == (int) currentPid) {
                continue;
            }
            CommandResult result = runCommand(null, "cmd", "/c", "taskkill", "/PID", String.valueOf(pid), "/F", "/T");
            if (result == null || result.exitCode != 0) {
                System.out.println("Failed to stop " + imageName + " PID " + pid + ".");
            } else {
                stoppedAny = true;
                System.out.println("Stopped " + imageName + " PID " + pid + ".");
            }
        }
        if (!stoppedAny && skipCurrentPid) {
            System.out.println("Only current process matched " + imageName + "; skipped.");
        }
    }

    private static Set<Integer> findPidsByImageName(String imageName) {
        Set<Integer> pids = new HashSet<>();
        if (!isWindows() || imageName == null || imageName.trim().isEmpty()) {
            return pids;
        }
        CommandResult result = runCommand(
                null,
                "cmd",
                "/c",
                "tasklist",
                "/FI",
                "IMAGENAME eq " + imageName,
                "/FO",
                "CSV",
                "/NH"
        );
        if (result == null || result.exitCode != 0 || result.output == null) {
            return pids;
        }
        Pattern pattern = Pattern.compile("^\"[^\"]+\",\"(\\d+)\"");
        String[] lines = result.output.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find()) {
                try {
                    pids.add(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    // ignore invalid pid
                }
            }
        }
        return pids;
    }

    private static void openBrowser(String url) {
        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                return;
            } catch (IOException e) {
                System.err.println("Desktop browse failed: " + e.getMessage());
            }
        }
        if (isWindows()) {
            runCommand(null, "cmd", "/c", "start", "", url);
            return;
        }
        if (isMac()) {
            runCommand(null, "open", url);
            return;
        }
        runCommand(null, "xdg-open", url);
    }

    private static String resolveMavenExecutable() {
        CommandResult inPath = runCommand(null, "mvn", "-v");
        if (inPath != null && inPath.exitCode == 0) {
            return "mvn";
        }
        String envHome = firstNonEmpty(System.getenv("MAVEN_HOME"), System.getenv("M2_HOME"));
        String envCandidate = resolveMavenFromHome(envHome);
        if (envCandidate != null) {
            return envCandidate;
        }
        String ideaHome = resolveIdeaHome();
        String ideaCandidate = resolveMavenFromIdeaHome(ideaHome);
        if (ideaCandidate != null) {
            return ideaCandidate;
        }
        String programFilesCandidate = resolveMavenFromProgramFiles();
        if (programFilesCandidate != null) {
            return programFilesCandidate;
        }
        return null;
    }

    private static List<String> resolveNpmCommand() {
        if (isWindows()) {
            CommandResult inPath = runCommand(null, "cmd", "/c", "npm", "-v");
            if (inPath != null && inPath.exitCode == 0) {
                return new ArrayList<>(Arrays.asList("cmd", "/c", "npm"));
            }
        } else {
            CommandResult inPath = runCommand(null, "npm", "-v");
            if (inPath != null && inPath.exitCode == 0) {
                return new ArrayList<>(Arrays.asList("npm"));
            }
        }
        String envHome = firstNonEmpty(
                System.getenv("NODE_HOME"),
                System.getenv("NODEJS_HOME"),
                System.getenv("NVM_HOME"),
                System.getenv("NVM_SYMLINK")
        );
        String homeCandidate = resolveNpmFromHome(envHome);
        if (homeCandidate != null) {
            return wrapExecutable(homeCandidate);
        }
        String appDataCandidate = resolveNpmFromAppData();
        if (appDataCandidate != null) {
            return wrapExecutable(appDataCandidate);
        }
        String programFilesCandidate = resolveNpmFromProgramFiles();
        if (programFilesCandidate != null) {
            return wrapExecutable(programFilesCandidate);
        }
        String nodeExecutable = resolveNodeExecutable();
        if (nodeExecutable != null) {
            Path nodeDir = Paths.get(nodeExecutable).getParent();
            if (nodeDir != null) {
                Path npmCmd = nodeDir.resolve(isWindows() ? "npm.cmd" : "npm");
                if (Files.exists(npmCmd)) {
                    return wrapExecutable(npmCmd.toString());
                }
                Path npmCli = nodeDir.resolve("node_modules").resolve("npm").resolve("bin").resolve("npm-cli.js");
                if (Files.exists(npmCli)) {
                    List<String> command = new ArrayList<>();
                    command.add(nodeExecutable);
                    command.add(npmCli.toString());
                    return command;
                }
            }
        }
        return null;
    }

    private static String resolveMavenFromHome(String home) {
        if (home == null || home.trim().isEmpty()) {
            return null;
        }
        Path base = Paths.get(home);
        Path candidate = base.resolve("bin").resolve(isWindows() ? "mvn.cmd" : "mvn");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return null;
    }

    private static String resolveNpmFromHome(String home) {
        if (home == null || home.trim().isEmpty()) {
            return null;
        }
        Path base = Paths.get(home);
        if (isWindows()) {
            Path direct = base.resolve("npm.cmd");
            if (Files.exists(direct)) {
                return direct.toString();
            }
            Path bin = base.resolve("bin").resolve("npm.cmd");
            if (Files.exists(bin)) {
                return bin.toString();
            }
            return null;
        }
        Path unix = base.resolve("bin").resolve("npm");
        if (Files.exists(unix)) {
            return unix.toString();
        }
        return null;
    }

    private static String resolveNodeExecutable() {
        CommandResult inPath = runCommand(null, "node", "-v");
        if (inPath != null && inPath.exitCode == 0) {
            return "node";
        }
        String envHome = firstNonEmpty(
                System.getenv("NODE_HOME"),
                System.getenv("NODEJS_HOME"),
                System.getenv("NVM_SYMLINK"),
                System.getenv("NVM_HOME")
        );
        String homeCandidate = resolveNodeFromHome(envHome);
        if (homeCandidate != null) {
            return homeCandidate;
        }
        String localAppDataCandidate = resolveNodeFromLocalAppData();
        if (localAppDataCandidate != null) {
            return localAppDataCandidate;
        }
        String programFilesCandidate = resolveNodeFromProgramFiles();
        if (programFilesCandidate != null) {
            return programFilesCandidate;
        }
        return null;
    }

    private static String resolveNodeFromHome(String home) {
        if (home == null || home.trim().isEmpty()) {
            return null;
        }
        Path base = Paths.get(home);
        Path candidate = base.resolve(isWindows() ? "node.exe" : "node");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        Path binCandidate = base.resolve("bin").resolve(isWindows() ? "node.exe" : "node");
        if (Files.exists(binCandidate)) {
            return binCandidate.toString();
        }
        return null;
    }

    private static String resolveNodeFromLocalAppData() {
        if (!isWindows()) {
            return null;
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.trim().isEmpty()) {
            return null;
        }
        Path candidate = Paths.get(localAppData, "Programs", "nodejs", "node.exe");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return null;
    }

    private static String resolveNodeFromProgramFiles() {
        if (!isWindows()) {
            return null;
        }
        String[] roots = new String[] {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)")
        };
        for (String root : roots) {
            if (root == null || root.trim().isEmpty()) {
                continue;
            }
            Path candidate = Paths.get(root, "nodejs", "node.exe");
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }
        return null;
    }

    private static String resolveMavenFromIdeaHome(String ideaHome) {
        if (ideaHome == null || ideaHome.trim().isEmpty()) {
            return null;
        }
        Path candidate = Paths.get(ideaHome)
                .resolve("plugins")
                .resolve("maven")
                .resolve("lib")
                .resolve("maven3")
                .resolve("bin")
                .resolve(isWindows() ? "mvn.cmd" : "mvn");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return null;
    }

    private static String resolveIdeaHome() {
        String ideaHome = System.getProperty("idea.home.path");
        if (ideaHome != null && !ideaHome.trim().isEmpty()) {
            return ideaHome;
        }
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-javaagent:") && arg.contains("idea_rt.jar")) {
                String pathPart = arg.substring("-javaagent:".length());
                int equalsIndex = pathPart.indexOf('=');
                if (equalsIndex > 0) {
                    pathPart = pathPart.substring(0, equalsIndex);
                }
                Path ideaAgent = Paths.get(pathPart);
                Path parent = ideaAgent.getParent();
                if (parent != null && "lib".equalsIgnoreCase(parent.getFileName().toString())) {
                    Path home = parent.getParent();
                    if (home != null && Files.isDirectory(home)) {
                        return home.toString();
                    }
                }
            }
        }
        return null;
    }

    private static String resolveMavenFromProgramFiles() {
        if (!isWindows()) {
            return null;
        }
        String[] roots = new String[] {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)")
        };
        for (String root : roots) {
            if (root == null || root.trim().isEmpty()) {
                continue;
            }
            Path jetbrains = Paths.get(root, "JetBrains");
            if (!Files.isDirectory(jetbrains)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(jetbrains, "IntelliJ IDEA*")) {
                for (Path ideaDir : stream) {
                    String candidate = resolveMavenFromIdeaHome(ideaDir.toString());
                    if (candidate != null) {
                        return candidate;
                    }
                }
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private static String resolveNpmFromProgramFiles() {
        if (!isWindows()) {
            return null;
        }
        String[] roots = new String[] {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)")
        };
        for (String root : roots) {
            if (root == null || root.trim().isEmpty()) {
                continue;
            }
            Path candidate = Paths.get(root, "nodejs", "npm.cmd");
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }
        return null;
    }

    private static String resolveNpmFromAppData() {
        if (!isWindows()) {
            return null;
        }
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.trim().isEmpty()) {
            return null;
        }
        Path candidate = Paths.get(appData, "npm", "npm.cmd");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return null;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static boolean printMissingRequirements() {
        List<String> missing = new ArrayList<>();
        int javaMajor = getJavaMajorVersion();
        if (javaMajor < REQUIRED_JAVA_MAJOR) {
            String current = javaMajor > 0 ? " (current: " + javaMajor + ")" : " (not found)";
            missing.add("JDK " + REQUIRED_JAVA_MAJOR + "+ " + current);
        }
        if (resolveMavenExecutable() == null) {
            missing.add("Maven 3.6+");
        }
        if (resolveNodeExecutable() == null) {
            missing.add("Node.js 16+");
        }
        if (resolveNpmCommand() == null) {
            missing.add("npm (bundled with Node.js)");
        }
        if (!isMysqlAvailable()) {
            missing.add("MySQL 8.0+");
        }
        if (missing.isEmpty()) {
            System.out.println("All required tools are installed.");
            return false;
        }
        System.out.println("Missing required tools:");
        for (String item : missing) {
            System.out.println(" - " + item);
        }
        return true;
    }

    private static boolean runMavenCommandOrFail(Path workingDir, String mvnExecutable, String... args) {
        List<String> command = new ArrayList<>();
        if (isWindows() && (mvnExecutable.endsWith(".cmd") || mvnExecutable.endsWith(".bat"))) {
            command.add("cmd");
            command.add("/c");
            command.add(mvnExecutable);
        } else {
            command.add(mvnExecutable);
        }
        for (String arg : args) {
            command.add(arg);
        }
        return runCommandOrFail(workingDir, command.toArray(new String[0]));
    }

    private static boolean runNpmCommandOrFail(Path workingDir, List<String> npmCommand, String... args) {
        List<String> command = new ArrayList<>(npmCommand);
        command.addAll(Arrays.asList(args));
        return runCommandOrFail(workingDir, command.toArray(new String[0]));
    }

    private static List<String> wrapExecutable(String executable) {
        if (isWindows() && (executable.endsWith(".cmd") || executable.endsWith(".bat"))) {
            return new ArrayList<>(Arrays.asList("cmd", "/c", executable));
        }
        return new ArrayList<>(Arrays.asList(executable));
    }

    private static boolean runCommandOrFail(Path workingDir, String... command) {
        CommandResult result = runCommand(workingDir, command);
        if (result == null) {
            System.err.println("Failed to run command.");
            return false;
        }
        if (result.exitCode != 0) {
            System.err.println("Command failed: " + String.join(" ", command));
            if (result.output != null && !result.output.trim().isEmpty()) {
                System.err.println(result.output.trim());
            }
            return false;
        }
        return true;
    }

    private static CommandResult runCommand(Path workingDir, String... command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDir != null) {
            builder.directory(workingDir.toFile());
        }
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = readAll(process.getInputStream());
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String readAll(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString();
    }

    private static void streamOutput(InputStream inputStream) {
        Thread thread = new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    System.out.write(buffer, 0, read);
                    System.out.flush();
                }
            } catch (IOException e) {
                System.err.println("Failed to read process output: " + e.getMessage());
            }
        }, "frontend-output");
        thread.setDaemon(true);
        thread.start();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void stopManagedProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        sleep(1000);
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static void keepLauncherAlive(BackendProcess backendProcess, FrontendProcess frontendProcess) {
        while (true) {
            if (backendProcess != null && backendProcess.process != null && !backendProcess.process.isAlive()) {
                System.out.println("Backend process exited. Stopping launcher...");
                return;
            }
            if (frontendProcess != null && frontendProcess.process != null && !frontendProcess.process.isAlive()) {
                System.out.println("Frontend process exited. Stopping launcher...");
                return;
            }
            sleep(1000);
        }
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("win");
    }

    private static boolean isMac() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("mac");
    }

    private static class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private static class BackendProcess {
        private final Process process;
        private final boolean startedByUs;

        private BackendProcess(Process process, boolean startedByUs) {
            this.process = process;
            this.startedByUs = startedByUs;
        }
    }

    private static class FrontendProcess {
        private final Process process;
        private final int port;
        private final String url;
        private final boolean startedByUs;

        private FrontendProcess(Process process, int port, String url, boolean startedByUs) {
            this.process = process;
            this.port = port;
            this.url = url;
            this.startedByUs = startedByUs;
        }
    }
}
