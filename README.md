# GitClear

GitClear is a minimal Git client for professional teams.

The goal is simple: make Git calm, clear and safe without hiding what Git is doing.

## Current status

Version: `0.1.0-dev`

This first development version can:

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

## Run

```bash
mvn javafx:run
```

## Design principles

- Minimal UI
- Clear actions
- Git commands are visible
- No password storage
- Local-first
- Safe by default

## Authentication

GitClear currently delegates authentication to the Git installation on your machine.

This means SSH keys, existing credential helpers, VPN, proxy and company Git settings are handled by your normal Git environment.
