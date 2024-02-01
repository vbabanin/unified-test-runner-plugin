package com.mongodb.unified.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFileLocator {

    private static final Map<String, List<PsiJavaFile>> CACHED_TEST_RUNNERS = new HashMap<>();
    private static final String RESOURCES_TEST_FOLDER = "resources/";

    public static List<PsiJavaFile> searchForTestRunners(final Project project, final VirtualFile jsonTestFile) {
        String directoryName = getPotentialTestPath(jsonTestFile);

        if (CACHED_TEST_RUNNERS.containsKey(directoryName)) {
            return CACHED_TEST_RUNNERS.get(directoryName);
        }

//        Module syncModule = ModuleManager.getInstance(project).findModuleByName("driver-sync");
//        Module reactiveModule = ModuleManager.getInstance(project).findModuleByName("driver-reactive-streams");

        //TODO this code block takes significant amount of time to execute.
        // Need to optimize it with narrowing down search scope to withing a module or folder.
        Collection<VirtualFile> javaFiles = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));

        List<PsiJavaFile> psiJavaFiles = findAssociatedTestExecutors(project, javaFiles, directoryName);
        CACHED_TEST_RUNNERS.put(directoryName, psiJavaFiles);
        return psiJavaFiles;
    }

    @NotNull
    private static List<PsiJavaFile> findAssociatedTestExecutors(final Project project, final Collection<VirtualFile> javaFiles,
                                                     final String directoryName) {
        List<PsiJavaFile> psiJavaFiles = new ArrayList<>();
        for (VirtualFile file : javaFiles) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(file);
            if (psiJavaFile != null) {
                psiJavaFile.accept(new JavaRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitLiteralExpression(PsiLiteralExpression expression) {
                        super.visitLiteralExpression(expression);
                        String text = expression.getText();
                        if (text != null && text.contains(directoryName)) {
                            psiJavaFiles.add(psiJavaFile);
                        }
                    }
                });
            }
        }
        return psiJavaFiles;
    }

    private static String getPotentialTestPath(final VirtualFile virtualFile) {
        String path = virtualFile.getPath();
        int resourcesFolderStartIndex = path.indexOf(RESOURCES_TEST_FOLDER);
        int fileNameStartIndex = path.indexOf(virtualFile.getName());
        return path.substring(resourcesFolderStartIndex + RESOURCES_TEST_FOLDER.length(), fileNameStartIndex - 1);
    }
}
