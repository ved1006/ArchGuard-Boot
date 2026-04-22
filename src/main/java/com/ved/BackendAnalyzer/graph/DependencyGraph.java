package com.ved.BackendAnalyzer.graph;

import java.util.*;

import com.ved.BackendAnalyzer.model.ClassInfo;

public class DependencyGraph {

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Set<GraphEdge> edges = new HashSet<>();

    public void addNode(GraphNode node) {
        nodes.putIfAbsent(node.getFullyQualifiedName(), node);
    }

    public GraphNode getOrCreateNode(String fullyQualifiedName, String simpleName, ClassInfo.Type defaultLayer) {
        if (!nodes.containsKey(fullyQualifiedName)) {
            nodes.put(fullyQualifiedName, new GraphNode(fullyQualifiedName, simpleName, defaultLayer));
        }
        return nodes.get(fullyQualifiedName);
    }

    public void addEdge(GraphNode source, GraphNode target, GraphEdge.DependencyType type) {
        addEdge(source, target, type, false);
    }

    public void addEdge(GraphNode source, GraphNode target, GraphEdge.DependencyType type, boolean isViolation) {
        if (source != null && target != null) {
            edges.add(new GraphEdge(source, target, type, isViolation));
        }
    }

    public Collection<GraphNode> getNodes() {
        return nodes.values();
    }

    public Set<GraphEdge> getEdges() {
        return edges;
    }
    
    public Map<String, GraphNode> getNodeMap() {
        return nodes;
    }
}
