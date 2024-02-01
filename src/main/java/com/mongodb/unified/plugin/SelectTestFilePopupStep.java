package com.mongodb.unified.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SelectTestFilePopupStep extends BaseListPopupStep<PsiJavaFile> {

    private final Project project;
    private final String testDescription;

    public SelectTestFilePopupStep(List<PsiJavaFile> psiJavaFiles, Project project, String testDescription) {
        super("Select File to Run a Test With", psiJavaFiles);
        this.project = project;
        this.testDescription = testDescription;
    }

    @Override
    public PopupStep onChosen(PsiJavaFile selectedFile, boolean finalChoice) {
        runTestClass(project, selectedFile, testDescription);
        return FINAL_CHOICE;
    }

    @NotNull
    @Override
    public String getTextFor(PsiJavaFile javaFile) {
        return javaFile.getClasses()[0].getQualifiedName();
    }

    public static void runTestClass(Project project, PsiJavaFile psiJavaTestFile, String testDescription) {
        JunitModuleTestRunner.runViaJunit(project, psiJavaTestFile, testDescription);
    }
}
