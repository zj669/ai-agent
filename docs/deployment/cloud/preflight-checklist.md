# 云端部署前校验清单（执行前最后确认）

> 目的：在**不改动线上环境**的前提下，确保参数、部署包、依赖文件、回滚信息都齐备。  
> 使用方式：按顺序打勾；全部通过后，才进入正式部署指令执行。

## A. 指令与变更边界确认

- [ ] 已收到明确执行指令（谁批准、执行窗口、允许影响范围）
- [ ] 已确认本次仅执行 Docker 云端部署，不混入功能改造
- [ ] 已明确回滚触发条件（例如：健康检查失败 > N 分钟）

## B. 目标主机与访问参数

- [ ] SSH 目标信息完整：`<user>@<host>`、端口、密钥/认证方式
- [ ] 目标 Linux 发行版与版本已记录（Ubuntu/CentOS 等）
- [ ] 目标主机磁盘容量满足要求（镜像 + 数据卷 + 日志）
- [ ] 防火墙开放端口策略已确认（至少 80/443，按需开放 9001/9090/3000）

## C. 生产参数（必须可替换）

- [ ] `.env.prod` 已准备完成且所有 `change_me`/`replace_with_...` 已替换
- [ ] `JWT_SECRET` 满足 32+ 字符并已存入安全介质
- [ ] `CORS_ALLOWED_ORIGINS` 指向正式域名（HTTPS）
- [ ] MinIO / MySQL / Redis / Grafana 密码均为强口令
- [ ] 域名与证书参数已准备（若启用 HTTPS 反向代理）

## D. 部署包完整性（最容易漏）

- [ ] 后端 Jar 存在：`ai-agent-interfaces-1.0.0-SNAPSHOT.jar`
- [ ] 前端构建产物存在：`frontend-dist/`
- [ ] `docker-compose-prod.yml` 已纳入部署包
- [ ] Compose 依赖挂载文件已纳入部署包：
  - [ ] `mysql/master.cnf`
  - [ ] `mysql/slave.cnf`
  - [ ] `prometheus/prometheus.yml`
  - [ ] `grafana/dashboards/*`
  - [ ] `grafana/datasources/*`
- [ ] `check-cloud-deploy.sh` / `preflight-verify.sh` 已纳入部署包

## E. 可执行验证（仅检查，不启动线上变更）

- [ ] 本地执行：`bash docs/deployment/cloud/preflight-verify.sh env /path/to/.env.prod`
- [ ] 本地执行：`bash docs/deployment/cloud/preflight-verify.sh bundle /path/to/release`
- [ ] 输出无 `ERR`，仅允许与现场相关的 `WARN`

## F. 回滚与审计信息

- [ ] 已准备回滚命令（`docker compose down` / 切回旧版本）
- [ ] 已记录当前线上版本标识（镜像 tag 或 Jar 文件名）
- [ ] 已定义部署后观察窗口与负责人（日志/告警观察）

---

## 最终执行门槛（Go / No-Go）

- **Go**：A~F 全部满足 + 批准人确认窗口开始  
- **No-Go**：任一项缺失（尤其是参数、挂载文件、回滚信息）
