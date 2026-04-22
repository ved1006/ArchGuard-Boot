package com.ved.BackendAnalyzer.service;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ved.BackendAnalyzer.dto.AnalysisResponse;
import com.ved.BackendAnalyzer.dto.AnalysisSummary;
import com.ved.BackendAnalyzer.git.RepoCloner;
import com.ved.BackendAnalyzer.graph.DependencyGraph;
import com.ved.BackendAnalyzer.graph.GraphBuilder;
import com.ved.BackendAnalyzer.graph.GraphEdge;
import com.ved.BackendAnalyzer.graph.GraphResponse;
import com.ved.BackendAnalyzer.graph.analysis.CycleDetector;
import com.ved.BackendAnalyzer.graph.analysis.GraphMetrics;
import com.ved.BackendAnalyzer.model.ClassInfo;
import com.ved.BackendAnalyzer.model.Issue;
import com.ved.BackendAnalyzer.model.MethodCallInfo;
import com.ved.BackendAnalyzer.rules.ControllerRepositoryRule;
import com.ved.BackendAnalyzer.rules.EntityReturnedFromControllerRule;
import com.ved.BackendAnalyzer.rules.MissingRestControllerAdviceRule;
import com.ved.BackendAnalyzer.rules.MissingServiceLayerRule;
import com.ved.BackendAnalyzer.rules.UnpaginatedFindAllRule;
import com.ved.BackendAnalyzer.scanner.AnnotationScanner;
import com.ved.BackendAnalyzer.scanner.JavaFileScanner;
import com.ved.BackendAnalyzer.scanner.MethodCallScanner;

@Service
public class AnalyzerService {

    private final RepoCloner repoCloner;
    private final String defaultRepoUrl;
    private final JavaFileScanner javaFileScanner = new JavaFileScanner();
    private final AnnotationScanner annotationScanner = new AnnotationScanner();
    private final MethodCallScanner methodCallScanner = new MethodCallScanner();
    private final ControllerRepositoryRule controllerRepositoryRule = new ControllerRepositoryRule();
    private final MissingServiceLayerRule missingServiceLayerRule = new MissingServiceLayerRule();
    private final EntityReturnedFromControllerRule entityReturnedRule = new EntityReturnedFromControllerRule();
    private final MissingRestControllerAdviceRule missingAdviceRule = new MissingRestControllerAdviceRule();
    private final UnpaginatedFindAllRule unpaginatedFindAllRule = new UnpaginatedFindAllRule();

    public AnalyzerService(RepoCloner repoCloner,
                           @Value("${archguard.default-repo-url}") String defaultRepoUrl) {
        this.repoCloner = repoCloner;
        this.defaultRepoUrl = defaultRepoUrl;
    }

    public AnalysisResponse analyzeRepo(String requestedRepoUrl) {
        String repoUrl = resolveRepoUrl(requestedRepoUrl);
        AnalysisArtifacts artifacts = prepareArtifacts(repoUrl);

        try {
            List<Issue> issues = collectIssues(artifacts);
            issues.sort(Comparator
                    .comparingInt((Issue issue) -> severityWeight(issue.getSeverity()))
                    .thenComparing(Issue::getRuleId)
                    .thenComparing(Issue::getLocation));

            GraphResponse graphResponse = buildGraphResponse(artifacts);
            AnalysisSummary summary = AnalysisSummary.from(
                    artifacts.repoUrl,
                    artifacts.javaFiles.size(),
                    artifacts.classes,
                    issues.size(),
                    graphResponse.getScore(),
                    graphResponse.getCycles().size()
            );

            return new AnalysisResponse(
                    repoUrl,
                    Instant.now().toString(),
                    summary,
                    issues,
                    graphResponse
            );
        } finally {
            repoCloner.deleteRepo(artifacts.repoDir);
        }
    }

    public GraphResponse buildDependencyGraphResponse(String requestedRepoUrl) {
        String repoUrl = resolveRepoUrl(requestedRepoUrl);
        AnalysisArtifacts artifacts = prepareArtifacts(repoUrl);

        try {
            return buildGraphResponse(artifacts);
        } finally {
            repoCloner.deleteRepo(artifacts.repoDir);
        }
    }

    private AnalysisArtifacts prepareArtifacts(String repoUrl) {
        File repoDir = repoCloner.cloneRepo(repoUrl);
        List<Path> javaFiles = javaFileScanner.scan(repoDir.toPath());
        List<ClassInfo> classes = annotationScanner.scan(javaFiles);
        List<MethodCallInfo> calls = methodCallScanner.scan(javaFiles);
        Path sourceRoot = javaFileScanner.detectSourceRoot(repoDir.toPath());
        GraphBuilder graphBuilder = new GraphBuilder(sourceRoot);
        DependencyGraph graph = graphBuilder.buildGraph(javaFiles);

        return new AnalysisArtifacts(repoUrl, repoDir, javaFiles, classes, calls, graph);
    }

    private List<Issue> collectIssues(AnalysisArtifacts artifacts) {
        List<Issue> issues = new ArrayList<>();
        issues.addAll(controllerRepositoryRule.check(artifacts.graph));
        issues.addAll(missingServiceLayerRule.check(artifacts.classes));
        issues.addAll(entityReturnedRule.check(artifacts.javaFiles, artifacts.classes));
        issues.addAll(missingAdviceRule.check(artifacts.javaFiles));
        issues.addAll(unpaginatedFindAllRule.check(artifacts.calls));
        return issues;
    }

    private GraphResponse buildGraphResponse(AnalysisArtifacts artifacts) {
        CycleDetector detector = new CycleDetector();
        List<List<String>> cycles = detector.detectCycles(artifacts.graph);

        GraphMetrics metricsEngine = new GraphMetrics();
        int internalCount = (int) artifacts.graph.getNodes().stream()
                .filter(node -> node.getLayer() != ClassInfo.Type.EXTERNAL)
                .count();
        long violations = artifacts.graph.getEdges().stream().filter(GraphEdge::isViolation).count();

        GraphMetrics.MetricsResult metrics = metricsEngine.calculate(
                artifacts.graph,
                cycles,
                Math.max(internalCount, artifacts.javaFiles.size()),
                violations
        );

        return new GraphResponse(artifacts.graph, cycles, metrics);
    }

    private String resolveRepoUrl(String requestedRepoUrl) {
        if (requestedRepoUrl == null || requestedRepoUrl.isBlank()) {
            return defaultRepoUrl;
        }
        return requestedRepoUrl.trim();
    }

    private int severityWeight(Issue.Severity severity) {
        return switch (severity) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    private record AnalysisArtifacts(
            String repoUrl,
            File repoDir,
            List<Path> javaFiles,
            List<ClassInfo> classes,
            List<MethodCallInfo> calls,
            DependencyGraph graph
    ) {
    }
}
