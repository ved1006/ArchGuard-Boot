package com.ved.BackendAnalyzer.dto;

import java.util.List;

import com.ved.BackendAnalyzer.graph.GraphResponse;
import com.ved.BackendAnalyzer.model.Issue;

public class AnalysisResponse {

    private final String repoUrl;
    private final String analyzedAt;
    private final AnalysisSummary summary;
    private final List<Issue> issues;
    private final GraphResponse graph;

    public AnalysisResponse(String repoUrl,
                            String analyzedAt,
                            AnalysisSummary summary,
                            List<Issue> issues,
                            GraphResponse graph) {
        this.repoUrl = repoUrl;
        this.analyzedAt = analyzedAt;
        this.summary = summary;
        this.issues = issues;
        this.graph = graph;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getAnalyzedAt() {
        return analyzedAt;
    }

    public AnalysisSummary getSummary() {
        return summary;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public GraphResponse getGraph() {
        return graph;
    }
}
