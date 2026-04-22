package com.ved.BackendAnalyzer.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaFileScanner {

    public List<Path> scan(Path repoRoot) {
        Path sourceRoot = detectSourceRoot(repoRoot);
        try {
            if (!Files.exists(sourceRoot)) {
                return List.of();
            }

            try (Stream<Path> pathStream = Files.walk(sourceRoot)) {
                return pathStream
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(this::isProjectSource)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan java files", e);
        }
    }

    public Path detectSourceRoot(Path repoRoot) {
        List<Path> candidates = List.of(
                repoRoot.resolve("src/main/java"),
                repoRoot.resolve("src")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return repoRoot;
    }

    private boolean isProjectSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return !normalized.contains("/target/")
                && !normalized.contains("/build/")
                && !normalized.contains("/node_modules/")
                && !normalized.contains("/.git/");
    }
}
