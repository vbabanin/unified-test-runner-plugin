package com.mongodb.unified.plugin;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;

import java.io.File;

public class JunitModuleTestRunner {
    private static final UnifiedJavaAgent unifiedJavaAgent = new UnifiedJavaAgent();

    public static void runViaJunit(Project project, PsiJavaFile psiJavaTestFile, String testDescription) {
        PsiClass psiTestClass = psiJavaTestFile.getClasses()[0];
        RunManager runManager = RunManager.getInstance(project);

        JUnitConfigurationType junitConfigurationType = ConfigurationTypeUtil.findConfigurationType(JUnitConfigurationType.class);
        RunnerAndConfigurationSettings settings =
                runManager.createConfiguration("Run " + psiTestClass.getName(), junitConfigurationType.getConfigurationFactories()[0]);

        File tempAgentJar = unifiedJavaAgent.createTempAgentJar();
        String agentArgs = "-javaagent:" + tempAgentJar.getPath() + "=\"" + testDescription + "\"";
        JUnitConfiguration configuration = (JUnitConfiguration) settings.getConfiguration();
        configuration.setMainClass(psiTestClass);
        configuration.setVMParameters(agentArgs);

        runManager.setTemporaryConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }
}
