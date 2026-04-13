package com.ved.BackendAnalyzer.graph;

import java.util.Objects;

public class GraphEdge {

    public enum DependencyType {
        METHOD_CALL,
        FIELD_INJECTION,
        CONSTRUCTOR_INJECTION,
        INHERITANCE,
        INTERFACE_IMPLEMENTATION,
        RETURN_TYPE,
        PARAMETER_TYPE
    }

    private final GraphNode source;
    private final GraphNode target;
    private final DependencyType type;
    private final boolean isViolation;
    private boolean isCycleEdge = false;

    public GraphEdge(GraphNode source, GraphNode target, DependencyType type) {
        this(source, target, type, false);
    }

    public GraphEdge(GraphNode source, GraphNode target, DependencyType type, boolean isViolation) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.isViolation = isViolation;
    }

    public boolean isCycleEdge() {
        return isCycleEdge;
    }

    public void setCycleEdge(boolean cycleEdge) {
        isCycleEdge = cycleEdge;
    }

    public GraphNode getSource() {
        return source;
    }

    public GraphNode getTarget() {
        return target;
    }

    public DependencyType getType() {
        return type;
    }

    public boolean isViolation() {
        return isViolation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(source, graphEdge.source) &&
               Objects.equals(target, graphEdge.target) &&
               type == graphEdge.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, type);
    }
}
