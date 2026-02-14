package com.ved.BackendAnalyzer.scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.ved.BackendAnalyzer.model.ClassInfo;

public class AnnotationScanner {

    public List<ClassInfo> scan(List<Path> javaFiles) {
        List<ClassInfo> result = new ArrayList<>();
        JavaParser parser = new JavaParser();

        for (Path path : javaFiles) {
            try {
                CompilationUnit cu = parser.parse(path).getResult().orElse(null);
                if (cu == null) continue;

                String pkg = cu.getPackageDeclaration()
                        .map(p -> p.getNameAsString())
                        .orElse("");

                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    ClassInfo.Type type = detectType(cls);
                    result.add(new ClassInfo(
                            cls.getNameAsString(),
                            pkg,
                            type
                    ));
                }

            } catch (Exception e) {
                // ignore broken files for now
            }
        }
        return result;
    }

    private ClassInfo.Type detectType(ClassOrInterfaceDeclaration cls) {
        if (cls.isAnnotationPresent("RestController") || cls.isAnnotationPresent("Controller"))
            return ClassInfo.Type.CONTROLLER;

        if (cls.isAnnotationPresent("Service"))
            return ClassInfo.Type.SERVICE;

        if (cls.isAnnotationPresent("Repository"))
            return ClassInfo.Type.REPOSITORY;

        if (cls.isAnnotationPresent("Entity"))
            return ClassInfo.Type.ENTITY;

        return ClassInfo.Type.OTHER;
    }
}
