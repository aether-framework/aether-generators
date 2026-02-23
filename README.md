![License](https://img.shields.io/badge/license-MIT-red)
![Maven Central](https://img.shields.io/maven-central/v/de.splatgames.aether/aether-generators)
![Version](https://img.shields.io/badge/version-1.1.2-green)

# Aether Generators 🚀

Aether Generators is a suite of **annotation‑driven code generators** for JVM applications. It provides compile‑time
processors to generate **DTOs** and **fluent MVC Builders** (with transient/persistent modes) and a tiny runtime to
support realistic test workflows.

> [!CAUTION]
> ## ❌ Project Discontinued
>
> This repository is no longer maintained due to structural changes within **Splatgames.de Software**.
>
> The project in its current form is considered deprecated.
>
> It may either:
> - be re-released in a redesigned and structurally aligned version, or  
> - remain permanently abandoned.
>
> No updates, fixes, or support will be provided.
>
> Use at your own risk.

---

## ✨ Features

✅ **DTO Generator**: Generates `*Dto` classes into `generated-sources` under the original package path
(e.g. `de.splatgames.software.example.employee.entity.EmployeeDto`). Supports ordered fields, copy/build helpers,
and optional multi‑output (e.g. `EmployeeDto`, `TransactionDto`).

✅ **MVC Builder Generator**: Generates `*Builder` for your entities with a clean testing API:

- `transient()` and `persistent()` modes
- `create()` / `createMany(n)`
- `withX(...)` fluent setters
- Optional defaults via a `DefaultsProvider`
- Relation handling (e.g. auto‑create `@ManyToOne`; `ManyToMany` prepared for join persistence)
- **Core is Spring‑free**; persistence is abstracted via a `PersistAdapter` (separate Spring adapter available)

✅ **Lightweight Runtime**: A tiny `AbstractBuilder` + `PersistAdapter` API; no framework required.

✅ **Java 17+** baseline; works on newer JVMs (21/25) due to forward compatibility.

---

## 📦 Installation

Aether Generators is available via **Maven** and **Gradle**.

#### **Maven**

> 🎉 All Aether products are available on **Maven Central** – no extra repository required!

```xml

<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-generators</artifactId>
    <version>1.1.2</version>
</dependency>
```

#### **Gradle**

```groovy
dependencies {
    implementation 'de.splatgames.aether:aether-generators:1.1.2'
}
```

> ℹ️ Fine‑grained usage per module (e.g., `aether-mvc-annotations`, `aether-mvc-runtime`, `aether-mvc-processor`) is
> supported; the umbrella artifact provides an easy start.

---

## 🚀 Quick Start

**1. Annotate your entity**

```java
import de.splatgames.aether.mvcbuilder.annotations.MvcBuilder;

@MvcBuilder
public class Role {
    private Long id;            // ignored by builder if annotated accordingly
    private String name;
    private boolean isDefault;
    // @ManyToMany List<Permission> permissions;
}
```

**2. Use the generated builder in tests**

```java
// Transient object (no DB writes)
var role = new RoleBuilder()
    .transientMode()
    .withName("ADMIN")
    .withDefault(true)
    .create();

// Persistent object (uses PersistAdapter, e.g. Spring adapter)
var persisted = new RoleBuilder(new SpringPersistAdapter(ctx), new RoleDefaults())
    .persistent()
    .withName("USER")
    .create();
```

**3. Enable annotation processing (Maven)**

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <parameters>true</parameters>
        <release>17</release>
    </configuration>
</plugin>
```

---

## 📢 Latest Release

- 🚀 **Version:** `1.1.2`
- 📅 **Release Date:** `September 2, 2025`
- 📦 **Available on**:
  [![Maven Central](https://img.shields.io/maven-central/v/de.splatgames.aether/aether-generators)](https://search.maven.org/artifact/de.splatgames.aether/aether-generators)

---

## 🤝 Contributing

We welcome contributions! 🎉  
Please check out our [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute.

---

## 📜 License

Aether Generators is released under the **MIT License**.

```text
MIT License

Copyright (c) 2025 Splatgames.de Software and Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy of this software...
```

---

## 🌟 Conclusion

🔥 **Get started with Aether Generators now!** 🚀
