package com.ved.BackendAnalyzer.rules;

import java.util.List;

import com.ved.BackendAnalyzer.model.ClassInfo;

public class MissingRestControllerAdviceRule {

    public void check(List<ClassInfo> classes) {

        boolean hasAdvice = classes.stream()
                .anyMatch(c ->
                        c.getClassName().toLowerCase().contains("exception") ||
                        c.getClassName().toLowerCase().contains("advice")
                );

        if (!hasAdvice) {
            System.out.println(
                "[MEDIUM] No global exception handler (@RestControllerAdvice) found. " +
                "Unhandled exceptions may leak to clients."
            );
        }
    }
}
