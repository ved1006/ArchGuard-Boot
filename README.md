# ArchGuard Boot

ArchGuard Boot is now set up as a full-stack project:

- `Spring Boot` backend in the repo root
- `React + Vite` frontend in [`frontend`](./frontend)

The backend clones a public Spring Boot repository, analyzes its Java code, and returns:

- issue list
- summary counts
- quality score
- dependency graph data

The React frontend calls the backend API and shows the results in a visual dashboard.

## Project Structure

```text
ArchGuard-Boot/
|- src/                         # Spring Boot backend source
|- frontend/                    # React frontend
|- mvnw / mvnw.cmd              # Maven wrapper
|- pom.xml                      # Backend dependencies
|- start-backend.cmd            # Easy backend starter
|- start-frontend.cmd           # Easy frontend starter
|- CODEX_PROMPT.md              # Ready-to-use Codex 5.4 prompt
```

## What Was Fixed

- Converted the backend from console-heavy prototype behavior to JSON APIs.
- Added `POST /api/analyze` for full analysis.
- Added `GET /api/health` for quick backend checks.
- Added CORS so the React frontend can call the backend.
- Made the target repo URL configurable through the request body.
- Improved the rules so they return structured issues.
- Fixed the Windows Maven wrapper for this environment.
- Added a separate React frontend with a repository input, issue cards, stats, and graph view.

## Backend API

### `GET /api/health`

Returns:

```json
{
  "status": "ok"
}
```

### `POST /api/analyze`

Request body:

```json
{
  "repoUrl": "https://github.com/ved1006/campusCore"
}
```

If `repoUrl` is omitted or blank, the backend uses the default repo from `application.properties`.

## How To Run

### Option 1: easiest

Open two terminals in the project root.

Terminal 1:

```bat
start-backend.cmd
```

Terminal 2:

```bat
start-frontend.cmd
```

Then open:

```text
http://localhost:5173
```

### Option 2: manual

Backend:

```bat
mvnw.cmd spring-boot:run
```

Frontend:

```bat
cd frontend
npm.cmd run dev
```

## Requirements

Already installed and verified in this workspace:

- Java 17 compatible backend build
- Maven wrapper dependencies
- Node frontend packages in `frontend/node_modules`

## Verified

These checks were completed successfully in this workspace:

- `cmd /c npm install`
- `cmd /c npm run build`
- `cmd /c mvnw.cmd test`
- Spring Boot startup health check at `GET /api/health`
- End-to-end analysis request against `https://github.com/ved1006/campusCore`

Example verified summary from the backend:

```json
{
  "repoUrl": "https://github.com/ved1006/campusCore",
  "javaFiles": 13,
  "controllers": 1,
  "services": 4,
  "repositories": 1,
  "entities": 1,
  "dtos": 2,
  "exceptions": 3,
  "others": 1,
  "issues": 2,
  "score": 91.42533936651584,
  "cycles": 1
}
```

## Beginner Notes

- `src/main/java/...` contains the Spring Boot backend code.
- `frontend/src/...` contains the React frontend code.
- Spring Boot runs on port `8080`.
- React runs on port `5173`.
- The frontend uses a Vite proxy so calls to `/api/...` go to the backend automatically.

## PowerShell Note

On this machine, `npm` may be blocked by PowerShell execution policy when it tries to use `npm.ps1`.

If that happens, use:

```bat
npm.cmd run dev
```

instead of:

```bat
npm run dev
```
