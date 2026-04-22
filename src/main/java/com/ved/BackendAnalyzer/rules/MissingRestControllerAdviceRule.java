package com.ved.BackendAnalyzer.rules;

import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.ved.BackendAnalyzer.model.Issue;

public class MissingRestControllerAdviceRule {

    public List<Issue> check(List<java.nio.file.Path> javaFiles) {
        JavaParser parser = new JavaParser();

        for (java.nio.file.Path path : javaFiles) {
            try {
                CompilationUnit cu = parser.parse(path).getResult().orElse(null);
                if (cu == null) {
                    continue;
                }

                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    if (cls.isAnnotationPresent("RestControllerAdvice") || cls.isAnnotationPresent("ControllerAdvice")) {
                        return List.of();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return List.of(new Issue(
                "MISSING_REST_CONTROLLER_ADVICE",
                Issue.Severity.MEDIUM,
                "No @RestControllerAdvice or @ControllerAdvice class was found for centralized exception handling.",
                "Project architecture"
        ));
    }
}
