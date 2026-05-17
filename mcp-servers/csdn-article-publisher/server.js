#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const readline = require("node:readline");

const SAVE_ARTICLE_URL =
  "https://bizapi.csdn.net/blog-console-api/v1/postedit/saveArticle";
const DEFAULT_REFERER =
  "https://mp.csdn.net/mp_blog/creation/editor?spm=1000.2115.3001.4503";
const DEFAULT_USER_AGENT =
  "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36";

const TOOL_NAME = "send_article";

const inputSchema = {
  type: "object",
  properties: {
    title: {
      type: "string",
      description: "Article title. Maps to saveArticle.title.",
    },
    description: {
      type: "string",
      description: "Article summary/description. Maps to saveArticle.description.",
    },
    content: {
      type: "string",
      description:
        "Article body as HTML. The captured CSDN editor submitted HTML, not raw Markdown.",
    },
    tags: {
      anyOf: [
        { type: "array", items: { type: "string" } },
        { type: "string" },
      ],
      description:
        "Article tags. Array values are joined with commas; string values are sent as-is.",
    },
  },
  required: ["title", "description", "content", "tags"],
  additionalProperties: false,
};

const toolDefinition = {
  name: TOOL_NAME,
  description:
    "Publish a CSDN article through the CSDN saveArticle API captured from the editor HAR.",
  inputSchema,
};

const rl = readline.createInterface({
  input: process.stdin,
  crlfDelay: Infinity,
});

let exitTimer = null;
let completedToolResponse = false;

rl.on("line", async (line) => {
  if (completedToolResponse) {
    return;
  }

  const trimmed = line.trim();
  if (!trimmed) {
    return;
  }

  let request;
  try {
    request = JSON.parse(trimmed);
  } catch (error) {
    writeError(null, -32700, `Invalid JSON-RPC message: ${error.message}`);
    scheduleExit();
    return;
  }

  try {
    await handleRequest(request);
  } catch (error) {
    writeError(request.id ?? null, -32000, error.message || String(error));
    scheduleExit();
  }
});

rl.on("close", () => {
  if (process.env.MCP_KEEP_ALIVE !== "1") {
    scheduleExit(0);
  }
});

async function handleRequest(request) {
  const method = request.method;

  if (method === "notifications/initialized") {
    return;
  }

  if (method === "initialize") {
    writeResult(request.id, {
      protocolVersion: "2024-11-05",
      capabilities: {
        tools: {},
      },
      serverInfo: {
        name: "csdn-article-publisher-mcp",
        version: "0.1.0",
      },
    });
    return;
  }

  if (method === "tools/list") {
    writeResult(request.id, {
      tools: [toolDefinition],
    });
    completedToolResponse = true;
    scheduleExit();
    return;
  }

  if (method === "tools/call") {
    const params = request.params || {};
    if (params.name !== TOOL_NAME) {
      throw new Error(`Unknown tool: ${params.name || "<missing>"}`);
    }

    const result = await sendArticle(params.arguments || {});
    writeResult(request.id, {
      content: [
        {
          type: "text",
          text: JSON.stringify(result, null, 2),
        },
      ],
    });
    completedToolResponse = true;
    scheduleExit();
    return;
  }

  writeError(request.id ?? null, -32601, `Method not found: ${method}`);
  scheduleExit();
}

async function sendArticle(args) {
  const payload = buildPayload(args);
  const dryRun = process.env.CSDN_DRY_RUN === "1";

  if (dryRun) {
    return {
      success: true,
      dry_run: true,
      url: SAVE_ARTICLE_URL,
      payload,
    };
  }

  const cookie = getCookieHeader();
  if (!cookie) {
    throw new Error(
      "Missing CSDN_COOKIE. Configure the MCP server env with a valid CSDN browser cookie."
    );
  }

  const response = await fetch(SAVE_ARTICLE_URL, {
    method: "POST",
    headers: buildRequestHeaders(cookie),
    body: JSON.stringify(payload),
  });

  const responseText = await response.text();
  const responseJson = parseJson(responseText);

  if (!response.ok) {
    throw new Error(
      `CSDN saveArticle HTTP ${response.status}: ${truncate(responseText, 600)}`
    );
  }

  const code = responseJson && Number(responseJson.code);
  if (Number.isFinite(code) && code !== 200) {
    throw new Error(
      `CSDN saveArticle failed: ${truncate(responseText, 600)}`
    );
  }

  return {
    success: true,
    article_id:
      responseJson?.data?.article_id ?? responseJson?.data?.articleId ?? null,
    url: responseJson?.data?.url ?? null,
    title: responseJson?.data?.title ?? payload.title,
    description: responseJson?.data?.description ?? payload.description,
    message: responseJson?.msg || responseJson?.message || "success",
    traceId: responseJson?.traceId,
    raw: responseJson || responseText,
  };
}

function buildRequestHeaders(cookie) {
  const headers = {
    accept: "application/json, text/plain, */*",
    "content-type": "application/json;charset=UTF-8",
    origin: "https://mp.csdn.net",
    referer: process.env.CSDN_REFERER || DEFAULT_REFERER,
    "user-agent": process.env.CSDN_USER_AGENT || DEFAULT_USER_AGENT,
    cookie,
  };

  addOptionalHeader(headers, "x-ca-key", process.env.CSDN_X_CA_KEY);
  addOptionalHeader(headers, "x-ca-nonce", process.env.CSDN_X_CA_NONCE);
  addOptionalHeader(headers, "x-ca-signature", process.env.CSDN_X_CA_SIGNATURE);
  addOptionalHeader(
    headers,
    "x-ca-signature-headers",
    process.env.CSDN_X_CA_SIGNATURE_HEADERS
  );

  return headers;
}

function addOptionalHeader(headers, name, value) {
  const text = value != null ? String(value).trim() : "";
  if (text) {
    headers[name] = text;
  }
}

function buildPayload(args) {
  requireNonBlank(args.title, "title");
  requireNonBlank(args.description, "description");
  requireNonBlank(args.content, "content");

  const tags = normalizeTags(args.tags);
  requireNonBlank(tags, "tags");

  return {
    article_id: "",
    title: String(args.title),
    description: String(args.description),
    content: String(args.content),
    tags,
    categories: "",
    type: "original",
    status: 0,
    read_type: "public",
    creation_statement: 1,
    reason: "",
    original_link: "",
    authorized_status: false,
    check_original: false,
    source: "pc_postedit",
    not_auto_saved: 1,
    creator_activity_id: "",
    cover_images: [],
    cover_type: 0,
    vote_id: 0,
    resource_id: "",
    scheduled_time: 0,
    is_new: 1,
    sync_git_code: 0,
  };
}

function getCookieHeader() {
  if (process.env.CSDN_COOKIE_FILE) {
    return fs.readFileSync(process.env.CSDN_COOKIE_FILE, "utf8").trim();
  }
  return (process.env.CSDN_COOKIE || "").trim();
}

function normalizeTags(tags) {
  if (Array.isArray(tags)) {
    return tags
      .map((tag) => String(tag).trim())
      .filter(Boolean)
      .join(",");
  }
  if (typeof tags === "string") {
    return tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean)
      .join(",");
  }
  return "";
}

function requireNonBlank(value, name) {
  if (value === undefined || value === null || String(value).trim() === "") {
    throw new Error(`Missing required argument: ${name}`);
  }
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function truncate(text, maxLength) {
  if (!text || text.length <= maxLength) {
    return text || "";
  }
  return `${text.slice(0, maxLength)}...`;
}

function writeResult(id, result) {
  process.stdout.write(
    `${JSON.stringify({
      jsonrpc: "2.0",
      id,
      result,
    })}\n`
  );
}

function writeError(id, code, message) {
  process.stdout.write(
    `${JSON.stringify({
      jsonrpc: "2.0",
      id,
      error: {
        code,
        message,
      },
    })}\n`
  );
}

function scheduleExit(delayMs = 50) {
  if (process.env.MCP_KEEP_ALIVE === "1") {
    return;
  }
  if (exitTimer) {
    clearTimeout(exitTimer);
  }
  exitTimer = setTimeout(() => process.exit(0), delayMs);
}
