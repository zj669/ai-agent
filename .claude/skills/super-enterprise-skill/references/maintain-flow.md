# Maintain 流程

更新已有业务分层工作区 skill 时：

1. 读取目标根 `SKILL.md`。
2. 如果目标 skill 存在最近日志，读取这些日志。
3. 判断本次变更归属（详见 `references/maintenance-playbook.md`）：
   - 根 `SKILL.md`；
   - `logs/YYYY-MM.md`；
   - `references/<business-domain>/...`；
   - `references/incidents/...`；
   - `scripts/<family>/README.md`；
   - `agents/<agent-name>/...`；
   - evals 或 templates。
4. 修改拥有该知识的最窄文件。
5. 除非路由确实变化，否则保持稳定的业务路由 ID 和稳定的路由措辞。
6. 不要为了局部 SOP 变更重写整个根 skill。
7. 如果规则因用户纠正或生产事故而变化，同时写入：
   - `logs/YYYY-MM.md` 中带日期的简短事实；
   - 相关业务 SOP 或根章节中的可复用规则。
8. 验证结构、链接、触发词、路由冲突和安全等级。
