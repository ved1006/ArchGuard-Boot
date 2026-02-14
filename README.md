# BackendAnalyzer

BackendAnalyzer is a static code analysis tool specifically designed for Spring Boot applications. It automates the code review process by scanning Java source code for common architectural violations and deviations from Spring Boot best practices.

## 🚀 Features

-   **Automated Repository Cloning**: Directly clones Git repositories for analysis.
-   **AST-Based Parsing**: Uses `JavaParser` to deeply understand the code structure, not just regex matching.
-   **Layered Architecture Enforcement**: Detects when Controllers bypass the Service layer to access Repositories directly.
-   **DTO Pattern Enforcement**: Identifies Controllers that return Database Entities directly instead of DTOs.
-   **Performance Checks**: Flags potential performance bottlenecks like unpaginated `findAll()` calls.
-   **Exception Handling**: Checks for the existence of global exception handling (`@RestControllerAdvice`).

## 🛠 Tech Stack

-   **Language**: Java 17
-   **Framework**: Spring Boot 3.x
-   **Parsing**: JavaParser (javaparser-core)
-   **Git Integration**: JGit (org.eclipse.jgit)
-   **Build Tool**: Maven

## 📋 Implemented Rules

The analyzer currently enforces the following rules:

1.  **ControllerRepositoryRule**: Flagged when a `@RestController` directly injects or calls a `@Repository`. Controllers should always go through a Service layer.
2.  **EntityReturnedFromControllerRule**: Flagged when a Controller method returns a class annotated with `@Entity`. Use DTOs to decouple the API from the database schema.
3.  **MissingServiceLayerRule**: Flagged if the project lacks a Service layer concept or if business logic appears to be misplaced.
4.  **UnpaginatedFindAllRule**: Flagged when `findAll()` is called without `Pageable`. This can cause memory overflows in large databases.
5.  **MissingRestControllerAdviceRule**: Flagged if no class is annotated with `@RestControllerAdvice`, indicating a lack of centralized exception handling.

## 📦 Installation & Usage

### Prerequisites
-   Java 17+
-   Maven

### Steps
1.  **Clone the BackendAnalyzer**:
    ```bash
    git clone <your-repo-url>
    cd BackendAnalyzer
    ```

2.  **Build the Project**:
    ```bash
    mvn clean install
    ```

3.  **Run the Analyzer**:
    You can run it as a Spring Boot application.
    ```bash
    mvn spring-boot:run
    ```

4.  **Trigger Analysis**:
    The tool exposes a REST endpoint to trigger the analysis.
    ```bash
    curl -X POST http://localhost:8080/analyze
    ```
    *Note: Currently, the target repository URL is configured in `AnalyzerService.java`. Update the `repoUrl` variable to analyze a different repository.*

## 📂 Project Structure

```
src/main/java/com/ved/BackendAnalyzer
├── controller       # REST endpoints to trigger analysis
├── git              # Git cloning and management logic
├── model            # Internal data models (Issue, ClassInfo, etc.)
├── rules            # Implementation of specific analysis rules
├── scanner          # Code scanners (Annotation, MethodCall, JavaFile)
├── service          # Main business logic orchestrating the analysis
└── utils            # Utility classes
```

## 🔮 Future Roadmap

-   **Dynamic Configuration**: Pass repository URL as a request parameter.
-   **CI/CD Integration**: a GitHub Action to block PRs on violations.
-   **Web Dashboard**: A UI to visualize issues and trends.
-   **Custom Rules**: A DSL to define custom architectural rules.
