package com.ved.BackendAnalyzer.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ved.BackendAnalyzer.graph.DependencyGraph;
import com.ved.BackendAnalyzer.graph.GraphResponse;
import com.ved.BackendAnalyzer.service.AnalyzerService;

@RestController
public class AnalyzeController {
    
    private final AnalyzerService analyzerService;

    @PostMapping("/analyze")
    public String analyze() {
        System.out.println("Analyze triggered");
        analyzerService.analyzeRepo();   
        return "analysis started";
    }

    @PostMapping("/graph")
    public GraphResponse buildGraph() {
        DependencyGraph graph = analyzerService.buildDependencyGraph();
        
        // 1. Detect Cycles
        com.ved.BackendAnalyzer.graph.analysis.CycleDetector detector = new com.ved.BackendAnalyzer.graph.analysis.CycleDetector();
        java.util.List<java.util.List<String>> cycles = detector.detectCycles(graph);
        
        // 2. Compute Metrics & Score
        com.ved.BackendAnalyzer.graph.analysis.GraphMetrics metricsEngine = new com.ved.BackendAnalyzer.graph.analysis.GraphMetrics();
        
        // Approximate TotalFiles by scanning the internal node count
        int internalCount = (int) graph.getNodes().stream()
                .filter(n -> n.getLayer() != com.ved.BackendAnalyzer.model.ClassInfo.Type.EXTERNAL)
                .count();
        
        // Count base architectural violations
        long violations = graph.getEdges().stream().filter(e -> e.isViolation()).count();
        
        com.ved.BackendAnalyzer.graph.analysis.GraphMetrics.MetricsResult analysis = 
                metricsEngine.calculate(graph, cycles, internalCount, violations);

        System.out.println("----- ANALYSIS RESULT -----");
        System.out.println("Nodes: " + analysis.nodes);
        System.out.println("Edges: " + analysis.edges);
        System.out.println("Cycles: " + analysis.cycles);
        
        if (!cycles.isEmpty()) {
            System.out.println("Detected Cycles:");
            for (java.util.List<String> scc : cycles) {
                // Get simple names for better readability
                java.util.List<String> names = scc.stream()
                        .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                        .collect(java.util.stream.Collectors.toList());
                
                String path = String.join(" -> ", names);
                if (!names.isEmpty()) {
                    path += " -> " + names.get(0); // Close the loop
                }
                System.out.println("  [Cycle] " + path);
            }
        }
        System.out.println("Coupling: " + String.format("%.2f", analysis.coupling));
        System.out.println("Density: " + String.format("%.2f", analysis.density));
        System.out.println("Score: " + String.format("%.2f", analysis.qualityScore));
        
        return new GraphResponse(graph, cycles, analysis);
    }

    public AnalyzeController(AnalyzerService analyzerService) {
    this.analyzerService = analyzerService;
    }
}
