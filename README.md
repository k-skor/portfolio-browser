# portfolio-browser
Mobile Multiplatform GitHub Project Browser

## Project Description

The goal of this project is to create a mobile multiplatform application that allows users to browse
and interact with their projects. It should allow multiple ID providers to login and integrate import
feature from GitHub, Figma, LinkedIn, etc. It should allow interactions between users on the
content they share like in a social media app.

## Development

1. Firebase - add `google-services.json` to the `androidApp/src/debug`
2. Secrets - create `secrets.local` in the root dir and fill it with values:
```
githubApiKey=
githubApiUser=
```
   

# Project Roadmap

## Milestone 1 (2025-2026)

1. Project structure with the Compose+KMP+MVI architecture
2. Functional requirements:
   - DB integration
   - authentication + guest profile
   - provider integration
   - UI with list+details+profile+settings pages
3. UX concept
4. Technical requirements:
   - use Koin for DI
   - implement MVI store on top of OrbitMVI
   - Ktor for network calls
   - multiplatform functionality (Android + iOS skeleton)
   - DB integration

## Milestone 2 (2026-)

1. [Refactor](refactor-plan.md) the codebase for easier User Stories implementation
2. Functional requirements:
   - extend external providers
   - role based resource access
3. Alpha release
