package com.ved.BackendAnalyzer.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ved.BackendAnalyzer.dto.AnalysisResponse;
import com.ved.BackendAnalyzer.dto.AnalyzeRequest;
import com.ved.BackendAnalyzer.graph.GraphResponse;
import com.ved.BackendAnalyzer.service.AnalyzerService;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalyzerService analyzerService;

    public AnalyzeController(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody(required = false) AnalyzeRequest request) {
        String repoUrl = request != null ? request.getRepoUrl() : null;
        return analyzerService.analyzeRepo(repoUrl);
    }

    @PostMapping("/graph")
    public GraphResponse buildGraph(@RequestBody(required = false) AnalyzeRequest request) {
        String repoUrl = request != null ? request.getRepoUrl() : null;
        return analyzerService.buildDependencyGraphResponse(repoUrl);
    }
}
