# Backend Development Guidelines

> Best practices for backend development in the AI Agent Platform (Java 21 + Spring Boot 3.4.9 + Spring AI 1.0.1).

---

## Overview

This directory contains guidelines for backend development. The backend follows a strict **DDD (Domain-Driven Design)** multi-module Maven architecture with separated bounded contexts.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | DDD module organization, package layout, naming rules | ✅ Filled |
| [Database Guidelines](./database-guidelines.md) | MyBatis Plus, PO patterns, schema management, transactions | ✅ Filled |
| [Error Handling](./error-handling.md) | Exception types, GlobalExceptionHandler, Response<T> | ✅ Filled |
| [Quality Guidelines](./quality-guidelines.md) | Forbidden patterns, required patterns, naming, testing | ✅ Filled |
| [Logging Guidelines](./logging-guidelines.md) | SLF4J, log levels, module tags, structured logging | ✅ Filled |

---

## Pre-Development Checklist

Before starting backend work, read these files in order:

1. **Always read first**: `directory-structure.md` — understand DDD layers and module layout
2. **Database work**: `database-guidelines.md` — MyBatis Plus patterns, schema location
3. **Error handling**: `error-handling.md` — exception hierarchy, Response<T>
4. **Code standards**: `quality-guidelines.md` — forbidden patterns, naming, review checklist
5. **Adding logging**: `logging-guidelines.md` — log levels, format, what to log

---

## Quick Reference

### Build Commands

```bash
mvn clean install                                    # Full build
mvn clean install -DskipTests                        # Fast build (skip tests)
mvn clean install -pl ai-agent-interfaces -am         # Build specific module with dependencies
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local  # Run locally
mvn test                                             # Run all tests
```

### Key Architectural Rules

1. **Domain layer is PURE** — NO Spring, MyBatis, or framework dependencies
2. **Constructor injection** — `@RequiredArgsConstructor` + `final` fields, NO `@Autowired`
3. **Spring AI auto-config is DISABLED** — models created dynamically via `ChatModelPort`
4. **Schema management is MANUAL** — `docker/init/mysql/01_init_schema.sql`, NO Flyway

---

**Language**: All documentation should be written in **English**.
