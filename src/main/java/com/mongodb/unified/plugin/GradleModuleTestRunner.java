package com.mongodb.unified.plugin;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is responsible for running a test class using the Gradle run configuration.
 * However, there are complexities with attaching static java agent, because it gets attached to
 * Gradle process and not that of Junit which makes it impossible to do instrumentation.
 */
public class GradleModuleTestRunner {

    private static final UnifiedJavaAgent unifiedJavaAgent = new UnifiedJavaAgent();

    public static void runViaPlugin(Project project, PsiJavaFile psiJavaTestFile, String testDescription) {
        String moduleName = extractModuleName(psiJavaTestFile, project);
        String testClassName = psiJavaTestFile.getClasses()[0].getQualifiedName();
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(testClassName, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            // Handle class not found
            return;
        }

        List<AnAction> actions = new ArrayList<>();
        RunnerAndConfigurationSettings activeSettings = RunManager.getInstance(project)
                .findConfigurationByName(psiClass.getName());

        String gradleCommand = ":" + moduleName + ":test --tests \"" + testClassName + "\"";
        File tempAgentJar = unifiedJavaAgent.createTempAgentJar();

        String agentArgs = "-javaagent:" + tempAgentJar.getPath() + "=\"" + testDescription + "\"";

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(
                "Run " + testClassName, GradleExternalTaskConfigurationType.getInstance().getFactory()
        );
        settings.setActivateToolWindowBeforeRun(true);
        GradleRunConfiguration configuration = (GradleRunConfiguration) settings.getConfiguration();
        configuration.getSettings().setExternalProjectPath((project.getBasePath()));
        configuration.getSettings().setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
        configuration.getSettings().setTaskNames(Arrays.asList(gradleCommand.split(" ")));
        configuration.getSettings().setVmOptions(agentArgs);

        runManager.setTemporaryConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    private static String extractModuleName(PsiJavaFile psiJavaTestFile, Project project) {
        String qualifiedName = psiJavaTestFile.getClasses()[0].getQualifiedName();
        com.intellij.openapi.module.Module module = ModuleUtilCore.findModuleForFile(psiJavaTestFile.getVirtualFile(), project);
        if (module == null) {
            // Handle case where the module is not found
            throw new RuntimeException("Module not found");
        }
        String fullModuleName = module.getName();

        String[] parts = fullModuleName.split("\\.");
        return parts.length > 2 ? parts[parts.length - 2] : fullModuleName;
    }


    /*
     * Replacing method body on PSI experiment.
     */

//    public static File modifyAndSaveFile(PsiClass originalPsiClass, Project project) {
//        PsiClass copy = (PsiClass) originalPsiClass.copy();
//        PsiMethod method = findMethodByName(copy, "data");
//        if (method != null) {
//            CompletableFuture<Path> completableFuture = new CompletableFuture<>();
//            WriteCommandAction.runWriteCommandAction(project, () -> {
//                String newMethodBody = "{ return null; }";
//                replaceMethodBody(method, newMethodBody, project);
//
//                PsiFile psiFile = method.getContainingFile();
//
//                if (psiFile != null) {
//                    try {
//                        Path tempFilePath = Files.createTempFile(psiFile.getName(), ".java");
//                        try (FileWriter writer = new FileWriter(tempFilePath.toFile())) {
//                            writer.write(psiFile.getText());
//                        }
//
//                        // Now you have the modified file at tempFilePath
//                        // You can use this path to execute your Gradle task
//                        completableFuture.complete(tempFilePath);
//                    } catch (IOException e) {
//                       completableFuture.completeExceptionally(e);
//                    }
//                }
//            });
//            try {
//                return completableFuture.get().toFile();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            } catch (ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        throw new RuntimeException("No data method found");
//    }
//
//    private static void replaceMethodBody(PsiMethod method, String newBody, Project project) {
//        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
//        PsiCodeBlock newMethodBody = elementFactory.createCodeBlockFromText(newBody, null);
//        PsiCodeBlock oldBody = method.getBody();
//        if (oldBody != null) {
//            oldBody.replace(newMethodBody);
//        }
//    }
}