package com.ved.BackendAnalyzer.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

@Component
public class RepoCloner {

    public File cloneRepo(String repoUrl) {
        try {
            Path tempDir = Files.createTempDirectory("repo-");

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setCloneSubmodules(false)
                    .setDirectory(tempDir.toFile())
                    .call();

            return tempDir.toFile();

        } catch (Exception e) {
            throw new RuntimeException("Failed to clone repo", e);
        }
    }

    public void deleteRepo(File repoDir) {
        if (repoDir == null || !repoDir.exists()) {
            return;
        }

        try (var walk = Files.walk(repoDir.toPath())) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {
        }
    }
}
