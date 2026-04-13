package com.ved.BackendAnalyzer.service;

import org.springframework.stereotype.Service;
import com.ved.BackendAnalyzer.model.Issue;

import com.ved.BackendAnalyzer.rules.ControllerRepositoryRule;
import com.ved.BackendAnalyzer.rules.EntityReturnedFromControllerRule;
import com.ved.BackendAnalyzer.rules.UnpaginatedFindAllRule;

import com.ved.BackendAnalyzer.rules.MissingServiceLayerRule;
import com.ved.BackendAnalyzer.rules.MissingRestControllerAdviceRule;

import com.ved.BackendAnalyzer.git.RepoCloner;
import com.ved.BackendAnalyzer.model.MethodCallInfo;
import com.ved.BackendAnalyzer.scanner.MethodCallScanner;
import com.ved.BackendAnalyzer.graph.GraphBuilder;
import com.ved.BackendAnalyzer.graph.DependencyGraph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.ved.BackendAnalyzer.scanner.AnnotationScanner;
import com.ved.BackendAnalyzer.scanner.JavaFileScanner;

import java.io.File;

import com.ved.BackendAnalyzer.model.ClassInfo;


@Service
public class AnalyzerService {

    private final RepoCloner repoCloner;
    private final JavaFileScanner javaFileScanner = new JavaFileScanner();
    private final AnnotationScanner annotationScanner = new AnnotationScanner();
    private final MethodCallScanner methodCallScanner = new MethodCallScanner();
    private final ControllerRepositoryRule controllerRepositoryRule = new ControllerRepositoryRule();
    private final MissingServiceLayerRule missingServiceLayerRule =new MissingServiceLayerRule();
    private final EntityReturnedFromControllerRule entityReturnedRule =
        new EntityReturnedFromControllerRule();

        private final MissingRestControllerAdviceRule missingAdviceRule =
        new MissingRestControllerAdviceRule();
        private final UnpaginatedFindAllRule unpaginatedFindAllRule =new UnpaginatedFindAllRule();



    public AnalyzerService(RepoCloner repoCloner) {
        this.repoCloner = repoCloner;
    }

    public void analyzeRepo() {
        System.out.println("AnalyzerService called");

        String repoUrl = "https://github.com/ved1006/campusCore";
        File repoDir = repoCloner.cloneRepo(repoUrl);

        List<Issue> allIssues = new ArrayList<>();

        List<Path> javaFiles = javaFileScanner.scan(repoDir.toPath());
        System.out.println("Java files found: " + javaFiles.size());


        List<ClassInfo> classes = annotationScanner.scan(javaFiles);

        long controllers = classes.stream().filter(c -> c.getType() == ClassInfo.Type.CONTROLLER).count();
        long services = classes.stream().filter(c -> c.getType() == ClassInfo.Type.SERVICE).count();
        long repos = classes.stream().filter(c -> c.getType() == ClassInfo.Type.REPOSITORY).count();
        long entities = classes.stream().filter(c -> c.getType() == ClassInfo.Type.ENTITY).count();

        System.out.println("Controllers: " + controllers);
        System.out.println("Services: " + services);
        System.out.println("Repositories: " + repos);
        System.out.println("Entities: " + entities);
        List<MethodCallInfo> calls = methodCallScanner.scan(javaFiles);

        System.out.println("Method calls detected:");
        calls.forEach(c ->
            System.out.println(
                c.getCallerClass() + " -> " +
                c.getCalledObject() + "." +
                c.getMethodName()
            )
        );
        controllerRepositoryRule.check(classes, calls);
        missingServiceLayerRule.check(classes);
        entityReturnedRule.check(classes);
        missingAdviceRule.check(classes);
        allIssues.addAll(unpaginatedFindAllRule.check(calls));
        System.out.println("---- ANALYSIS REPORT ----");
for (Issue issue : allIssues) {
    System.out.println(
        "[" + issue.getSeverity() + "] " +
        issue.getRuleId() + " | " +
        issue.getLocation() + " | " +
        issue.getMessage()
    );
}


    }

    public DependencyGraph buildDependencyGraph() {
        System.out.println("GraphBuilder endpoint called");
        String repoUrl = "https://github.com/ved1006/campusCore";
        File repoDir = repoCloner.cloneRepo(repoUrl);
        List<Path> javaFiles = javaFileScanner.scan(repoDir.toPath());

        System.out.println("Building Dependency Graph...");
        Path sourceRoot = repoDir.toPath().resolve("src/main/java");
        GraphBuilder graphBuilder = new GraphBuilder(sourceRoot);
        DependencyGraph graph = graphBuilder.buildGraph(javaFiles);

        long internalNodes = graph.getNodes().stream().filter(n -> n.getLayer() != ClassInfo.Type.EXTERNAL).count();
        long externalNodes = graph.getNodes().stream().filter(n -> n.getLayer() == ClassInfo.Type.EXTERNAL).count();
        System.out.println("Dependency Graph Built -> Internal Nodes: " + internalNodes + " | External Nodes: " + externalNodes + " | Total Edges: " + graph.getEdges().size());

        return graph;
    }
}