package com.ved.BackendAnalyzer.graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON-serializable response object for the dependency graph.
 * Structured for D3.js force-graph consumption.
 */
public class GraphResponse {

    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;
    private List<List<String>> cycles;
    private MetricsDTO metrics;
    private double score;
    private StatsDTO stats;

    public GraphResponse(DependencyGraph graph, List<List<String>> cycles, com.ved.BackendAnalyzer.graph.analysis.GraphMetrics.MetricsResult analysis) {
        // Build node list
        this.nodes = graph.getNodes().stream()
                .map(n -> new NodeDTO(
                        n.getFullyQualifiedName(),
                        n.getSimpleName(),
                        n.getLayer().name(),
                        n.isInCycle()
                ))
                .collect(Collectors.toList());

        // Build edge list using FQN as source/target identifiers
        this.edges = graph.getEdges().stream()
                .map(e -> new EdgeDTO(
                        e.getSource().getFullyQualifiedName(),
                        e.getTarget().getFullyQualifiedName(),
                        e.getType().name(),
                        e.isViolation(),
                        e.isCycleEdge()
                ))
                .collect(Collectors.toList());

        this.cycles = cycles;
        this.score = analysis.qualityScore;
        this.metrics = new MetricsDTO(
                analysis.nodes,
                analysis.edges,
                analysis.cycles,
                analysis.coupling,
                analysis.density
        );

        // Compute stats (Keep for compatibility)
        long internal = graph.getNodes().stream()
                .filter(n -> !"EXTERNAL".equals(n.getLayer().name()))
                .count();
        long external = graph.getNodes().stream()
                .filter(n -> "EXTERNAL".equals(n.getLayer().name()))
                .count();
        this.stats = new StatsDTO(internal, external, this.edges.size());
    }

    public List<NodeDTO> getNodes() { return nodes; }
    public List<EdgeDTO> getEdges() { return edges; }
    public List<List<String>> getCycles() { return cycles; }
    public MetricsDTO getMetrics() { return metrics; }
    public double getScore() { return score; }
    public StatsDTO getStats() { return stats; }

    public static class NodeDTO {
        private String id;
        private String label;
        private String layer;
        private boolean inCycle;

        public NodeDTO(String id, String label, String layer, boolean inCycle) {
            this.id = id;
            this.label = label;
            this.layer = layer;
            this.inCycle = inCycle;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getLayer() { return layer; }
        public boolean isInCycle() { return inCycle; }
    }

    public static class EdgeDTO {
        private String source;
        private String target;
        private String type;
        private boolean isViolation;
        private boolean isCycleEdge;

        public EdgeDTO(String source, String target, String type, boolean isViolation, boolean isCycleEdge) {
            this.source = source;
            this.target = target;
            this.type = type;
            this.isViolation = isViolation;
            this.isCycleEdge = isCycleEdge;
        }

        public String getSource() { return source; }
        public String getTarget() { return target; }
        public String getType() { return type; }
        public boolean isViolation() { return isViolation; }
        public boolean isCycleEdge() { return isCycleEdge; }
    }

    public static class MetricsDTO {
        private int nodes;
        private int edges;
        private int cycles;
        private double coupling;
        private double density;

        public MetricsDTO(int nodes, int edges, int cycles, double coupling, double density) {
            this.nodes = nodes;
            this.edges = edges;
            this.cycles = cycles;
            this.coupling = coupling;
            this.density = density;
        }

        public int getNodes() { return nodes; }
        public int getEdges() { return edges; }
        public int getCycles() { return cycles; }
        public double getCoupling() { return coupling; }
        public double getDensity() { return density; }
    }

    public static class StatsDTO {
        private long internalNodes;
        private long externalNodes;
        private long totalEdges;

        public StatsDTO(long internalNodes, long externalNodes, long totalEdges) {
            this.internalNodes = internalNodes;
            this.externalNodes = externalNodes;
            this.totalEdges = totalEdges;
        }

        public long getInternalNodes() { return internalNodes; }
        public long getExternalNodes() { return externalNodes; }
        public long getTotalEdges() { return totalEdges; }
    }
}
