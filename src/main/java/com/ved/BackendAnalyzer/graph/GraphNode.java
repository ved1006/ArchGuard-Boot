package com.ved.BackendAnalyzer.graph;

import java.util.Objects;
import com.ved.BackendAnalyzer.model.ClassInfo;

public class GraphNode {
    private final String fullyQualifiedName;
    private final String simpleName;
    private final ClassInfo.Type layer;
    private boolean inCycle = false;

    public GraphNode(String fullyQualifiedName, String simpleName, ClassInfo.Type layer) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.simpleName = simpleName;
        this.layer = layer;
    }

    public boolean isInCycle() {
        return inCycle;
    }

    public void setInCycle(boolean inCycle) {
        this.inCycle = inCycle;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public ClassInfo.Type getLayer() {
        return layer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(fullyQualifiedName, graphNode.fullyQualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName);
    }
}
