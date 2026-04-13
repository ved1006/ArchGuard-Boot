package com.ved.BackendAnalyzer.graph.analysis;

import com.ved.BackendAnalyzer.graph.DependencyGraph;

import java.util.List;

public class GraphMetrics {

    public static class MetricsResult {
        public int nodes;
        public int edges;
        public int cycles;
        public double coupling;
        public double density;
        public double qualityScore;
    }

    public MetricsResult calculate(DependencyGraph graph, List<List<String>> cycles, int totalFiles, long violationCount) {
        MetricsResult res = new MetricsResult();
        res.nodes = graph.getNodes().size();
        res.edges = graph.getEdges().size();
        res.cycles = cycles.size();

        // 🔹 Coupling: edges / nodes
        res.coupling = res.nodes > 0 ? (double) res.edges / res.nodes : 0;

        // 🔹 Refined Density: edges / (nodes * (nodes - 1))
        if (res.nodes > 1) {
            res.density = (double) res.edges / (res.nodes * (res.nodes - 1));
        } else {
            res.density = 0;
        }

        // 🔹 Hybrid Quality Score
        double baseScore = 100.0;

        // Issue Penalty (Assuming violations are "HIGH" for this simplified model)
        // (15 × HIGH) + (7.5 × MEDIUM) + (2.5 × LOW)
        double issuePenalty = violationCount * 15.0;

        // Cycle Penalty: 12 × NumberOfCycles
        double cyclePenalty = res.cycles * 12.0;

        // Coupling Penalty: 5 × max(0, coupling − 1)
        double couplingPenalty = 5.0 * Math.max(0, res.coupling - 1.0);

        // Density Penalty: 5 × density
        double densityPenalty = 5.0 * res.density;

        // Scale Factor: max(1, TotalFiles / 5)
        double scaleFactor = Math.max(1.0, (double) totalFiles / 5.0);

        double rawScore = baseScore - ((issuePenalty + cyclePenalty + couplingPenalty + densityPenalty) / scaleFactor);

        // Clamping: score = Math.max(0, Math.min(100, score))
        res.qualityScore = Math.max(0, Math.min(100, rawScore));

        return res;
    }
}
