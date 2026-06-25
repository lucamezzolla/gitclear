# Contributing to GitClear

Thanks for your interest in GitClear.

This project is currently in an early development phase. The main priority is to keep the product direction clear:

> Minimal interface. Professional workflow. Transparent Git commands.

## Development setup

Requirements:

- Java 21
- Maven
- Git

Run the app:

```bash
mvn javafx:run
```

Compile:

```bash
mvn -DskipTests compile
```

Package:

```bash
mvn -DskipTests package
```

## Code style

For now, keep the code simple and readable.

Preferred principles:

- avoid magic behavior
- keep Git commands visible
- keep UI classes understandable
- prefer small services over large mixed classes
- add clear error messages for users

## Product rules

Before adding a feature, ask:

1. Does this help a developer understand what is happening?
2. Does this reduce risk?
3. Does this keep the interface calm?
4. Would this be useful in a company workflow?

If the feature adds noise without improving daily Git work, it probably does not belong in the main screen.

## Commit messages

Use clear commit messages.

Examples:

```text
Add repository clone dialog
Improve status parsing
Add GitHub Actions build
```

## Pull requests

Pull requests should include:

- what changed
- why it changed
- how it was tested
- screenshots for UI changes when useful

GitClear is still small. Keeping changes focused is more important than moving fast.
