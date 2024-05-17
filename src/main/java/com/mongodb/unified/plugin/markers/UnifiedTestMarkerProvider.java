package com.mongodb.unified.plugin.markers;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.json.psi.impl.JsonObjectImpl;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.awt.RelativePoint;
import com.mongodb.unified.plugin.SelectTestFilePopupStep;
import com.mongodb.unified.plugin.TestContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mongodb.unified.plugin.TestFileLocator.searchForTestRunners;

public class UnifiedTestMarkerProvider implements LineMarkerProvider {

    private static final Key<TestContext> TEST_CONTEXT = new Key<>("UnifiedTestType");
    private static final String PROPERTY_DESCRIPTION = "description";

    @Override
    public void collectSlowLineMarkers(@NotNull final List<? extends PsiElement> elements,
                                       @NotNull final Collection<? super LineMarkerInfo<?>> result) {
        LineMarkerProvider.super.collectSlowLineMarkers(elements, result);
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return createMarker(element);
    }

    private LineMarkerInfo createMarker(PsiElement element) {
        if (element.getLanguage().getID().equals("JSON")) {

            if (!isUnifiedTestFile(element)) {
                return null;
            }
            //TODO return leaf element instead of jsonObject for performance reasons.
//            if(element instanceof LeafPsiElement){
//                LeafPsiElement leafPsiElement = (LeafPsiElement) element;
//                if(leafPsiElement.getText().contains("description")){
//                    System.err.println();
//                }
//            }
            String fileDescription = getFileDescription(element);
            if (isArrayOfTests(element)) {
                JsonArray array = (JsonArray) element.getChildren()[1];
                List<TestContext.TestDescription> descriptions = getTestDescriptions(array, fileDescription);
                element.putUserData(TEST_CONTEXT, new TestContext(descriptions));
                cacheTestRunners(element);
               return createMarker(element, AllIcons.RunConfigurations.TestState.Run_run);
            }
            if (isSingleTest(element)) {
                TestContext.TestDescription testDescription = getTestDescription((JsonObjectImpl) element, fileDescription);
                TestContext textContext = new TestContext(testDescription);
                element.putUserData(TEST_CONTEXT, textContext);
                cacheTestRunners(element);
                return createMarker(element, AllIcons.RunConfigurations.TestState.Run);
            }
        }
        return null;
    }

    private static TestContext.@NotNull TestDescription getTestDescription(final JsonObjectImpl element, final String fileDescription) {
        String testDescription = ((JsonStringLiteral) element.findProperty(PROPERTY_DESCRIPTION).getValue()).getValue().replace("\"", "");
        return new TestContext.TestDescription(testDescription, fileDescription);
    }

    private static @NotNull List<TestContext.TestDescription> getTestDescriptions(final JsonArray array, final String fileDescription) {
        List<TestContext.TestDescription> descriptions = new ArrayList<>();
        for (JsonValue testDefinition : array.getValueList()) {
            JsonProperty description = ((JsonObjectImpl) testDefinition).findProperty(PROPERTY_DESCRIPTION);
            String testDescriptionName = description.getValue().getText().replace("\"", "");;
            descriptions.add(new TestContext.TestDescription(testDescriptionName, fileDescription));
        }
        return descriptions;
    }

    private boolean isSingleTest(final PsiElement element) {
        if (element instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) element;
            JsonArray arrayOfTests = (JsonArray) element.getParent();
            JsonProperty testProperty = (JsonProperty) arrayOfTests.getParent();
            if ("tests".equals(testProperty.getName())) {
                PsiElement[] children = jsonObject.getChildren();
                for (PsiElement child : children) {
                    if (child instanceof JsonProperty) {
                        JsonProperty jsonProperty = (JsonProperty) child;
                        if (jsonProperty.getText().contains(PROPERTY_DESCRIPTION)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isArrayOfTests(final PsiElement element) {
        if (element instanceof JsonProperty) {
            JsonProperty testProperty = (JsonProperty) element;
            if ("tests".equals(testProperty.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnifiedTestFile(final PsiElement element) {
        //TODO check if this file is located in resource folder and not in gradle out folder.
        return containsSchemaVersionKey(element);
    }


    public static boolean containsSchemaVersionKey(PsiElement element) {
        // Find the root JsonObject. If element is already a JsonFile, no need to navigate up.
        JsonObject rootObject = getRootObject(element);

        if (rootObject == null) {
            return false; // No JSON object found
        }

        // Iterate through the properties of the root object
        for (JsonProperty property : rootObject.getPropertyList()) {
            if ("schemaVersion".equals(property.getName())) {
                return true; // Found the "schemaVersion" key
            }
        }

        return false; // "schemaVersion" key not found
    }

    public static String getFileDescription(PsiElement element) {
        // Find the root JsonObject. If element is already a JsonFile, no need to navigate up.
        JsonObject rootObject = getRootObject(element);

        if (rootObject == null) {
            throw new IllegalStateException("No root object found");
        }

        // Iterate through the properties of the root object
        for (JsonProperty property : rootObject.getPropertyList()) {
            if (PROPERTY_DESCRIPTION.equals(property.getName())) {
                return property.getValue().getText().replaceAll("\"", "");
            }
        }
        throw new IllegalStateException("No file description object found");
    }

    @Nullable
    private static JsonObject getRootObject(final PsiElement element) {
        JsonObject rootObject = element instanceof JsonFile ?
                ((JsonFile) element).getTopLevelValue() instanceof JsonObject ?
                        (JsonObject) ((JsonFile) element).getTopLevelValue() : null
                : PsiTreeUtil.getParentOfType(element, JsonObject.class);
        return rootObject;
    }

    private LineMarkerInfo createMarker(PsiElement element, Icon icon) {
        // Load an icon for the gutter
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
        TestContext testContext = element.getUserData(TEST_CONTEXT);
        PsiFile containingFile = element.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        VirtualFile parent = virtualFile.getParent();
        if (parent.isDirectory()) {
            List<PsiJavaFile> psiJavaFiles = searchForTestRunners(element.getProject(), virtualFile);
            SelectTestFilePopupStep selectTestFilePopupStep =
                    new SelectTestFilePopupStep(psiJavaFiles, element.getProject(), testContext);

            ListPopup popup = JBPopupFactory.getInstance().createListPopup(selectTestFilePopupStep);
            popup.show(new RelativePoint(e));
        }
    }

    private static void cacheTestRunners(final PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        VirtualFile parent = virtualFile.getParent();
        if (parent.isDirectory()) {
            searchForTestRunners(element.getProject(), virtualFile);
        }
    }
}