package com.ved.BackendAnalyzer.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ved.BackendAnalyzer.graph.DependencyGraph;
import com.ved.BackendAnalyzer.graph.GraphEdge;
import com.ved.BackendAnalyzer.model.Issue;

public class ControllerRepositoryRule {

    public List<Issue> check(DependencyGraph graph) {
        Set<String> seen = graph.getEdges().stream()
                .filter(GraphEdge::isViolation)
                .map(edge -> edge.getSource().getFullyQualifiedName() + "->" + edge.getTarget().getFullyQualifiedName())
                .collect(Collectors.toSet());

        List<Issue> issues = new ArrayList<>();
        for (String key : seen) {
            String[] parts = key.split("->", 2);
            String source = parts[0];
            String target = parts[1];

            issues.add(new Issue(
                    "CONTROLLER_REPOSITORY_ACCESS",
                    Issue.Severity.HIGH,
                    "Controller depends directly on Repository. Route calls through a Service layer instead.",
                    source + " -> " + target
            ));
        }
        return issues;
    }
}
