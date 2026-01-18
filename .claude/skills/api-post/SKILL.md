---
name: http-client
description: Universal HTTP client for executing API requests. Use this skill when the user asks to call an external API, test endpoints, debug HTTP requests, or fetch data from a URL. Supports all methods (GET/POST/PUT/DELETE) and SSE streams.
---

# HTTP Client Guide

## Usage Protocol

This skill provides a Python wrapper for making HTTP requests.

**Command Syntax:**
```bash
python scripts/api_client.py --method [METHOD] [OPTIONS] <URL>

```

## Critical Rules

1. **JSON Formatting**: When using `--data`, you **MUST** enclose the JSON string in **SINGLE QUOTES** (`'`) and use double quotes (`"`) inside the JSON.
* ✅ Correct: `--data '{"name": "user"}'`
* ❌ Incorrect: `--data "{\"name\": \"user\"}"` (Prone to shell escaping errors)


2. **Large Responses**: If the expected response is large (e.g., lists, files), ALWAYS use `--output` to save to a file instead of printing to stdout.
3. **Stream**: Use `--stream` only for SSE (Server-Sent Events) or continuous data flows.

## Parameter Reference

| Argument | Flag | Description |
| --- | --- | --- |
| Method | `--method`, `-X` | GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS (Default: GET) |
| Data | `--data`, `-d` | JSON string body. **Remember to use single quotes wrapper.** |
| Header | `--header`, `-H` | Custom headers (Repeatable). Format: `"Key: Value"` |
| Param | `--param`, `-p` | Query parameters (Repeatable). URL encoding is automatic. |
| Output | `--output`, `-o` | Path to save response body. |
| Timeout | `--timeout` | Request timeout in seconds (Default: 30). |

## Execution Patterns

### 1. Basic Data Retrieval (GET)

Use for fetching data or querying resources.

```bash
python scripts/api_client.py --method GET [https://api.example.com/users](https://api.example.com/users) --param "page=1"

```

### 2. Data Submission (POST/PUT)

**CRITICAL**: Strictly follow the JSON quoting rule.

```bash
python scripts/api_client.py --method POST --header "Content-Type: application/json" --data '{"username": "test_user", "role": "admin"}' [https://api.example.com/register](https://api.example.com/register)

```

### 3. Authenticated Request

Include tokens via headers.

```bash
python scripts/api_client.py --method GET --header "Authorization: Bearer <token>" [https://api.example.com/profile](https://api.example.com/profile)

```

### 4. File Download / Large Response

Always redirect to file for safety.

```bash
python scripts/api_client.py --method GET --output .business/downloads/data.json [https://api.example.com/export](https://api.example.com/export)

```
