package com.fallguys.pipeline;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation tests for the Jenkinsfile pipeline configuration.
 * Tests verify pipeline structure, stages, environment variables, credentials, and deployment logic.
 */
@DisplayName("Jenkinsfile Validation Tests")
public class JenkinsfileValidationTest {

    private static String jenkinsfileContent;
    private static List<String> jenkinsfileLines;
    private static final String JENKINSFILE_PATH = "../../../../Jenkinsfile";

    @BeforeAll
    static void setUp() throws IOException {
        // Navigate up from freebridge/app-main/src/test to project root
        Path testClassPath = Paths.get(JenkinsfileValidationTest.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParent();
        Path projectRoot = testClassPath;

        // Find Jenkinsfile - try multiple potential locations
        Path jenkinsfilePath = null;

        // Try relative path from test resources
        Path relPath = Paths.get("Jenkinsfile");
        if (Files.exists(relPath)) {
            jenkinsfilePath = relPath;
        } else {
            // Try from current working directory
            relPath = Paths.get("../../../../Jenkinsfile");
            if (Files.exists(relPath)) {
                jenkinsfilePath = relPath;
            } else {
                // Try absolute path construction
                String currentDir = System.getProperty("user.dir");
                // If we're in freebridge/app-main, go up two levels
                if (currentDir.contains("/app-main")) {
                    jenkinsfilePath = Paths.get(currentDir).getParent().getParent().resolve("Jenkinsfile");
                } else if (currentDir.contains("/freebridge")) {
                    jenkinsfilePath = Paths.get(currentDir).getParent().resolve("Jenkinsfile");
                } else {
                    jenkinsfilePath = Paths.get(currentDir, "Jenkinsfile");
                }
            }
        }

        assertTrue(Files.exists(jenkinsfilePath),
            "Jenkinsfile must exist. Tried path: " + jenkinsfilePath.toAbsolutePath());
        jenkinsfileContent = Files.readString(jenkinsfilePath);
        jenkinsfileLines = Files.readAllLines(jenkinsfilePath);
    }

    @Test
    @DisplayName("Should have valid pipeline structure")
    void testPipelineStructure() {
        assertTrue(jenkinsfileContent.contains("pipeline {"), "Must be a declarative pipeline");
        assertTrue(jenkinsfileContent.contains("agent any"), "Must declare an agent");
        assertTrue(jenkinsfileContent.contains("stages {"), "Must have stages block");
        assertTrue(jenkinsfileContent.contains("post {"), "Must have post-build actions");
    }

    @Test
    @DisplayName("Should have GitHub push trigger configured")
    void testGitHubTrigger() {
        assertTrue(jenkinsfileContent.contains("triggers {"), "Must have triggers block");
        assertTrue(jenkinsfileContent.contains("githubPush()"), "Must have GitHub push trigger");
    }

    @Test
    @DisplayName("Should have all required environment variables")
    void testEnvironmentVariables() {
        assertTrue(jenkinsfileContent.contains("environment {"), "Must have environment block");

        // Java encoding
        assertTrue(jenkinsfileContent.contains("JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'"),
                "Must set Java file encoding to UTF-8");

        // Manifest repository configuration
        assertTrue(jenkinsfileContent.contains("CRED_ID_MANIFEST = 'github-manifest-key'"),
                "Must define manifest repository credential ID");
        assertTrue(jenkinsfileContent.contains("MANIFEST_REPO_URL ="),
                "Must define manifest repository URL");
        assertTrue(jenkinsfileContent.contains("git@github.com"),
                "Manifest repo URL must use SSH protocol");

        // Docker configuration
        assertTrue(jenkinsfileContent.contains("IMAGE_NAME = 'o2ppo/freebrback001'"),
                "Must define Docker image name");
        assertTrue(jenkinsfileContent.contains("DOCKER_CRED_ID = credentials('dockerhub-credentials')"),
                "Must define Docker Hub credentials");

        // Git configuration
        assertTrue(jenkinsfileContent.contains("GIT_EMAIL ="),
                "Must define Git email for commits");
    }

    @Test
    @DisplayName("Should have Checkout Code stage")
    void testCheckoutStage() {
        assertTrue(jenkinsfileContent.contains("stage('Checkout Code')"),
                "Must have Checkout Code stage");
        assertTrue(jenkinsfileContent.contains("cleanWs()"),
                "Must clean workspace before checkout");
        assertTrue(jenkinsfileContent.contains("checkout scm"),
                "Must checkout source code from SCM");
    }

    @Test
    @DisplayName("Should have Setup & Check stage with proper git operations")
    void testSetupAndCheckStage() {
        assertTrue(jenkinsfileContent.contains("stage('Setup & Check')"),
                "Must have Setup & Check stage");
        assertTrue(jenkinsfileContent.contains("git rev-parse --short HEAD"),
                "Must get short commit hash");
        assertTrue(jenkinsfileContent.contains("env.GIT_COMMIT_HASH"),
                "Must set GIT_COMMIT_HASH environment variable");
        assertTrue(jenkinsfileContent.contains("env.IMAGE_TAG"),
                "Must set IMAGE_TAG environment variable");
        assertTrue(jenkinsfileContent.contains("currentBuild.number"),
                "Must use build number in image tag");
        assertTrue(jenkinsfileContent.contains("env.TARGET_BRANCH"),
                "Must determine target branch");
        assertTrue(jenkinsfileContent.contains("replace('origin/', '')"),
                "Must strip origin/ prefix from branch name");
    }

    @Test
    @DisplayName("Should have Build & Push stage with Gradle and Docker")
    void testBuildAndPushStage() {
        assertTrue(jenkinsfileContent.contains("stage('Build & Push')"),
                "Must have Build & Push stage");

        // Gradle build
        assertTrue(jenkinsfileContent.contains("chmod +x freebridge/gradlew"),
                "Must make gradlew executable");
        assertTrue(jenkinsfileContent.contains("./gradlew clean build"),
                "Must run Gradle clean build");
        assertTrue(jenkinsfileContent.contains("-x test"),
                "Must skip tests during build for speed");

        // Docker operations
        assertTrue(jenkinsfileContent.contains("withCredentials([usernamePassword"),
                "Must use credentials for Docker");
        assertTrue(jenkinsfileContent.contains("docker build"),
                "Must build Docker image");
        assertTrue(jenkinsfileContent.contains("docker login"),
                "Must login to Docker registry");
        assertTrue(jenkinsfileContent.contains("docker push"),
                "Must push Docker image");
        assertTrue(jenkinsfileContent.contains("docker tag"),
                "Must tag image as latest");

        // Verify both tags are pushed
        long pushCount = jenkinsfileLines.stream()
                .filter(line -> line.contains("docker push"))
                .count();
        assertEquals(2, pushCount, "Must push both versioned and latest tags");
    }

    @Test
    @DisplayName("Should have Update Manifest Repo stage with proper operations")
    void testUpdateManifestRepoStage() {
        assertTrue(jenkinsfileContent.contains("stage('Update Manifest Repo')"),
                "Must have Update Manifest Repo stage");

        // SSH agent
        assertTrue(jenkinsfileContent.contains("sshagent(credentials:"),
                "Must use SSH agent for Git operations");
        assertTrue(jenkinsfileContent.contains("ssh-keyscan github.com"),
                "Must add GitHub to known hosts");

        // Git operations
        assertTrue(jenkinsfileContent.contains("git clone"),
                "Must clone manifest repository");
        assertTrue(jenkinsfileContent.contains("git config user.name"),
                "Must configure Git user name");
        assertTrue(jenkinsfileContent.contains("git config user.email"),
                "Must configure Git user email");
        assertTrue(jenkinsfileContent.contains("Jenkins Backend Bot"),
                "Must use descriptive bot name for commits");

        // Manifest file validation
        assertTrue(jenkinsfileContent.contains("if [ ! -f kube-folder/backend-deployment.yml ]"),
                "Must check if manifest file exists");
        assertTrue(jenkinsfileContent.contains("exit 1"),
                "Must fail if manifest file not found");

        // Image tag update
        assertTrue(jenkinsfileContent.contains("sed -i"),
                "Must use sed to update image tag");
        assertTrue(jenkinsfileContent.contains("backend-deployment.yml"),
                "Must update backend deployment manifest");

        // Commit and push
        assertTrue(jenkinsfileContent.contains("git add ."),
                "Must stage changes");
        assertTrue(jenkinsfileContent.contains("git diff --cached --quiet"),
                "Must check for staged changes");
        assertTrue(jenkinsfileContent.contains("git commit -m"),
                "Must commit changes");
        assertTrue(jenkinsfileContent.contains("[Jenkins] Update backend image"),
                "Must use descriptive commit message");
        assertTrue(jenkinsfileContent.contains("git push origin main"),
                "Must push to main branch");
    }

    @Test
    @DisplayName("Should have Deploy to Server stage with kubectl")
    void testDeployToServerStage() {
        assertTrue(jenkinsfileContent.contains("stage('Deploy to Server Eric pc')"),
                "Must have Deploy to Server stage");

        // Kubeconfig
        assertTrue(jenkinsfileContent.contains("withCredentials([file(credentialsId: 'k8s-kubeconfig'"),
                "Must use kubeconfig file credential");
        assertTrue(jenkinsfileContent.contains("export KUBECONFIG=$KUBECONFIG"),
                "Must export KUBECONFIG environment variable");
        assertTrue(jenkinsfileContent.contains("chmod 600 $KUBECONFIG"),
                "Must set secure permissions on kubeconfig");

        // kubectl installation
        assertTrue(jenkinsfileContent.contains("if ! command -v kubectl"),
                "Must check if kubectl is installed");
        assertTrue(jenkinsfileContent.contains("curl -LO \"https://dl.k8s.io/release/"),
                "Must download kubectl if not installed");
        assertTrue(jenkinsfileContent.contains("chmod +x kubectl"),
                "Must make kubectl executable");

        // Deployment operations
        assertTrue(jenkinsfileContent.contains("kubectl cluster-info"),
                "Must verify cluster connectivity");
        assertTrue(jenkinsfileContent.contains("kubectl apply -f kube-folder/backend-deployment.yml"),
                "Must apply backend deployment manifest");
        assertTrue(jenkinsfileContent.contains("kubectl apply -f kube-folder/backend-service.yml"),
                "Must apply backend service manifest");
        assertTrue(jenkinsfileContent.contains("kubectl rollout restart deployment/backend"),
                "Must restart deployment to pull new image");

        // Directory validation
        assertTrue(jenkinsfileContent.contains("if [ ! -d manifest-repo ]"),
                "Must check if manifest-repo directory exists");
    }

    @Test
    @DisplayName("Should have proper post-build cleanup actions")
    void testPostBuildActions() {
        assertTrue(jenkinsfileContent.contains("post {"), "Must have post-build block");
        assertTrue(jenkinsfileContent.contains("always {"), "Must have always block for cleanup");

        // Docker cleanup
        assertTrue(jenkinsfileContent.contains("docker logout || true"),
                "Must logout from Docker registry");
        assertTrue(jenkinsfileContent.contains("docker rmi") &&
                   jenkinsfileContent.contains("|| true"),
                "Must remove Docker images with error suppression");
        assertTrue(jenkinsfileContent.contains("docker image prune -f"),
                "Must prune dangling Docker images");

        // Workspace cleanup
        long cleanWsCount = jenkinsfileLines.stream()
                .filter(line -> line.contains("cleanWs()"))
                .count();
        assertEquals(2, cleanWsCount, "Must clean workspace twice (before checkout and in post)");
    }

    @Test
    @DisplayName("Should have Discord notification on success")
    void testSuccessNotification() {
        assertTrue(jenkinsfileContent.contains("success {"), "Must have success block");
        assertTrue(jenkinsfileContent.contains("withCredentials([string(credentialsId: 'discord'"),
                "Must use Discord webhook credential");
        assertTrue(jenkinsfileContent.contains("discordSend("), "Must send Discord notification");
        assertTrue(jenkinsfileContent.contains("백엔드 배포 성공"), "Must have success message in Korean");
        assertTrue(jenkinsfileContent.contains(":tada:"), "Must have celebration emoji");
        assertTrue(jenkinsfileContent.contains("result: 'SUCCESS'"),
                "Must set SUCCESS result");
        assertTrue(jenkinsfileContent.contains("${env.IMAGE_TAG}"),
                "Must include image tag in notification");
    }

    @Test
    @DisplayName("Should have Discord notification on failure")
    void testFailureNotification() {
        assertTrue(jenkinsfileContent.contains("failure {"), "Must have failure block");
        assertTrue(jenkinsfileContent.contains("withCredentials([string(credentialsId: 'discord'"),
                "Must use Discord webhook credential");
        assertTrue(jenkinsfileContent.contains("discordSend("), "Must send Discord notification");
        assertTrue(jenkinsfileContent.contains("백엔드 배포 실패"), "Must have failure message in Korean");
        assertTrue(jenkinsfileContent.contains(":x:"), "Must have error emoji");
        assertTrue(jenkinsfileContent.contains("result: 'FAILURE'"),
                "Must set FAILURE result");
        assertTrue(jenkinsfileContent.contains("Check Console Output"),
                "Must prompt to check console output");
    }

    @Test
    @DisplayName("Should use secure credential management throughout")
    void testCredentialSecurity() {
        // Verify all credentials are properly referenced
        assertTrue(jenkinsfileContent.contains("credentials("),
                "Must use credentials function");
        assertTrue(jenkinsfileContent.contains("withCredentials(["),
                "Must use withCredentials block for sensitive operations");

        // No hardcoded secrets
        assertFalse(jenkinsfileContent.matches("(?i).*password\\s*=\\s*['\"](?!\\$).*"),
                "Must not contain hardcoded passwords");
        assertFalse(jenkinsfileContent.matches("(?i).*token\\s*=\\s*['\"](?!\\$).*"),
                "Must not contain hardcoded tokens");

        // Verify SSH key usage
        assertTrue(jenkinsfileContent.contains("sshagent"),
                "Must use sshagent for SSH operations");
    }

    @Test
    @DisplayName("Should have proper error handling and validation")
    void testErrorHandling() {
        // Exit codes for critical failures
        assertTrue(jenkinsfileContent.contains("exit 1"),
                "Must exit with error code on critical failures");

        // Error suppression where appropriate
        assertTrue(jenkinsfileContent.contains("|| true"),
                "Must suppress errors for cleanup operations");

        // File existence checks
        assertTrue(jenkinsfileContent.contains("if [ ! -f") ||
                   jenkinsfileContent.contains("if [ ! -d"),
                "Must validate file/directory existence");

        // Echo statements for debugging
        long echoCount = jenkinsfileLines.stream()
                .filter(line -> line.contains("echo \""))
                .count();
        assertTrue(echoCount >= 5,
                "Must have sufficient logging statements (found: " + echoCount + ")");
    }

    @Test
    @DisplayName("Should have proper stage dependencies and ordering")
    void testStageOrdering() {
        int checkoutIndex = findStageIndex("Checkout Code");
        int setupIndex = findStageIndex("Setup & Check");
        int buildIndex = findStageIndex("Build & Push");
        int manifestIndex = findStageIndex("Update Manifest Repo");
        int deployIndex = findStageIndex("Deploy to Server Eric pc");

        assertTrue(checkoutIndex < setupIndex,
                "Checkout must come before Setup");
        assertTrue(setupIndex < buildIndex,
                "Setup must come before Build");
        assertTrue(buildIndex < manifestIndex,
                "Build must come before Manifest Update");
        assertTrue(manifestIndex < deployIndex,
                "Manifest Update must come before Deploy");
    }

    @Test
    @DisplayName("Should use script blocks for complex operations")
    void testScriptBlocks() {
        long scriptBlockCount = jenkinsfileLines.stream()
                .filter(line -> line.trim().equals("script {"))
                .count();
        assertTrue(scriptBlockCount >= 3,
                "Must use script blocks for complex Groovy operations (found: " + scriptBlockCount + ")");
    }

    @Test
    @DisplayName("Should handle workspace cleanup properly")
    void testWorkspaceManagement() {
        // Initial cleanup
        assertTrue(jenkinsfileContent.contains("cleanWs()"),
                "Must clean workspace");

        // Manifest repo cleanup
        assertTrue(jenkinsfileContent.contains("rm -rf manifest-repo"),
                "Must remove old manifest repo directory");

        // Final cleanup in post
        assertTrue(jenkinsfileContent.contains("always {") &&
                   jenkinsfileContent.substring(jenkinsfileContent.indexOf("always {"))
                           .contains("cleanWs()"),
                "Must clean workspace in post-always block");
    }

    @Test
    @DisplayName("Should use proper shell command syntax")
    void testShellCommandSyntax() {
        // No shell injection vulnerabilities
        assertFalse(jenkinsfileContent.matches(".*sh\\s*['\"].*\\$\\{[^}]*\\}.*&&.*['\"]"),
                "Shell commands with variables should be properly escaped");

        // Use of triple quotes for multiline
        assertTrue(jenkinsfileContent.contains("sh '''") ||
                   jenkinsfileContent.contains("sh \"\"\""),
                "Must use triple quotes for multiline shell scripts");
    }

    @Test
    @DisplayName("Should validate manifest repository structure")
    void testManifestRepoValidation() {
        assertTrue(jenkinsfileContent.contains("kube-folder/backend-deployment.yml"),
                "Must reference correct deployment manifest path");
        assertTrue(jenkinsfileContent.contains("kube-folder/backend-service.yml"),
                "Must reference correct service manifest path");
        assertTrue(jenkinsfileContent.contains("ls -R"),
                "Must list directory structure on error for debugging");
    }

    @Test
    @DisplayName("Should use environment variable substitution correctly")
    void testEnvironmentVariableUsage() {
        assertTrue(jenkinsfileContent.contains("${env.IMAGE_TAG}"),
                "Must use IMAGE_TAG variable");
        assertTrue(jenkinsfileContent.contains("${env.IMAGE_NAME}"),
                "Must use IMAGE_NAME variable");
        assertTrue(jenkinsfileContent.contains("${env.MANIFEST_REPO_URL}"),
                "Must use MANIFEST_REPO_URL variable");
        assertTrue(jenkinsfileContent.contains("${env.GIT_EMAIL}"),
                "Must use GIT_EMAIL variable");
        assertTrue(jenkinsfileContent.contains("${env.CRED_ID_MANIFEST}"),
                "Must use CRED_ID_MANIFEST variable");
    }

    @Test
    @DisplayName("Should have kubectl version pinned for reproducibility")
    void testKubectlVersionPinning() {
        assertTrue(jenkinsfileContent.contains("v1.31.0"),
                "kubectl version should be pinned to v1.31.0");
        assertTrue(jenkinsfileContent.contains("/bin/linux/amd64/kubectl"),
                "kubectl must be Linux AMD64 binary");
    }

    @Test
    @DisplayName("Should verify sed command for image update")
    void testSedImageUpdate() {
        assertTrue(jenkinsfileContent.contains("sed -i"),
                "Must use sed in-place editing");
        assertTrue(jenkinsfileContent.contains("image:"),
                "Must update image field");

        // Verify the update shows the result
        assertTrue(jenkinsfileContent.contains("cat kube-folder/backend-deployment.yml | grep \"image:\""),
                "Must verify the image update");
    }

    @Test
    @DisplayName("Should handle Docker login with proper security")
    void testDockerLoginSecurity() {
        assertTrue(jenkinsfileContent.contains("echo $DOCKER_PASS | docker login"),
                "Must use stdin for Docker password");
        assertTrue(jenkinsfileContent.contains("--password-stdin"),
                "Must use --password-stdin flag");
        assertTrue(jenkinsfileContent.contains("-u $DOCKER_USER"),
                "Must specify Docker username");
    }

    @Test
    @DisplayName("Should have proper Git configuration in manifest update")
    void testGitConfiguration() {
        String manifestStageContent = extractStageContent("Update Manifest Repo");

        assertTrue(manifestStageContent.contains("git config user.name"),
                "Must configure Git username");
        assertTrue(manifestStageContent.contains("git config user.email"),
                "Must configure Git email");

        // Verify configuration happens before commit
        int configIndex = manifestStageContent.indexOf("git config");
        int commitIndex = manifestStageContent.indexOf("git commit");
        assertTrue(configIndex < commitIndex,
                "Git configuration must happen before commit");
    }

    @Test
    @DisplayName("Should validate all stages have steps block")
    void testStagesHaveSteps() {
        String[] stages = {
            "Checkout Code",
            "Setup & Check",
            "Build & Push",
            "Update Manifest Repo",
            "Deploy to Server Eric pc"
        };

        for (String stage : stages) {
            String stageContent = extractStageContent(stage);
            assertTrue(stageContent.contains("steps {"),
                    "Stage '" + stage + "' must have steps block");
        }
    }

    @Test
    @DisplayName("Edge case: Should handle missing manifest file gracefully")
    void testMissingManifestFileHandling() {
        String manifestStage = extractStageContent("Update Manifest Repo");
        assertTrue(manifestStage.contains("if [ ! -f kube-folder/backend-deployment.yml ]"),
                "Must check for manifest file existence");
        assertTrue(manifestStage.contains("Error: kube-folder/backend-deployment.yml not found"),
                "Must provide clear error message");
        assertTrue(manifestStage.contains("ls -R"),
                "Must show directory structure on error");
    }

    @Test
    @DisplayName("Edge case: Should handle missing manifest-repo directory in deploy stage")
    void testMissingManifestRepoHandling() {
        String deployStage = extractStageContent("Deploy to Server Eric pc");
        assertTrue(deployStage.contains("if [ ! -d manifest-repo ]"),
                "Must check for manifest-repo directory");
        assertTrue(deployStage.contains("Error: manifest-repo directory not found"),
                "Must provide clear error message");
        assertTrue(deployStage.contains("Was the previous stage successful?"),
                "Must hint at dependency on previous stage");
    }

    @Test
    @DisplayName("Edge case: Should handle no changes to push scenario")
    void testNoChangesToPush() {
        String manifestStage = extractStageContent("Update Manifest Repo");
        assertTrue(manifestStage.contains("if ! git diff --cached --quiet"),
                "Must check for staged changes");
        assertTrue(manifestStage.contains("No changes to push"),
                "Must handle no-changes case gracefully");
    }

    @Test
    @DisplayName("Regression test: Ensure file encoding is set for Java")
    void testJavaFileEncoding() {
        assertTrue(jenkinsfileContent.contains("JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'"),
                "Java file encoding must be UTF-8 to prevent encoding issues");
    }

    @Test
    @DisplayName("Boundary test: Verify all credential IDs are non-empty strings")
    void testCredentialIdsNotEmpty() {
        assertFalse(jenkinsfileContent.contains("credentialsId: ''"),
                "Credential IDs must not be empty");
        assertFalse(jenkinsfileContent.contains("credentialsId: \"\""),
                "Credential IDs must not be empty");
    }

    // Helper methods

    private int findStageIndex(String stageName) {
        for (int i = 0; i < jenkinsfileLines.size(); i++) {
            if (jenkinsfileLines.get(i).contains("stage('" + stageName + "')")) {
                return i;
            }
        }
        return -1;
    }

    private String extractStageContent(String stageName) {
        int startIndex = jenkinsfileContent.indexOf("stage('" + stageName + "')");
        if (startIndex == -1) {
            return "";
        }

        int braceCount = 0;
        int currentIndex = jenkinsfileContent.indexOf("{", startIndex);
        int endIndex = currentIndex;

        for (int i = currentIndex; i < jenkinsfileContent.length(); i++) {
            char c = jenkinsfileContent.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            if (braceCount == 0) {
                endIndex = i;
                break;
            }
        }

        return jenkinsfileContent.substring(startIndex, endIndex + 1);
    }
}