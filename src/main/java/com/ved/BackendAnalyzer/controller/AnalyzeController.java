package com.ved.BackendAnalyzer.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public AnalyzeController(AnalyzerService analyzerService) {
    this.analyzerService = analyzerService;
    }
}
