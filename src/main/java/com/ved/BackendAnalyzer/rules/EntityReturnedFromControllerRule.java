package com.ved.BackendAnalyzer.rules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.ved.BackendAnalyzer.model.ClassInfo;
import com.ved.BackendAnalyzer.model.Issue;

public class EntityReturnedFromControllerRule {

    public List<Issue> check(List<java.nio.file.Path> javaFiles, List<ClassInfo> classes) {
        Set<String> entities = classes.stream()
                .filter(c -> c.getType() == ClassInfo.Type.ENTITY)
                .map(c -> c.getClassName())
                .collect(Collectors.toSet());

        if (entities.isEmpty()) {
            return List.of();
        }

        JavaParser parser = new JavaParser();
        List<Issue> issues = new java.util.ArrayList<>();

        for (java.nio.file.Path path : javaFiles) {
            try {
                CompilationUnit cu = parser.parse(path).getResult().orElse(null);
                if (cu == null) {
                    continue;
                }

                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (!isController(cls)) {
                        continue;
                    }

                    for (MethodDeclaration method : cls.getMethods()) {
                        String entityType = extractEntityType(method.getType(), entities);
                        if (entityType != null) {
                            int lineNumber = method.getBegin().map(position -> position.line).orElse(-1);
                            issues.add(new Issue(
                                    "ENTITY_EXPOSED_FROM_CONTROLLER",
                                    Issue.Severity.MEDIUM,
                                    "Controller method returns entity type '" + entityType + "'. Prefer a DTO or response model.",
                                    cls.getNameAsString() + "#" + method.getNameAsString() + " (line " + lineNumber + ")"
                            ));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return issues;
    }

    private boolean isController(ClassOrInterfaceDeclaration cls) {
        return cls.isAnnotationPresent("RestController") || cls.isAnnotationPresent("Controller");
    }

    private String extractEntityType(Type type, Set<String> entityNames) {
        if (type.isVoidType()) {
            return null;
        }

        if (type instanceof ArrayType arrayType) {
            return extractEntityType(arrayType.getComponentType(), entityNames);
        }

        if (type instanceof ClassOrInterfaceType classType) {
            String simpleName = classType.getNameAsString();
            if (entityNames.contains(simpleName)) {
                return simpleName;
            }

            if (classType.getTypeArguments().isPresent()) {
                for (Type typeArgument : classType.getTypeArguments().get()) {
                    String nestedMatch = extractEntityType(typeArgument, entityNames);
                    if (nestedMatch != null) {
                        return nestedMatch;
                    }
                }
            }
        }

        return null;
    }
}
