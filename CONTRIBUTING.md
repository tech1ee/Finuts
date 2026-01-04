# Contributing to Finuts

Thank you for your interest in contributing to Finuts!

## Development Setup

### Prerequisites

- **JDK**: 17+
- **Android Studio**: Ladybug (2024.2.1) or later
- **Xcode**: 15.0+ (for iOS development)
- **Gradle**: 8.11.1 (included via wrapper)

### Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Finuts.git
   ```
3. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. Make your changes
5. Run tests and linter:
   ```bash
   ./gradlew test detekt
   ```
6. Commit your changes
7. Push and create a Pull Request

## Code Style

### Kotlin

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Detekt for static analysis
- Maximum line length: 120 characters
- Use meaningful variable and function names

### Architecture

- Follow Clean Architecture principles
- Use MVVM pattern for presentation layer
- Keep business logic in the `shared` module
- Use Koin for dependency injection

### Testing

- Write tests first (TDD encouraged)
- Minimum 65% code coverage
- Use `kotlin.test` for assertions
- Use Turbine for Flow testing

## Commit Messages

Follow conventional commits:

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `docs`: Documentation changes
- `test`: Adding/updating tests
- `chore`: Build, CI, or tooling changes

Example:
```
feat(accounts): add multi-currency support

- Added Currency enum with major currencies
- Updated Account entity with currency field
- Added currency conversion in TransactionViewModel
```

## Pull Request Process

1. Ensure all tests pass
2. Update documentation if needed
3. Fill out the PR template completely
4. Request review from maintainers
5. Address review comments
6. Squash commits before merge (if requested)

## Code Review

All PRs require at least one approval before merging. Reviewers will check:

- Code quality and style
- Test coverage
- Documentation
- Performance implications
- Security considerations

## Reporting Issues

Use GitHub Issues with appropriate templates:
- **Bug Report**: For bugs and unexpected behavior
- **Feature Request**: For new features or enhancements

## Questions?

Feel free to open a Discussion or reach out to maintainers.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
