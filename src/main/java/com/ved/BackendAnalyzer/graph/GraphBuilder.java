package com.ved.BackendAnalyzer.graph;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.ved.BackendAnalyzer.model.ClassInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class GraphBuilder {

    private final JavaParser parser;

    public GraphBuilder(Path sourceRoot) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(sourceRoot));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(symbolSolver);
        this.parser = new JavaParser(parserConfiguration);
    }

    public DependencyGraph buildGraph(List<Path> javaFiles) {
        DependencyGraph graph = new DependencyGraph();

        // Pass 1: Parse files, create nodes for all internal classes
        for (Path path : javaFiles) {
            try {
                parser.parse(path).getResult().ifPresent(cu -> {
                    for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                        String fqn = cls.getFullyQualifiedName().orElse(cls.getNameAsString());
                        ClassInfo.Type layer = detectLayer(cls);
                        GraphNode node = new GraphNode(fqn, cls.getNameAsString(), layer);
                        graph.addNode(node);
                    }
                });
            } catch (Exception e) {
                // Ignore parse errors for now
            }
        }

        // Pass 2: Extract dependencies
        for (Path path : javaFiles) {
            try {
                parser.parse(path).getResult().ifPresent(cu -> {
                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                        String fqn = cls.getFullyQualifiedName().orElse(cls.getNameAsString());
                        GraphNode sourceNode = graph.getNodeMap().get(fqn);
                        if (sourceNode != null) {
                            extractDependencies(cls, sourceNode, graph);
                        }
                    });
                });
            } catch (Exception e) {
                // Ignore
            }
        }

        return graph;
    }

    private void extractDependencies(ClassOrInterfaceDeclaration cls, GraphNode sourceNode, DependencyGraph graph) {
        // 1. Inheritance
        for (ClassOrInterfaceType extendedType : cls.getExtendedTypes()) {
            addTypeDependency(extendedType, sourceNode, graph, GraphEdge.DependencyType.INHERITANCE);
        }

        // 2. Interface Implementation
        for (ClassOrInterfaceType implementedType : cls.getImplementedTypes()) {
            addTypeDependency(implementedType, sourceNode, graph, GraphEdge.DependencyType.INTERFACE_IMPLEMENTATION);
        }

        // 3. Field Injection
        cls.findAll(FieldDeclaration.class).forEach(field -> {
            Type elementType = field.getElementType();
            addTypeDependency(elementType, sourceNode, graph, GraphEdge.DependencyType.FIELD_INJECTION);
        });

        // 4. Constructor Injection
        cls.findAll(ConstructorDeclaration.class).forEach(constructor -> {
            constructor.getParameters().forEach(param -> {
                addTypeDependency(param.getType(), sourceNode, graph, GraphEdge.DependencyType.CONSTRUCTOR_INJECTION);
            });
        });

        // 5. Methods (Returns and Parameters)
        cls.findAll(MethodDeclaration.class).forEach(method -> {
            addTypeDependency(method.getType(), sourceNode, graph, GraphEdge.DependencyType.RETURN_TYPE);
            method.getParameters().forEach(param -> {
                addTypeDependency(param.getType(), sourceNode, graph, GraphEdge.DependencyType.PARAMETER_TYPE);
            });
        });

        // 6. Method Calls
        cls.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                var resolvedTarget = call.resolve().declaringType();
                String targetFqn = resolvedTarget.getQualifiedName();
                createEdgeToFqn(targetFqn, sourceNode, graph, GraphEdge.DependencyType.METHOD_CALL);
            } catch (UnsolvedSymbolException | UnsupportedOperationException | IllegalStateException e) {
                // Symbol cannot be resolved, ignore
            }
        });
    }

    private void addTypeDependency(Type type, GraphNode sourceNode, DependencyGraph graph, GraphEdge.DependencyType depType) {
        try {
            ResolvedType resolvedType = type.resolve();
            if (resolvedType.isReferenceType()) {
                ResolvedReferenceType refType = resolvedType.asReferenceType();
                createEdgeToFqn(refType.getQualifiedName(), sourceNode, graph, depType);
                
                // Also handle type parameters like List<User> -> Add User as a dependency
                for (ResolvedType typeParam : refType.typeParametersValues()) {
                    if (typeParam.isReferenceType()) {
                        createEdgeToFqn(typeParam.asReferenceType().getQualifiedName(), sourceNode, graph, depType);
                    }
                }
            }
        } catch (UnsolvedSymbolException | UnsupportedOperationException | IllegalStateException e) {
            // Cannot resolve type
        }
    }

    private void createEdgeToFqn(String targetFqn, GraphNode sourceNode, DependencyGraph graph, GraphEdge.DependencyType depType) {
        if ("void".equals(targetFqn)) return;
        
        String simpleName = targetFqn.substring(targetFqn.lastIndexOf('.') + 1);
        GraphNode targetNode = graph.getOrCreateNode(targetFqn, simpleName, ClassInfo.Type.EXTERNAL);
        
        // Architectural Violation Detection: Controller -> Repository
        boolean isViolation = sourceNode.getLayer() == ClassInfo.Type.CONTROLLER && 
                              targetNode.getLayer() == ClassInfo.Type.REPOSITORY;
        
        graph.addEdge(sourceNode, targetNode, depType, isViolation);
    }

    private ClassInfo.Type detectLayer(ClassOrInterfaceDeclaration cls) {
        // 1. Check Annotations (High Precision)
        if (cls.isAnnotationPresent("RestController") || cls.isAnnotationPresent("Controller"))
            return ClassInfo.Type.CONTROLLER;
        if (cls.isAnnotationPresent("Service"))
            return ClassInfo.Type.SERVICE;
        if (cls.isAnnotationPresent("Repository"))
            return ClassInfo.Type.REPOSITORY;
        if (cls.isAnnotationPresent("Entity"))
            return ClassInfo.Type.ENTITY;

        // 2. Check Inheritance (Exceptions)
        for (ClassOrInterfaceType extended : cls.getExtendedTypes()) {
            String name = extended.getNameAsString();
            if (name.contains("Exception") || name.contains("Throwable")) {
                return ClassInfo.Type.EXCEPTION;
            }
        }

        // 3. Name-based Heuristics (Services/Repos/DTOs/Exceptions)
        String className = cls.getNameAsString();
        if (className.endsWith("Service") || className.endsWith("ServiceImpl")) {
            return ClassInfo.Type.SERVICE;
        }
        if (className.endsWith("Repository") || className.endsWith("Repo") || className.endsWith("DAO")) {
            return ClassInfo.Type.REPOSITORY;
        }
        if (className.endsWith("DTO") || className.endsWith("Response") || className.endsWith("Request")) {
            return ClassInfo.Type.DTO;
        }
        if (className.endsWith("Exception")) {
            return ClassInfo.Type.EXCEPTION;
        }

        return ClassInfo.Type.OTHER;
    }
}
