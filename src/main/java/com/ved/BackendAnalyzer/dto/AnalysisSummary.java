package com.ved.BackendAnalyzer.dto;

import java.util.List;

import com.ved.BackendAnalyzer.model.ClassInfo;

public class AnalysisSummary {

    private final String repoUrl;
    private final int javaFiles;
    private final long controllers;
    private final long services;
    private final long repositories;
    private final long entities;
    private final long dtos;
    private final long exceptions;
    private final long others;
    private final int issues;
    private final double score;
    private final int cycles;

    public AnalysisSummary(String repoUrl,
                           int javaFiles,
                           long controllers,
                           long services,
                           long repositories,
                           long entities,
                           long dtos,
                           long exceptions,
                           long others,
                           int issues,
                           double score,
                           int cycles) {
        this.repoUrl = repoUrl;
        this.javaFiles = javaFiles;
        this.controllers = controllers;
        this.services = services;
        this.repositories = repositories;
        this.entities = entities;
        this.dtos = dtos;
        this.exceptions = exceptions;
        this.others = others;
        this.issues = issues;
        this.score = score;
        this.cycles = cycles;
    }

    public static AnalysisSummary from(String repoUrl,
                                       int javaFiles,
                                       List<ClassInfo> classes,
                                       int issueCount,
                                       double score,
                                       int cycles) {
        return new AnalysisSummary(
                repoUrl,
                javaFiles,
                count(classes, ClassInfo.Type.CONTROLLER),
                count(classes, ClassInfo.Type.SERVICE),
                count(classes, ClassInfo.Type.REPOSITORY),
                count(classes, ClassInfo.Type.ENTITY),
                count(classes, ClassInfo.Type.DTO),
                count(classes, ClassInfo.Type.EXCEPTION),
                count(classes, ClassInfo.Type.OTHER),
                issueCount,
                score,
                cycles
        );
    }

    private static long count(List<ClassInfo> classes, ClassInfo.Type type) {
        return classes.stream().filter(classInfo -> classInfo.getType() == type).count();
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public int getJavaFiles() {
        return javaFiles;
    }

    public long getControllers() {
        return controllers;
    }

    public long getServices() {
        return services;
    }

    public long getRepositories() {
        return repositories;
    }

    public long getEntities() {
        return entities;
    }

    public long getDtos() {
        return dtos;
    }

    public long getExceptions() {
        return exceptions;
    }

    public long getOthers() {
        return others;
    }

    public int getIssues() {
        return issues;
    }

    public double getScore() {
        return score;
    }

    public int getCycles() {
        return cycles;
    }
}
