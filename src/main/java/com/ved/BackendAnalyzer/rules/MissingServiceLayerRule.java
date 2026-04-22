package com.ved.BackendAnalyzer.rules;

import java.util.List;

import com.ved.BackendAnalyzer.model.ClassInfo;
import com.ved.BackendAnalyzer.model.Issue;

public class MissingServiceLayerRule {

    public List<Issue> check(List<ClassInfo> classes) {
        boolean hasController = classes.stream()
                .anyMatch(c -> c.getType() == ClassInfo.Type.CONTROLLER);

        boolean hasService = classes.stream()
                .anyMatch(c -> c.getType() == ClassInfo.Type.SERVICE);

        if (hasController && !hasService) {
            return List.of(new Issue(
                    "MISSING_SERVICE_LAYER",
                    Issue.Severity.HIGH,
                    "Controllers exist but no Service layer was found. Business logic may be living in controllers.",
                    "Project architecture"
            ));
        }

        return List.of();
    }
}
