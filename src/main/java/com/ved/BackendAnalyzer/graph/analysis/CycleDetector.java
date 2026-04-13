package com.ved.BackendAnalyzer.graph.analysis;

import com.ved.BackendAnalyzer.graph.DependencyGraph;
import com.ved.BackendAnalyzer.graph.GraphEdge;
import com.ved.BackendAnalyzer.graph.GraphNode;

import java.util.*;

public class CycleDetector {

    private int timer = 0;
    private final Map<String, Integer> ids = new HashMap<>();
    private final Map<String, Integer> low = new HashMap<>();
    private final Map<String, Boolean> onStack = new HashMap<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private final List<List<String>> sccs = new ArrayList<>();
    
    // For refined edge marking
    private final Map<String, Integer> nodeToComponentId = new HashMap<>();

    public List<List<String>> detectCycles(DependencyGraph graph) {
        timer = 0;
        ids.clear();
        low.clear();
        onStack.clear();
        stack.clear();
        sccs.clear();
        nodeToComponentId.clear();

        Map<String, List<String>> adj = new HashMap<>();
        for (GraphEdge e : graph.getEdges()) {
            adj.computeIfAbsent(e.getSource().getFullyQualifiedName(), k -> new ArrayList<>())
               .add(e.getTarget().getFullyQualifiedName());
        }

        for (GraphNode node : graph.getNodes()) {
            String fqn = node.getFullyQualifiedName();
            if (!ids.containsKey(fqn)) {
                dfs(fqn, adj);
            }
        }

        List<List<String>> cycles = new ArrayList<>();
        int componentCounter = 0;
        for (List<String> scc : sccs) {
            if (scc.size() > 1) {
                cycles.add(scc);
                for (String fqn : scc) {
                    GraphNode node = graph.getNodeMap().get(fqn);
                    if (node != null) node.setInCycle(true);
                    nodeToComponentId.put(fqn, componentCounter);
                }
                componentCounter++;
            }
        }

        // Refined Edge Marking Logic: source and target must belong to SAME SCC
        for (GraphEdge edge : graph.getEdges()) {
            String u = edge.getSource().getFullyQualifiedName();
            String v = edge.getTarget().getFullyQualifiedName();
            
            Integer uComp = nodeToComponentId.get(u);
            Integer vComp = nodeToComponentId.get(v);
            
            if (uComp != null && vComp != null && uComp.equals(vComp)) {
                edge.setCycleEdge(true);
            }
        }

        return cycles;
    }

    private void dfs(String at, Map<String, List<String>> adj) {
        stack.push(at);
        onStack.put(at, true);
        ids.put(at, timer);
        low.put(at, timer);
        timer++;

        List<String> neighbors = adj.getOrDefault(at, Collections.emptyList());
        for (String to : neighbors) {
            if (!ids.containsKey(to)) {
                dfs(to, adj);
                low.put(at, Math.min(low.get(at), low.get(to)));
            } else if (onStack.getOrDefault(to, false)) {
                low.put(at, Math.min(low.get(at), ids.get(to)));
            }
        }

        if (ids.get(at).equals(low.get(at))) {
            List<String> scc = new ArrayList<>();
            while (true) {
                String node = stack.pop();
                onStack.put(node, false);
                scc.add(node);
                if (node.equals(at)) break;
            }
            sccs.add(scc);
        }
    }
}
