# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability, please report it responsibly.

### How to Report

1. **Do NOT** create a public GitHub issue for security vulnerabilities
2. Email security concerns to: [security@finuts.app] (replace with actual email)
3. Include as much detail as possible:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to Expect

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 7 days
- **Resolution Timeline**: Depends on severity
  - Critical: 24-48 hours
  - High: 7 days
  - Medium: 30 days
  - Low: 90 days

### Scope

The following are in scope:
- Finuts mobile applications (iOS, Android)
- Data storage and encryption
- Authentication mechanisms
- API endpoints (if applicable)

### Out of Scope

- Denial of Service attacks
- Social engineering
- Physical security
- Third-party services not under our control

## Security Measures

### Data Protection

- All sensitive data encrypted at rest using SQLCipher
- No PII sent to external AI APIs
- Local-first architecture (data stays on device)
- Biometric authentication support

### Code Security

- Static analysis with Detekt
- Dependency vulnerability scanning
- Code review required for all changes
- Signed releases only

### CI/CD Security

- Secrets stored in GitHub Secrets (never in code)
- Signed commits encouraged
- Protected branches with required reviews

## Best Practices for Contributors

1. Never commit secrets, API keys, or credentials
2. Use environment variables for sensitive configuration
3. Follow secure coding guidelines
4. Report suspicious activity immediately

## Acknowledgments

We appreciate security researchers who help keep Finuts secure. Responsible disclosure will be acknowledged in our Hall of Fame (with permission).
