package com.ved.BackendAnalyzer.rules;

import java.util.List;

import com.ved.BackendAnalyzer.model.ClassInfo;

public class MissingServiceLayerRule {

    public void check(List<ClassInfo> classes) {

        boolean hasController = classes.stream()
                .anyMatch(c -> c.getType() == ClassInfo.Type.CONTROLLER);

        boolean hasService = classes.stream()
                .anyMatch(c -> c.getType() == ClassInfo.Type.SERVICE);

        if (hasController && !hasService) {
            System.out.println(
                "[HIGH] Controllers detected but no Service layer found. " +
                "Business logic may be leaking into Controllers."
            );
        }
    }
}
