# GitClear

GitClear is a minimal Git client for professional teams.

The idea is simple: Git should feel clear, calm and safe. GitClear does not try to hide Git. It shows what is happening, keeps the interface focused, and helps developers avoid risky actions.

## Vision

GitClear is designed for developers who work in real teams:

- simple enough for junior developers
- transparent enough for senior developers
- minimal enough to avoid visual noise
- safe enough for daily company workflows

GitClear should not become a crowded Git dashboard.

It should answer three questions quickly:

1. Where am I?
2. What changed?
3. What am I about to do?

## Current status

Version: `0.1.0-dev`

This first development version can:

- check whether Git is installed and available in `PATH`
- show the detected Git version
- clone a remote repository using the local `git` command
- open an existing local repository
- show the current branch/status line
- show changed files
- show the last Git command executed
- remember recent repositories locally

## Requirements

- Java 21
- Maven
- Git installed and available in `PATH`

## Run from source

```bash
mvn javafx:run
```

## Build

```bash
mvn -DskipTests package
```

## Authentication

GitClear currently delegates authentication to the Git installation on your machine.

This means SSH keys, existing credential helpers, VPN, proxy and company Git settings are handled by your normal Git environment.

GitClear does not store passwords.

## Design principles

### Minimal UI

Only the information needed for the current task should be visible.

### Git transparency

Every important action should show the real Git command being executed.

Example:

```bash
git status --short --branch
```

### Safe by default

Risky operations should be guided, explained and confirmed.

Future examples:

- backup branch before rebase
- warning before pushing to protected branches
- preflight check before push
- clear conflict explanation

### Professional workflow

GitClear is not only for beginners. It should support real team workflows such as:

- ticket-based branch names
- conventional commit messages
- protected branches
- pull before push checks
- clean history workflows

## Roadmap

See [ROADMAP.md](ROADMAP.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Support

If you like the idea and want to support the project, you can use the Sponsor button on GitHub or the configured support link.

GitClear is currently a personal side project developed in small, careful steps.
