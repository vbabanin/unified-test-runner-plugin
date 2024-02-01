
package com.mongodb.unified.plugin;

import com.intellij.json.psi.JsonObject;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class JsonFoldingBuilder implements FoldingBuilder {
    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        buildFoldRegions(node.getPsi(), descriptors);
        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void buildFoldRegions(PsiElement element, List<FoldingDescriptor> descriptors) {
        if (element instanceof JsonObject) {
            descriptors.add(new FoldingDescriptor(element.getNode(), element.getTextRange()));
        }
        for (PsiElement child : element.getChildren()) {
            buildFoldRegions(child, descriptors);
        }
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        // This is the text that will be displayed when the region is folded
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}