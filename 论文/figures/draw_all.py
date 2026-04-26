"""
论文流程图绘制脚本
生成以下图片（保存到 figures/ 目录）：
  fig_arch.png        - 系统整体架构图（分层）
  fig_func.png        - 系统功能结构图
  fig_state.png       - 工作流执行状态机图
  fig_exec_flow.png   - 工作流执行流程图
  fig_review_flow.png - 人工检查点审批流程图
  fig_usecase.png     - 系统用例图
"""

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import matplotlib.patheffects as pe
import numpy as np
import os

# ── 注册系统中文字体 ──────────────────────────────────────────────────────────
from matplotlib import font_manager as _fm
for _f in ["/usr/share/fonts/truetype/arphic/uming.ttc",
           "/usr/share/fonts/truetype/arphic/ukai.ttc"]:
    try:
        _fm.fontManager.addfont(_f)
    except Exception:
        pass

# ── 全局字体 ──────────────────────────────────────────────────────────────────
plt.rcParams.update({
    "font.family": ["AR PL UMing CN", "AR PL UKai CN",
                    "DejaVu Sans"],
    "axes.unicode_minus": False,
    "savefig.dpi": 200,
    "savefig.bbox": "tight",
})

OUT = os.path.dirname(os.path.abspath(__file__))

# ─────────────────────────────────────────────────────────────────────────────
# 通用工具
# ─────────────────────────────────────────────────────────────────────────────

def box(ax, x, y, w, h, text, fc="#4C9BE8", tc="white", fontsize=10,
        radius=0.05, bold=False, zorder=3):
    rect = FancyBboxPatch((x - w/2, y - h/2), w, h,
                          boxstyle=f"round,pad={radius}",
                          facecolor=fc, edgecolor="white",
                          linewidth=1.2, zorder=zorder)
    ax.add_patch(rect)
    weight = "bold" if bold else "normal"
    ax.text(x, y, text, ha="center", va="center", fontsize=fontsize,
            color=tc, weight=weight, zorder=zorder+1, wrap=True,
            multialignment="center")

def diamond(ax, x, y, w, h, text, fc="#F5A623", tc="white", fontsize=9, zorder=3):
    dx, dy = w/2, h/2
    pts = [(x, y+dy), (x+dx, y), (x, y-dy), (x-dx, y)]
    poly = plt.Polygon(pts, closed=True, facecolor=fc,
                       edgecolor="white", linewidth=1.2, zorder=zorder)
    ax.add_patch(poly)
    ax.text(x, y, text, ha="center", va="center", fontsize=fontsize,
            color=tc, weight="bold", zorder=zorder+1, multialignment="center")

def arrow(ax, x1, y1, x2, y2, label="", color="#555", fontsize=8,
          connectionstyle="arc3,rad=0.0"):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="-|>", color=color, lw=1.3,
                                connectionstyle=connectionstyle),
                zorder=4)
    if label:
        mx, my = (x1+x2)/2, (y1+y2)/2
        ax.text(mx+0.05, my, label, fontsize=fontsize, color="#333",
                ha="left", va="center", zorder=5)

# ─────────────────────────────────────────────────────────────────────────────
# 1. 系统整体架构图（四层）
# ─────────────────────────────────────────────────────────────────────────────

def draw_arch():
    fig, ax = plt.subplots(figsize=(12, 7))
    ax.set_xlim(0, 12); ax.set_ylim(0, 7)
    ax.axis("off")
    ax.set_facecolor("#F8F9FA")
    fig.patch.set_facecolor("#F8F9FA")

    layers = [
        # (y_center, height, bg, title, items)
        (6.1, 0.9, "#E8F4FD", "接口层  (ai-agent-interfaces)",
         ["REST API Controller", "SSE Emitter", "DTO / 参数校验", "JWT 鉴权过滤器"]),
        (4.8, 1.0, "#EAF7EA", "应用层  (ai-agent-application)",
         ["SchedulerService（调度核心）", "ChatApplicationService", "KnowledgeApplicationService"]),
        (3.4, 1.0, "#FFF3E0", "领域层  (ai-agent-domain)",
         ["Execution 聚合根", "WorkflowGraph", "ExecutionContext（三层记忆）",
          "NodeExecutorStrategy 端口", "StreamPublisher 端口"]),
        (1.9, 1.0, "#FCE4EC", "基础设施层  (ai-agent-infrastructure)",
         ["LLM/CONDITION/KNOWLEDGE/TOOL 执行器", "MyBatis 仓储适配器",
          "Redis(Redisson) 适配器", "Milvus 向量检索适配器", "Spring AI 模型适配器"]),
    ]

    for (yc, h, bg, title, items) in layers:
        rect = FancyBboxPatch((0.3, yc - h/2), 11.4, h,
                              boxstyle="round,pad=0.08",
                              facecolor=bg, edgecolor="#BBBBBB",
                              linewidth=1.5, zorder=2)
        ax.add_patch(rect)
        ax.text(0.6, yc + h/2 - 0.18, title, fontsize=9.5,
                color="#333", weight="bold", va="top", zorder=3)
        cols = len(items)
        xs = np.linspace(1.5, 10.5, cols)
        for xi, item in zip(xs, items):
            box(ax, xi, yc - 0.05, 9.6/cols - 0.25, 0.52, item,
                fc="#FFFFFF", tc="#333333", fontsize=8, radius=0.04)

    # 前端条
    rect_fe = FancyBboxPatch((0.3, 6.7 - 0.35), 11.4, 0.55,
                             boxstyle="round,pad=0.08",
                             facecolor="#EDE7F6", edgecolor="#BBBBBB",
                             linewidth=1.5, zorder=2)
    ax.add_patch(rect_fe)
    ax.text(6, 6.85 - 0.3, "前端  (React 18 + TypeScript   |   ReactFlow 画布   |   SSE Client)",
            fontsize=9, ha="center", va="center", color="#333",
            weight="bold", zorder=3)

    # 数据库条
    dbs = [("MySQL 8.0\n执行记录/审批日志", "#EF9A9A"),
           ("Redis 7.0\n分布式锁/审批队列", "#FFCC80"),
           ("Milvus 2.3\nLTM 向量检索", "#80DEEA")]
    xs_db = [2.2, 6, 9.8]
    for (label, fc), xdb in zip(dbs, xs_db):
        box(ax, xdb, 0.7, 3.0, 0.72, label, fc=fc, tc="#333",
            fontsize=8.5, radius=0.05)

    ax.text(6, 1.3, "── 数据存储层 ──", ha="center", fontsize=8.5,
            color="#777", style="italic")

    # 层间箭头
    for y_from, y_to in [(6.4, 6.15), (6.1-0.45, 4.8+0.5),
                         (4.8-0.5, 3.4+0.5), (3.4-0.5, 1.9+0.5), (1.9-0.5, 0.95+0.05)]:
        ax.annotate("", xy=(6, y_to), xytext=(6, y_from),
                    arrowprops=dict(arrowstyle="<->", color="#AAAAAA",
                                   lw=1.5), zorder=5)

    ax.set_title("基于 Spring Boot 的 AI Agent 智能体编排系统整体架构",
                 fontsize=12, weight="bold", pad=8, color="#222")
    plt.tight_layout()
    plt.savefig(os.path.join(OUT, "fig_arch.png"))
    plt.close()
    print("✓ fig_arch.png")


# ─────────────────────────────────────────────────────────────────────────────
# 2. 系统功能结构图（树形）
# ─────────────────────────────────────────────────────────────────────────────

def draw_func():
    fig, ax = plt.subplots(figsize=(14, 6))
    ax.set_xlim(0, 14); ax.set_ylim(0, 6)
    ax.axis("off")
    fig.patch.set_facecolor("#F8F9FA")

    # 根
    root = (7, 5.3)
    box(ax, *root, 4.5, 0.65, "AI Agent 智能体编排系统",
        fc="#1565C0", bold=True, fontsize=11)

    modules = [
        (1.1, "Agent\n管理", "#1E88E5",
         ["创建/编辑/发布", "版本管理/回滚", "LLM参数配置"]),
        (3.4, "工作流\n编排", "#43A047",
         ["可视化拖拽画布", "DAG环检测", "节点类型配置"]),
        (5.7, "工作流\n执行", "#FB8C00",
         ["DAG拓扑调度", "节点并行执行", "SSE实时推流"]),
        (8.0, "人工检查\n点审批", "#E53935",
         ["BEFORE/AFTER触发", "审批通过/拒绝", "并发安全保护"]),
        (10.3, "知识库\n管理", "#8E24AA",
         ["文档上传分块", "Milvus向量入库", "RAG语义检索"]),
        (12.6, "对话与\n历史", "#00838F",
         ["多轮对话管理", "执行历史查询", "思维链日志"]),
    ]

    for (mx, mname, mc, subs) in modules:
        my = 3.8
        box(ax, mx, my, 2.0, 0.7, mname, fc=mc, fontsize=9, bold=True)
        # 根到模块
        arrow(ax, *root, mx, my + 0.35, color="#AAAAAA")
        # 子功能
        ys = [2.8, 2.1, 1.4]
        for sy, sub in zip(ys, subs):
            box(ax, mx, sy, 1.9, 0.58, sub, fc="#FFFFFF", tc="#333",
                fontsize=7.5, radius=0.04)
            arrow(ax, mx, my - 0.35, mx, sy + 0.29, color="#CCCCCC")

    ax.set_title("系统功能结构图", fontsize=13, weight="bold", pad=6)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT, "fig_func.png"))
    plt.close()
    print("✓ fig_func.png")


# ─────────────────────────────────────────────────────────────────────────────
# 3. 工作流执行状态机图
# ─────────────────────────────────────────────────────────────────────────────

def draw_state():
    fig, ax = plt.subplots(figsize=(11, 7))
    ax.set_xlim(0, 11); ax.set_ylim(0, 7)
    ax.axis("off")
    fig.patch.set_facecolor("#FAFAFA")

    states = {
        "PENDING":           (2, 6,   "#90CAF9", 2.4, 0.65),
        "RUNNING":           (5.5, 6, "#66BB6A", 2.4, 0.65),
        "PAUSED_FOR_REVIEW": (9, 5,   "#FFA726", 2.4, 0.65),
        "SUCCEEDED":         (2, 3.5, "#A5D6A7", 2.4, 0.65),
        "FAILED":            (5.5, 3.5,"#EF9A9A", 2.4, 0.65),
        "CANCELLED":         (9, 6,   "#CE93D8", 2.4, 0.65),
    }

    for name, (x, y, fc, w, h) in states.items():
        box(ax, x, y, w, h, name, fc=fc, tc="#222", fontsize=9.5, bold=True)

    # 初始黑点
    circle = plt.Circle((0.5, 6), 0.18, fc="#222", zorder=5)
    ax.add_patch(circle)

    arrs = [
        ((0.68, 6),     (0.8, 6),   "初始化"),
        ((1.2+0.8, 6),  (4.3, 6),   "start()"),
        ((6.7, 6),      (7.8, 6),   "用户取消"),
        ((6.7, 6),      (7.8, 5),   "checkPause()"),
        ((5.5, 5.68),   (5.5, 5.0+0.33),   "节点失败"),
        ((5.5, 5.68),   (2.0, 4.175),      "全部完成"),
        ((8.0-0.2, 4.68),(6.7, 3.82),  "reject()"),
        ((7.8, 4.68),   (5.5, 4.175),  ""),
        ((9, 4.68),     (6.7, 6-0.33), "resume()"),
    ]

    arrow(ax, 0.68, 6, 0.8, 6, "", color="#333")
    arrow(ax, 1.2, 6, 4.3, 6, "start()", color="#1565C0", fontsize=8)
    arrow(ax, 6.7, 6, 7.8, 6, "用户取消", color="#8E24AA", fontsize=8)
    arrow(ax, 7.8, 5.68, 7.8, 5.33, "checkPause()", color="#E65100", fontsize=8)
    arrow(ax, 5.5, 5.68, 5.5, 4.175, "节点失败", color="#C62828", fontsize=8)
    arrow(ax, 4.3, 5.68, 2.0, 4.175, "全部完成", color="#2E7D32", fontsize=8)
    arrow(ax, 7.8, 4.68, 6.7, 4.175, "reject()", color="#C62828", fontsize=8)
    arrow(ax, 7.8, 4.68, 5.5, 4.175, "", color="#C62828")
    arrow(ax, 7.8+0.2, 5.33-0.1, 6.7, 5.82, "resume()", color="#1565C0",
          fontsize=8, connectionstyle="arc3,rad=0.3")

    # 终态标记
    for sx, sy in [(2, 3.5), (5.5, 3.5), (9, 6)]:
        circle2 = plt.Circle((sx, sy), 0.28, fc="white", ec="#333", lw=1.8, zorder=6)
        circle3 = plt.Circle((sx, sy), 0.20, fc="#333", zorder=7)
        ax.add_patch(circle2); ax.add_patch(circle3)

    ax.set_title("工作流执行状态机图", fontsize=13, weight="bold", pad=6)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT, "fig_state.png"))
    plt.close()
    print("✓ fig_state.png")


# ─────────────────────────────────────────────────────────────────────────────
# 4. 工作流执行流程图
# ─────────────────────────────────────────────────────────────────────────────

def draw_exec_flow():
    fig, ax = plt.subplots(figsize=(8, 13))
    ax.set_xlim(0, 8); ax.set_ylim(0, 13)
    ax.axis("off")
    fig.patch.set_facecolor("#FAFAFA")

    # 泳道背景
    lane_colors = ["#E3F2FD", "#E8F5E9", "#FFF3E0"]
    lane_titles = ["用户端 / 前端", "SchedulerService（应用层）", "基础设施层"]
    xs = [(0, 2.7), (2.7, 5.4), (5.4, 8)]
    for (x0, x1), lc, lt in zip(xs, lane_colors, lane_titles):
        rect = plt.Rectangle((x0, 0), x1-x0, 13, fc=lc, ec="#CCCCCC", lw=1)
        ax.add_patch(rect)
        ax.text((x0+x1)/2, 12.6, lt, ha="center", va="center",
                fontsize=9, weight="bold", color="#444")

    # 节点定义 (x, y, text, shape, color)
    nodes = [
        (1.35, 12, "用户发送\n任务消息", "box", "#42A5F5"),
        (4.05, 12, "建立 SSE 连接\n初始化执行器", "box", "#66BB6A"),
        (4.05, 10.8, "查询 Agent\n获取 DAG JSON", "box", "#66BB6A"),
        (6.7, 10.8, "MySQL/Agent\n版本表查询", "box", "#FF8A65"),
        (4.05, 9.6, "记忆水合\nhydrateMemory()", "box", "#66BB6A"),
        (6.7, 9.6, "Milvus 检索LTM\nMySQL 加载STM", "box", "#FF8A65"),
        (4.05, 8.4, "execution.start()\nDAG 初始化", "box", "#66BB6A"),
        (4.05, 7.2, "取首批就绪节点\n（入度为0）", "box", "#66BB6A"),
        (4.05, 6.0, "并行执行\n就绪节点", "box", "#FFA726"),
        (6.7, 6.0, "节点执行器\n(LLM/TOOL/...)", "box", "#FF8A65"),
        (1.35, 6.0, "SSE 推送\nnodeStart/delta/finish", "box", "#42A5F5"),
        (4.05, 4.8, "节点完成\nadvance()", "diamond", "#FFA726"),
        (4.05, 3.6, "是否有\n新就绪节点?", "diamond", "#AB47BC"),
        (4.05, 2.4, "执行完成\nstatus=SUCCEEDED", "box", "#4CAF50"),
        (1.35, 2.4, "SSE 推送\nworkflow_completed", "box", "#42A5F5"),
        (6.7, 4.8, "持久化\nMySQL 更新", "box", "#FF8A65"),
    ]

    positions = {}
    for (x, y, text, shape, color) in nodes:
        positions[(x, y)] = (text, shape)
        if shape == "box":
            box(ax, x, y, 2.3, 0.75, text, fc=color, tc="white", fontsize=8.5)
        else:
            diamond(ax, x, y, 2.3, 0.65, text, fc=color, tc="white", fontsize=8)

    # 箭头
    arrows = [
        ((1.35, 11.62), (4.05, 12.38), "触发"),
        ((4.05, 11.62), (4.05, 11.18), ""),
        ((4.05, 10.42), (4.05, 9.98), ""),
        ((5.19, 10.8), (6.07, 10.8), "查询"),
        ((4.05, 9.22), (4.05, 8.78), ""),
        ((5.19, 9.6), (6.07, 9.6), "查询"),
        ((4.05, 8.02), (4.05, 7.58), ""),
        ((4.05, 6.82), (4.05, 6.37), ""),
        ((5.19, 6.0), (6.07, 6.0), "调用"),
        ((4.05, 5.62), (4.05, 5.12), ""),
        ((5.19, 4.8), (6.07, 4.8), "持久化"),
        ((4.05, 4.47), (4.05, 3.92), ""),
        ((4.05, 3.27), (4.05, 2.78), "无就绪节点"),
        ((2.9, 6.0), (1.9, 6.0), "推流"),
        ((1.35, 5.62), (1.35, 2.78), ""),
        ((1.9, 2.4), (4.05-1.15, 2.4), ""),
    ]
    for (p1, p2, label) in arrows:
        arrow(ax, *p1, *p2, label=label, fontsize=7.5)

    # 有就绪节点循环
    ax.annotate("", xy=(4.05, 6.37), xytext=(4.05+0.8, 3.6),
                arrowprops=dict(arrowstyle="-|>", color="#AB47BC", lw=1.3,
                                connectionstyle="arc3,rad=-0.5"), zorder=4)
    ax.text(5.6, 4.8, "有节点", fontsize=7.5, color="#AB47BC")

    ax.set_title("工作流执行流程图", fontsize=13, weight="bold", pad=4)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT, "fig_exec_flow.png"))
    plt.close()
    print("✓ fig_exec_flow.png")


# ─────────────────────────────────────────────────────────────────────────────
# 5. 人工检查点审批流程图
# ─────────────────────────────────────────────────────────────────────────────

def draw_review_flow():
    fig, ax = plt.subplots(figsize=(10, 14))
    ax.set_xlim(0, 10); ax.set_ylim(0, 14)
    ax.axis("off")
    fig.patch.set_facecolor("#FAFAFA")

    # 泳道
    lane_colors = ["#E3F2FD", "#E8F5E9", "#FFF3E0", "#FCE4EC"]
    lane_titles = ["前端/审批人", "SchedulerService", "Execution 聚合根", "基础设施层"]
    xs2 = [(0, 2.5), (2.5, 5.0), (5.0, 7.5), (7.5, 10)]
    for (x0, x1), lc, lt in zip(xs2, lane_colors, lane_titles):
        rect = plt.Rectangle((x0, 0), x1-x0, 14, fc=lc, ec="#DDD", lw=1)
        ax.add_patch(rect)
        ax.text((x0+x1)/2, 13.65, lt, ha="center", va="center",
                fontsize=8.8, weight="bold", color="#444")

    def bx(x, y, text, fc="#66BB6A", tc="white"):
        box(ax, x, y, 2.2, 0.7, text, fc=fc, tc=tc, fontsize=8.2)
    def dm(x, y, text, fc="#FFA726"):
        diamond(ax, x, y, 2.2, 0.6, text, fc=fc, tc="white", fontsize=8)

    W, S, E, I = 1.25, 3.75, 6.25, 8.75  # lane centers

    bx(S, 13.1, "调度器检查\ncheckPause()", "#FB8C00")
    dm(S, 12.0, "检查点\n已启用?", "#AB47BC")
    bx(S, 11.0, "阶段/reviewedNodes\n条件检查", "#FB8C00")
    dm(S, 10.0, "条件\n全满足?", "#AB47BC")
    bx(I, 10.0, "MySQL 持久化\nRedis 队列写入", "#FF7043")
    bx(E, 10.0, "execution.advance()\n→ PAUSED_FOR_REVIEW", "#5C6BC0")
    bx(S, 8.9, "SSE 推送\nworkflow_paused", "#FB8C00")
    bx(W, 8.9, "弹出\n审批面板", "#42A5F5")
    bx(W, 7.8, "审批人查看内容\n（可选修改字段）", "#42A5F5")
    dm(W, 6.7, "通过还是\n拒绝?", "#E53935")

    # 通过分支
    bx(S, 5.6, "resumeExecution()\n乐观锁版本校验", "#FB8C00")
    dm(S, 4.5, "版本号\n匹配?", "#AB47BC")
    bx(S, 3.5, "合并 edits 到\nContext，写审批记录", "#FB8C00")
    bx(E, 3.5, "execution.resume()\n→ RUNNING\n加入reviewedNodes", "#5C6BC0")
    bx(I, 3.5, "MySQL 更新\nRedis 队列移除", "#FF7043")
    bx(S, 2.4, "SSE 推送\nworkflow_resumed", "#FB8C00")
    bx(W, 2.4, "画布恢复\n继续执行", "#42A5F5")
    bx(S, 1.3, "scheduleNodes()\n后续调度", "#FB8C00")

    # 拒绝分支
    bx(S, 6.7, "rejectExecution()\n写拒绝记录", "#EF5350")
    bx(E, 6.7, "execution.reject()\n→ FAILED", "#D32F2F")
    bx(W, 5.6, "显示拒绝原因\n执行终止", "#EF5350")

    # version 不匹配
    bx(W, 4.5, "返回 409\n请刷新重试", "#78909C")

    # 跳过
    bx(S, 9.0, "继续执行\n(无需暂停)", "#78909C")

    # 箭头
    ar = arrow
    ar(ax, S, 12.75, S, 12.3, "")
    ar(ax, S, 11.7, S, 11.35, "是")
    ax.text(S+0.15, 12.55, "是", fontsize=7.5, color="#555")
    ar(ax, S+1.1, 12.0, S+1.6, 9.3, "否", color="#999")  # 跳过
    ar(ax, S, 10.7, S, 10.3, "")
    ar(ax, S+1.1, 10.0, I-1.1, 10.0, "写入")
    ar(ax, I-1.1, 10.0, E-1.1, 10.0, "")
    ar(ax, S, 9.7, S, 9.25, "")
    ar(ax, S-1.25, 8.9, W+1.1, 8.9, "推流")
    ar(ax, W, 8.55, W, 8.15, "")
    ar(ax, W, 7.45, W, 7.0, "")
    ar(ax, W+1.1, 6.7, S-1.1, 5.95, "通过")
    ar(ax, W+1.1, 6.7, S-1.1, 6.7, "拒绝")
    ar(ax, S, 5.3, S, 4.8, "")
    ar(ax, S+1.1, 4.5, W+1.1, 4.5, "不匹配 → 409")
    ax.text(S-0.1, 4.35, "匹配", fontsize=7.5, color="#555")
    ar(ax, S, 4.2, S, 3.85, "")
    ar(ax, S+1.1, 3.5, E-1.1, 3.5, "")
    ar(ax, E+1.1, 3.5, I-1.1, 3.5, "")
    ar(ax, S, 3.15, S, 2.75, "")
    ar(ax, S-1.25, 2.4, W+1.1, 2.4, "推流")
    ar(ax, S, 2.1, S, 1.65, "")
    ar(ax, S+1.1, 6.7, E-1.1, 6.7, "")
    ar(ax, W+1.1, 6.7, W+1.1, 5.95, "")

    ax.set_title("人工检查点审批流程图", fontsize=13, weight="bold", pad=4)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT, "fig_review_flow.png"))
    plt.close()
    print("✓ fig_review_flow.png")


# ─────────────────────────────────────────────────────────────────────────────
# 6. 系统用例图
# ─────────────────────────────────────────────────────────────────────────────

def draw_usecase():
    fig, ax = plt.subplots(figsize=(13, 8))
    ax.set_xlim(0, 13); ax.set_ylim(0, 8)
    ax.axis("off")
    fig.patch.set_facecolor("#FAFAFA")

    # 系统边界
    rect_sys = FancyBboxPatch((2, 0.5), 9, 7,
                              boxstyle="round,pad=0.15",
                              facecolor="#F3F8FF", edgecolor="#1565C0",
                              linewidth=2, zorder=1)
    ax.add_patch(rect_sys)
    ax.text(6.5, 7.4, "AI Agent 智能体编排系统", ha="center", fontsize=11,
            weight="bold", color="#1565C0")

    # 角色（椭圆 + 标签）
    def actor(x, y, name, color="#333"):
        ellipse = mpatches.Ellipse((x, y+0.5), 0.4, 0.55,
                                   fc="white", ec=color, lw=1.5, zorder=3)
        ax.add_patch(ellipse)
        # 身体线
        ax.plot([x, x], [y+0.22, y-0.3], color=color, lw=1.5, zorder=3)
        ax.plot([x-0.35, x+0.35], [y+0.05, y+0.05], color=color, lw=1.5, zorder=3)
        ax.plot([x, x-0.25], [y-0.3, y-0.7], color=color, lw=1.5, zorder=3)
        ax.plot([x, x+0.25], [y-0.3, y-0.7], color=color, lw=1.5, zorder=3)
        ax.text(x, y-0.95, name, ha="center", fontsize=9, color=color, weight="bold")

    actor(0.7, 5.5, "Java\n开发者", "#1565C0")
    actor(0.7, 3.0, "业务\n人员", "#2E7D32")
    actor(0.7, 0.8, "系统\n管理员", "#6A1B9A")

    # 用例椭圆
    def usecase(x, y, text, color="#1565C0"):
        e = mpatches.Ellipse((x, y), 2.6, 0.72,
                             fc="white", ec=color, lw=1.5, zorder=3)
        ax.add_patch(e)
        ax.text(x, y, text, ha="center", va="center", fontsize=8.5,
                color=color, zorder=4)

    uc_list = [
        # developer
        (4.5, 6.8, "Agent 创建与发布", "#1565C0"),
        (7.5, 6.8, "工作流编排配置", "#1565C0"),
        (10.5, 6.8, "LLM 配置管理", "#1565C0"),
        # business
        (4.5, 4.8, "触发工作流执行", "#2E7D32"),
        (7.5, 4.8, "查看实时 SSE 推流", "#2E7D32"),
        (10.5, 4.8, "人工检查点审批", "#E53935"),
        (4.5, 3.5, "查看执行历史", "#2E7D32"),
        (7.5, 3.5, "对话交互", "#2E7D32"),
        # admin
        (4.5, 2.0, "知识库文档管理", "#6A1B9A"),
        (7.5, 2.0, "用户权限管理", "#6A1B9A"),
        (10.5, 2.0, "系统状态监控", "#6A1B9A"),
    ]
    for (x, y, text, color) in uc_list:
        usecase(x, y, text, color)

    # 角色到用例的关联线
    connections = [
        # developer → uc
        ((1.0, 5.5), (3.2, 6.8)),
        ((1.0, 5.5), (6.2, 6.8)),
        ((1.0, 5.5), (9.2, 6.8)),
        ((1.0, 5.5), (3.2, 4.8)),
        # business → uc
        ((1.0, 3.0), (3.2, 4.8)),
        ((1.0, 3.0), (6.2, 4.8)),
        ((1.0, 3.0), (9.2, 4.8)),
        ((1.0, 3.0), (3.2, 3.5)),
        ((1.0, 3.0), (6.2, 3.5)),
        # admin → uc
        ((1.0, 0.8), (3.2, 2.0)),
        ((1.0, 0.8), (6.2, 2.0)),
        ((1.0, 0.8), (9.2, 2.0)),
        ((1.0, 0.8), (3.2, 3.5)),
    ]
    for (p1, p2) in connections:
        ax.plot([p1[0], p2[0]], [p1[1], p2[1]], "-", color="#BBBBBB",
                lw=1.0, zorder=2)

    ax.set_title("系统用例图", fontsize=13, weight="bold", pad=6)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT, "fig_usecase.png"))
    plt.close()
    print("✓ fig_usecase.png")


if __name__ == "__main__":
    print("开始绘制论文流程图...")
    draw_arch()
    draw_func()
    draw_state()
    draw_exec_flow()
    draw_review_flow()
    draw_usecase()
    print("\n全部完成！图片保存在 figures/ 目录")
