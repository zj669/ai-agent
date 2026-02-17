# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/FileValidator.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/FileValidator.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/FileValidator.java
- Type: .java

## Responsibility
- 文档上传安全校验工具：文件名、大小、扩展名与 MIME 白名单校验。

## Key Symbols / Structure
- 常量
  - `ALLOWED_CONTENT_TYPES`
  - `ALLOWED_EXTENSIONS`
  - `MAX_FILE_SIZE = 50MB`
  - `PATH_TRAVERSAL_PATTERN`
- 方法
  - `validate(MultipartFile file)`
  - `validateFilename(String)`
  - `validateFileSize(long, String)`
  - `validateFileType(String, String)`
  - `getFileExtension(String)`

## Dependencies
- Spring `MultipartFile`
- `java.util.regex.Pattern`

## Notes
- 状态: 正常
- 拦截路径穿越与超大/非法类型文件。
