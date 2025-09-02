# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.2] - 2025-09-02

### Bug Fixes

- Fixed duplicate local variable names in generated `prePersistRelations(...)` methods by introducing
  **field-scoped variable naming** (e.g., `rel_<field>`, `col_<field>`). This prevents compile-time errors when multiple
  relations exist on an entity.
- Corrected traversal logic: cycle guard is applied exclusively in `ensurePersistent(...)`, avoiding premature
  termination in `prePersistRelations(...)`. This ensures proper relation pre-persistence while still protecting
  against infinite loops.
- Depth-limit traversal confirmed: entities beyond the configured depth are no longer incorrectly asserted in tests;
  builder now strictly enforces `REL_DEPTH_LIMIT`.

### Compatibility

- No changes to annotations or existing builder APIs.
- Generated code remains backward-compatible; projects using 1.1.1 require no migration.

---

## [1.1.1] - 2025-09-01

### Bug Fixes

- Fixed `TransientObjectException` when persisting entities with cyclic associations by introducing a safe pre-persist
  traversal (depth limit + identity-based cycle guard).
- Correct handling for `asIdOnly` to-one relations: the builder now assigns identifiers via a robust `writeId(...)`
  path (tries `setId(...)` first, then falls back to the `id` field), preventing transient references from being
  flushed.
- Reflection utilities hardened:
    - `getFieldValue(...)` prefers JavaBean getters (`getX`/`isX`) and falls back to hierarchical field lookup.
    - `setFieldReflect(...)` walks superclasses and no-ops on access errors instead of throwing.

### Enhancements

- Clearer diagnostics during builder generation (defaults provider checks, missing no-arg constructor, and generated API
  name conflicts).
- Safer `SpringPersistAdapter.save(...)`: repository-first; falls back to `EntityManager.persist/merge` based on runtime
  identifier presence.

### Compatibility

- No breaking changes to generated builder APIs; no migration required.

### Tests

- Added/updated integration tests covering:
    - `withRoleId(...)` wiring to an existing FK without creating phantom entities.
    - cyclic supervisor relations to verify traversal + cycle guard do not produce transient flush errors.
    - collection setter assignability (`Collection` vs `List`) for generated builders.

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
