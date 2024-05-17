package com.mongodb.unified.plugin.activity;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.mongodb.unified.plugin.UnifiedJavaAgent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class PluginStartupActivity implements StartupActivity {

    private static final UnifiedJavaAgent unifiedJavaAgent = new UnifiedJavaAgent();

    @Override
    public void runActivity(@NotNull Project project) {
        project.getMessageBus().connect().subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
            @Override
            public void runConfigurationAdded(@NotNull final RunnerAndConfigurationSettings settings) {
                modifyVMOptions(settings.getConfiguration());
            }

            @Override
            public void runConfigurationChanged(@NotNull final RunnerAndConfigurationSettings settings, @Nullable final String existingId) {
                modifyVMOptions(settings.getConfiguration());
            }

            @Override
            public void runConfigurationChanged(@NotNull final RunnerAndConfigurationSettings settings) {
                modifyVMOptions(settings.getConfiguration());
            }
        });
    }

    private void modifyVMOptions(RunConfiguration configuration) {
        if (configuration instanceof JUnitConfiguration) {
            JUnitConfiguration myConfiguration = (JUnitConfiguration) configuration;
            String currentVmOptions = myConfiguration.getVMParameters();

            if (!currentVmOptions.contains("unified-agent")) {
                File tempAgentJar = unifiedJavaAgent.createTempAgentJar();
                String additionalOptions = "-javaagent:" + tempAgentJar.getPath();
                myConfiguration.setVMParameters(currentVmOptions + " " + additionalOptions);
                return;
            }
            myConfiguration.setVMParameters(currentVmOptions);
        }
    }
}