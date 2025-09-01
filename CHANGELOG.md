# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2025-09-01

### Improvements

- More stable and deterministic DTO generation (consistent field ordering; reproducible `serialVersionUID`).
- Clearer compiler diagnostics for misconfiguration.
- Minor consistency tweaks to generated `toString()`, `equals()`, and `hashCode()`.
- Javadoc added and small quality improvements to generated outputs.

### Compatibility

- No changes to annotations or public API signatures of generated classes.
- No migration steps required.

### Internal

- Internal modularization and maintenance cleanups.

## [1.0.1] - 2025-09-01

### Bug Fixes
- Replaced falsely implemented Splatgames.de internal validations library with `java.util.Objects` for null checks.

### Enhancements
- Added NOTICE file to the project root to comply with Apache-2.0 license requirements.

## [1.0.0] - 2025-08-31
### 🎉 Initial Release
- First stable release of the project.
