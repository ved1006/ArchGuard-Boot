package com.ved.BackendAnalyzer.scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.ved.BackendAnalyzer.model.MethodCallInfo;

public class MethodCallScanner {

    public List<MethodCallInfo> scan(List<Path> javaFiles) {
        List<MethodCallInfo> calls = new ArrayList<>();
        JavaParser parser = new JavaParser();

        for (Path path : javaFiles) {
            try {
                CompilationUnit cu = parser.parse(path).getResult().orElse(null);
                if (cu == null) continue;

                String className = path.getFileName().toString();

                for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                    call.getScope().ifPresent(scope -> {
                        String calledObject = scope.toString();
                        String methodName = call.getNameAsString();

                        calls.add(new MethodCallInfo(
                                className,
                                calledObject,
                                methodName
                        ));
                    });
                }

            } catch (Exception e) {
                // ignore broken files
            }
        }
        return calls;
    }
}
