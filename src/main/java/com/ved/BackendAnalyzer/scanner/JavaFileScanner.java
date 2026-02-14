package com.ved.BackendAnalyzer.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JavaFileScanner {

    public List<Path> scan(Path repoRoot) {
        try {
            Path srcMainJava = repoRoot.resolve("src/main/java");

            if (!Files.exists(srcMainJava)) {
                System.out.println("No src/main/java found");
                return List.of();
            }

            return Files.walk(srcMainJava)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan java files", e);
        }
    }
}
