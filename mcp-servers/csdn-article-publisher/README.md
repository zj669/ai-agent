# CSDN Article Publisher MCP

Single-tool stdio MCP server for publishing CSDN articles.

The HAR capture shows the successful publish request is:

```text
POST https://bizapi.csdn.net/blog-console-api/v1/postedit/saveArticle
```

The successful request body contained these fields:

```json
{
  "article_id": "",
  "title": "这是一个标题",
  "description": "这是摘要dddddddddddddddddddddddddddddddddddddddddddddd",
  "content": "<p>markdown格式的正文</p>...",
  "tags": "Java,面试,自定义文章标签",
  "categories": "",
  "type": "original",
  "status": 0,
  "read_type": "public",
  "creation_statement": 1,
  "reason": "",
  "original_link": "",
  "authorized_status": false,
  "check_original": false,
  "source": "pc_postedit",
  "not_auto_saved": 1,
  "creator_activity_id": "",
  "cover_images": [
    "https://i-blog.csdnimg.cn/direct/ebb62e62a3eb4dda96ae9140e4e8c344.png"
  ],
  "cover_type": 1,
  "vote_id": 0,
  "resource_id": "",
  "scheduled_time": 0,
  "is_new": 1,
  "sync_git_code": 0
}
```

The server exposes one MCP tool:

```text
send_article
```

## Tool Arguments

```json
{
  "title": "文章标题",
  "description": "文章摘要",
  "content": "<p>HTML 正文</p>",
  "tags": ["Java", "面试", "自定义文章标签"]
}
```

`content` should be HTML because the captured CSDN editor submitted HTML to `saveArticle`, not raw Markdown.

All other `saveArticle` fields are filled internally with the HAR-compatible defaults:
new original public article, `status=0`, `source=pc_postedit`, no category, no cover, no schedule, no vote, no GitCode sync.

## Authentication

Do not hard-code CSDN cookies in source files. Configure authentication through an environment variable:

```bash
export CSDN_COOKIE='UserName=...; UserToken=...; SESSION=...'
node /home/zj669/repo/ai-agent/mcp-servers/csdn-article-publisher/server.js
```

You can also store the full cookie header in a local file and point the server to it:

```bash
export CSDN_COOKIE_FILE=/absolute/path/to/csdn.cookie
```

Optional environment variables:

```text
CSDN_USER_AGENT   Override the browser User-Agent sent to CSDN.
CSDN_REFERER      Override the CSDN editor Referer.
CSDN_X_CA_KEY     CSDN API gateway x-ca-key header captured from the editor request.
CSDN_X_CA_NONCE   CSDN API gateway x-ca-nonce header captured from the editor request.
CSDN_X_CA_SIGNATURE CSDN API gateway x-ca-signature header captured from the editor request.
CSDN_X_CA_SIGNATURE_HEADERS CSDN API gateway signed header list, usually x-ca-key,x-ca-nonce.
CSDN_DRY_RUN=1    Return the payload without sending the HTTP request.
MCP_KEEP_ALIVE=1  Keep the process alive for MCP clients that reuse stdio sessions.
```

## ai-agent MCP Configuration

For local development where the backend runs on the host, create a stdio MCP server config in the platform:

```json
{
  "command": "node",
  "args": [
    "/home/zj669/repo/ai-agent/mcp-servers/csdn-article-publisher/server.js"
  ],
  "env": {}
}
```

Set `CSDN_COOKIE` on the backend process environment so the MCP child process can inherit it.

For production Docker deployment, the backend image copies this directory to `/app/mcp-servers` and runs the tool from inside the backend container:

```json
{
  "command": "node",
  "args": [
    "/app/mcp-servers/csdn-article-publisher/server.js"
  ],
  "env": {}
}
```

Then connect the server from MCP 管理. The discovered tool name will be `send_article`; inside workflow TOOL nodes it will be stored as:

```text
mcp__{serverId}__send_article
```
