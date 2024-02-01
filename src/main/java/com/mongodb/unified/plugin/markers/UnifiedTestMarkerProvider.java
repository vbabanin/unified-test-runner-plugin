package com.mongodb.unified.plugin.markers;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.impl.JsonObjectImpl;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.awt.RelativePoint;
import com.mongodb.unified.plugin.SelectTestFilePopupStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.mongodb.unified.plugin.TestFileLocator.searchForTestRunners;

public class UnifiedTestMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (shouldBeMarkerAdded(element)) {
            return createMarker(element);
        }
        return null;
    }

    private boolean shouldBeMarkerAdded(PsiElement element) {
        if (!isUnifiedTestFile(element)) {
            return false;
        }
        if (element.getLanguage().getID().equals("JSON")) {
            //TODO return leaf element instead of jsonObject for performance reasons.
//            if(element instanceof LeafPsiElement){
//                LeafPsiElement leafPsiElement = (LeafPsiElement) element;
//                if(leafPsiElement.getText().contains("description")){
//                    System.err.println();
//                }
//            }
            if (element instanceof JsonObject) {
                JsonObject jsonObject = (JsonObject) element;
                JsonArray arrayOfTests = (JsonArray) element.getParent();
                JsonProperty testProperty = (JsonProperty) arrayOfTests.getParent();
                if ("tests".equals(testProperty.getName())) {
                    PsiElement[] children = jsonObject.getChildren();
                    for (PsiElement child : children) {
                        if (child instanceof JsonProperty) {
                            JsonProperty jsonProperty = (JsonProperty) child;
                            if (jsonProperty.getText().contains("description")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isUnifiedTestFile(final PsiElement element) {
        //TODO check if it is unified test. Probably by checking if schemaVersion field is present.
        // Also, check if this file is located in resource folder and not in gradle out folder.
        return true;
    }

    private LineMarkerInfo createMarker(PsiElement element) {
        // Load an icon for the gutter
        Icon icon = AllIcons.RunConfigurations.TestState.Run;

        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                icon,
                element2 -> "Click action",
                this::showListOfAvailableTests,
                GutterIconRenderer.Alignment.CENTER
        );
    }

    private void showListOfAvailableTests(final MouseEvent e, PsiElement element) {
        PsiFile containingFile = element.getContainingFile();

        String name = containingFile.getName();
        String testDescription = ((JsonStringLiteral) ((JsonObjectImpl) element).findProperty("description")
                .getValue()).getValue();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        VirtualFile parent = virtualFile.getParent();
        if (parent.isDirectory()) {

            List<PsiJavaFile> psiJavaFiles = searchForTestRunners(element.getProject(), virtualFile);
            SelectTestFilePopupStep selectTestFilePopupStep = new SelectTestFilePopupStep(psiJavaFiles, element.getProject(), testDescription);

            ListPopup popup = JBPopupFactory.getInstance().createListPopup(selectTestFilePopupStep);
            popup.show(new RelativePoint(e));
        }
    }
}