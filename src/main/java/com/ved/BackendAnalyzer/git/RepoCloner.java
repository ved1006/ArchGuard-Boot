package com.ved.BackendAnalyzer.git;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Component;

@Component
public class RepoCloner {

    public File cloneRepo(String repoUrl) {
        try {
            // create temp directory
            Path tempDir = Files.createTempDirectory("repo-");

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .call();

            System.out.println("Repo cloned at: " + tempDir.toAbsolutePath());
            return tempDir.toFile();

        } catch (Exception e) {
            throw new RuntimeException("Failed to clone repo", e);
        }
    }
}
