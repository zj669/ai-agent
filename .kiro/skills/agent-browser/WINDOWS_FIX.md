# agent-browser Windows 启动问题修复

## 问题现象

```
✗ Daemon failed to start (socket: C:\Users\xxx\.agent-browser\default.sock)
```

## 根本原因

Rust CLI 在 Windows 上通过 `cmd /c` 启动 Node.js daemon 时存在路径转义问题，导致 daemon 无法正确启动。

相关 Issue: [#90](https://github.com/vercel-labs/agent-browser/issues/90), [#132](https://github.com/vercel-labs/agent-browser/issues/132)

## 解决方案

手动启动 daemon，绕过 Rust CLI 的路径问题：

```bash
# 1. 找到全局 node_modules 路径
npm root -g
# 输出示例: E:\WorkSpace\env\nvm\node_modules

# 2. 进入 agent-browser dist 目录并启动 daemon
cd <npm_root>/agent-browser/dist
node daemon.js

# 3. 在另一个终端正常使用 agent-browser
agent-browser open https://example.com
```

## 状态

- PR [#314](https://github.com/vercel-labs/agent-browser/pull/314) 已提交修复，等待合并
- 当前版本 (0.9.2) 仍需手动启动 daemon
