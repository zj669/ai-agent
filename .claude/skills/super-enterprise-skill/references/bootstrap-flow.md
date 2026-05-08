# Bootstrap 流程

创建新的工作区 skill 时：

1. 确认目标工作区根目录、目标 skill 名称、安装位置和当前可信事实来源。
2. 读取 `references/required-data.md`，收集初始化所需最小数据集。
3. 在创造新结构前，先扫描已有 skill 和 SOP。
4. 先识别业务域。仓库、服务、域名、数据库、脚本和团队只能作为这些业务域下的支撑事实。
5. 将企业级规则与具体业务 SOP 分离。
6. 生成业务 SOP 前应用 `references/sop-authoring-spec.md`。
7. 生成最小可用目录结构：

   ```text
   <target-skill>/
     SKILL.md
     .env.example
     logs/
       YYYY-MM.md
     references/
       <business-domain>/index.md
       incidents/index.md
     scripts/
       README.md
     agents/
       .gitkeep
   ```

8. 根 `SKILL.md` 只放全局规则、业务地图、验证标准、输出规则和知识回写规则。
9. 具体业务流程放在 `references/<business-domain>/...`；不要把所有 SOP 内联到根文件。
10. 为已知业务域添加路由条目，并为未知业务域保留明确占位。
11. 以本 skill 根目录的 `.env.example` 为模板，按目标工作区实际使用的技术栈裁剪占位符变量，生成目标 skill 的 `.env.example`；绝不生成带真实密钥的 `.env`。
12. 在增加执行能力前先添加安全边界。
13. 按 `references/validation-checklist.md` 验证。
14. 汇报创建的文件、假设、缺失输入和后续维护动作。
