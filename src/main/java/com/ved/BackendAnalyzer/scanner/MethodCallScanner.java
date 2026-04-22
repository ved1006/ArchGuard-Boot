package com.ved.BackendAnalyzer.scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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

                String packageName = cu.getPackageDeclaration()
                        .map(pkg -> pkg.getNameAsString())
                        .orElse("");

                ClassOrInterfaceDeclaration enclosingClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElse(null);
                String className = enclosingClass != null
                        ? enclosingClass.getNameAsString()
                        : path.getFileName().toString().replace(".java", "");

                for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                    call.getScope().ifPresent(scope -> {
                        String calledObject = scope.toString();
                        String methodName = call.getNameAsString();
                        int lineNumber = call.getBegin().map(position -> position.line).orElse(-1);

                        calls.add(new MethodCallInfo(
                                className,
                                packageName,
                                calledObject,
                                methodName,
                                lineNumber
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
