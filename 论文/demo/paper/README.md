# 西南科技大学本科毕业论文 LaTeX 模板

> **柏彪 / 5120223930 / 大数据2203 / 计算机科学与技术学院**
> **题目：虚拟问诊：医学AI模拟诊疗系统设计与实现**

## 一、目录结构

```
paper/
├── main.tex                       主文档（编译入口）
├── swust-thesis.sty               西科大格式宏包
├── refs.bib                       参考文献库（GB/T 7714-2015）
├── README.md                      本文件
├── chapters/                      各章节源文件
│   ├── 01_abstract_zh.tex         中文摘要
│   ├── 02_abstract_en.tex         英文摘要
│   ├── 04_chapter1_introduction.tex      第 1 章 绪论
│   ├── 05_chapter2_technology.tex        第 2 章 相关技术概述
│   ├── 06_chapter3_requirement.tex       第 3 章 系统需求分析
│   ├── 07_chapter4_design.tex            第 4 章 系统总体设计
│   ├── 08_chapter5_agent.tex             第 5 章 AI Agent 核心技术（核心创新）
│   ├── 09_chapter6_module.tex            第 6 章 系统功能模块详细实现
│   ├── 10_chapter7_test_deploy.tex       第 7 章 系统测试与部署
│   ├── 11_chapter8_conclusion.tex        第 8 章 总结与展望
│   ├── 12_acknowledgement.tex     致谢
│   └── 13_appendix.tex             附录
└── figures/                        图片目录
```

## 二、编译环境要求

### 必需软件
- **TeX 发行版**：TeX Live 2022+ 或 MikTeX 最新版（推荐 TeX Live 2024）
- **编译引擎**：XeLaTeX
- **参考文献处理**：Biber（不是 BibTeX！）

### 必需中文字体（Windows 自带）
- 宋体：`SimSun`
- 黑体：`SimHei`
- Times New Roman（系统自带）

### 关键宏包
> 大部分通过 TeX 发行版默认安装，缺包时 MikTeX 会弹窗提示自动下载，TeX Live 通过 `tlmgr install` 安装。

| 宏包 | 用途 |
| ---- | ---- |
| `ctex` | 中文支持（`ctexbook` 文档类） |
| `geometry` | 页面尺寸 |
| `fancyhdr` | 页眉页脚 |
| `titlesec` / `titletoc` | 章节标题 / 目录样式 |
| `caption` | 图表标题 |
| `biblatex` + `gb7714-2015` | 国标参考文献 |
| `hyperref` | 超链接 |
| `listings` | 代码块 |
| `setspace` / `indentfirst` | 行距 / 首段缩进 |
| `booktabs` / `longtable` / `tabularx` / `multirow` | 表格 |

## 三、编译命令

### Windows PowerShell（推荐，全自动）

```powershell
cd e:\ClaudeCode\论文-柏彪\ai-medical-training-v3\paper
xelatex main.tex
biber main
xelatex main.tex
xelatex main.tex
```

> 三次 `xelatex` 是为了让目录与交叉引用正确生成。

### 用 latexmk 一键编译

```powershell
latexmk -xelatex -bibtex- main.tex
```

> `-bibtex-` 表示禁用 bibtex（因为我们用 biber）。latexmk 会自动检测引用变更并重新编译。

### VS Code + LaTeX Workshop

在 `.vscode/settings.json` 中配置：

```json
{
  "latex-workshop.latex.tools": [
    {
      "name": "xelatex",
      "command": "xelatex",
      "args": ["-synctex=1", "-interaction=nonstopmode", "-file-line-error", "%DOC%"]
    },
    {
      "name": "biber",
      "command": "biber",
      "args": ["%DOCFILE%"]
    }
  ],
  "latex-workshop.latex.recipes": [
    {
      "name": "xe → biber → xe → xe",
      "tools": ["xelatex", "biber", "xelatex", "xelatex"]
    }
  ],
  "latex-workshop.latex.recipe.default": "xe → biber → xe → xe"
}
```

## 四、撰写约定

### 4.1 各章节占位说明
- 每个 `.tex` 文件顶部有 **"写作要点"** 注释，说明该节字数与重点
- 标题与小节序号已按西科大格式与论文结构布好
- 待写部分用 `TODO：……` 标识
- 关键位置已预留图表占位 `\begin{figure}[htbp]…\end{figure}`，将真实图片放入 `figures/` 后取消 `\fbox{...}` 替换为 `\includegraphics`

### 4.2 引用规则
- 正文中：`\cite{key}` → 自动生成上标 `[1]`
- **首次引用** 同一文献时：手动加脚注 `\footnote{真实出处描述}`（按用户要求"必须真实来源 + 加脚注"）
- 末尾参考文献由 `\printbibliography` 自动按 GB/T 7714-2015 生成
- **所有 BibTeX 条目必须真实可查证**：
  - 期刊文章：必有 DOI
  - 会议文章：必有会议名 + 年份 + 页码或 arXiv 编号
  - 网络资源：必有 `url` + `note={访问时间：YYYY-MM-DD}`

### 4.3 图表插入
- 图：`figures/xxx.png`（推荐 PNG 300dpi 或 PDF 矢量）
- 图标题：`\caption{...}` 在 `\includegraphics` 之后（图在下）
- 表标题：`\caption{...}` 在 `\begin{tabular}` 之前（表在上）
- 标号格式自动按章节生成：图 1-1 / 表 2-1

### 4.4 代码块
- 使用 `\begin{lstlisting}[style=java, caption={…}, label={lst:…}]`
- 支持的 style：`java` / `ts` / `yaml` / `sql`
- 引用：`\ref{lst:agent_router}`

### 4.5 公式
- 单行公式：`\begin{equation}…\end{equation}` → 自动按章编号
- 引用：`\eqref{eq:xxx}`

## 五、与用户的撰写流程约定

> 模板交付后，按以下顺序逐章撰写。**每章撰写完毕后，柏同学审阅通过，再写下一章**。

| 顺序 | 内容 | 文件 |
| ---- | ---- | ---- |
| 1 | 中英文摘要 | `01_abstract_zh.tex` / `02_abstract_en.tex` |
| 2 | 第 1 章 绪论 | `04_chapter1_introduction.tex` |
| 3 | 第 2 章 相关技术概述 | `05_chapter2_technology.tex` |
| 4 | 第 3 章 系统需求分析 | `06_chapter3_requirement.tex` |
| 5 | 第 4 章 系统总体设计 | `07_chapter4_design.tex` |
| 6 | **第 5 章 AI Agent 核心技术（创新章）** | `08_chapter5_agent.tex` |
| 7 | 第 6 章 系统功能模块详细实现 | `09_chapter6_module.tex` |
| 8 | 第 7 章 系统测试与部署 | `10_chapter7_test_deploy.tex` |
| 9 | 第 8 章 总结与展望 | `11_chapter8_conclusion.tex` |
| 10 | 致谢 + 参考文献最终化 + 附录 + 整体校对 | `12_acknowledgement.tex` / `refs.bib` |

## 六、常见问题

### Q1：`xelatex` 报错 `Font "SimSun" not found`
- Windows：检查"控制面板 → 字体"是否有宋体（默认存在），重启 IDE
- Linux / macOS：需手动安装中文字体包

### Q2：`biber` 报错 `Cannot find 'main.bcf'`
- 必须先跑一次 `xelatex main.tex` 生成 `.bcf`，再跑 `biber main`

### Q3：参考文献中文人名末尾出现"等"变成英文逗号
- `gb7714-2015` 风格在某些 biblatex 版本下有此 bug，可改用 `gb7714-2015ay` 或升级 biblatex 至 3.19+

### Q4：目录页码显示不对
- 目录依赖 `.toc` 文件，需要至少 **两次 xelatex** 编译

### Q5：编译警告 `Underfull \hbox`
- 这是排版调整警告，不影响 PDF 输出，可忽略

## 七、版本信息

- 模板版本：v1.0
- 创建日期：2026-04-26
- 维护者：柏彪
