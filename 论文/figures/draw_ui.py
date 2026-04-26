import asyncio
from playwright.async_api import async_playwright
import os

OUT_DIR = "/home/zj669/repo/ai-agent/论文/figures/"
os.makedirs(OUT_DIR, exist_ok=True)

# ── 图1: 工作流执行界面 ──
html_wf_exec = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f8fafc; }
  .node { border: 2px solid #e2e8f0; border-radius: 8px; padding: 12px; width: 180px; background: white; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
  .node-success { border-color: #22c55e; border-left: 6px solid #22c55e; }
  .node-running { border-color: #3b82f6; border-left: 6px solid #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.3); }
  .node-pending { border-color: #cbd5e1; border-left: 6px solid #cbd5e1; opacity: 0.8; }
  .node-skipped { border-color: #94a3b8; border-left: 6px solid #94a3b8; opacity: 0.6; background: #f1f5f9; }
  .line { position: absolute; height: 2px; background: #cbd5e1; z-index: -1; }
  .bg-grid { background-size: 20px 20px; background-image: radial-gradient(circle, #cbd5e1 1px, rgba(0,0,0,0) 1px); }
</style>
</head>
<body class="w-[1200px] h-[800px] flex overflow-hidden">
  <!-- 左侧：功能区 -->
  <div class="w-16 bg-white border-r border-slate-200 flex flex-col items-center py-4 space-y-6 text-slate-400">
    <div class="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center text-white font-bold">A</div>
    <div class="w-8 h-8 rounded bg-slate-100 flex items-center justify-center"><svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20"><path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"></path></svg></div>
    <div class="w-8 h-8 rounded bg-blue-50 text-blue-500 flex items-center justify-center"><svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg></div>
  </div>
  
  <!-- 中间：画布 -->
  <div class="flex-1 bg-grid relative p-8">
    <div class="absolute top-4 left-4 flex space-x-2">
      <button class="bg-white border text-sm px-3 py-1 rounded shadow-sm text-slate-600 font-medium tracking-wide">执行详情: workflow-ae89-4d2b...</button>
      <span class="bg-blue-100 text-blue-700 text-xs px-2 py-1.5 rounded font-bold">RUNNING</span>
    </div>
    
    <!-- Nodes -->
    <div class="node node-success absolute top-[100px] left-[50px]">
      <div class="flex justify-between items-center mb-2"><span class="text-xs font-bold text-slate-500">START</span><span class="text-xs text-green-500">✓ 21ms</span></div>
      <div class="font-bold text-slate-800 text-sm">开始节点</div>
    </div>
    
    <div class="line" style="top: 135px; left: 230px; width: 60px;"></div>
    
    <div class="node node-success absolute top-[100px] left-[290px]">
      <div class="flex justify-between items-center mb-2"><span class="text-xs font-bold text-slate-500">LLM</span><span class="text-xs text-green-500">✓ 2.3s</span></div>
      <div class="font-bold text-slate-800 text-sm">意图识别大模型</div>
    </div>
    
    <!-- 分支线 -->
    <div class="line" style="top: 135px; left: 470px; width: 40px;"></div>
    <div class="line" style="top: 135px; left: 510px; width: 2px; height: 160px;"></div>
    <div class="line" style="top: 135px; left: 510px; width: 40px;"></div>
    <div class="line" style="top: 295px; left: 510px; width: 40px;"></div>
    
    <div class="node node-skipped absolute top-[100px] left-[550px]">
      <div class="flex justify-between items-center mb-2"><span class="text-xs font-bold text-slate-500">TOOL</span><span class="text-xs text-slate-400">SKIPPED</span></div>
      <div class="font-bold text-slate-500 text-sm">发送内部邮件</div>
    </div>
    
    <div class="node node-running absolute top-[260px] left-[550px]">
      <div class="flex justify-between items-center mb-2"><span class="text-xs font-bold text-slate-500">KNOWLEDGE</span><span class="w-3 h-3 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></span></div>
      <div class="font-bold text-slate-800 text-sm">检索 Milvus 知识库</div>
      <div class="mt-2 text-xs text-slate-500 bg-slate-50 p-1 rounded font-mono truncate">query: "产品退款政策"</div>
    </div>
    
    <div class="line" style="top: 295px; left: 730px; width: 60px;"></div>
    
    <div class="node node-pending absolute top-[260px] left-[790px]">
      <div class="flex justify-between items-center mb-2"><span class="text-xs font-bold text-slate-500">LLM</span></div>
      <div class="font-bold text-slate-800 text-sm">生成回复 (RAG)</div>
    </div>
  </div>
  
  <!-- 右侧：SSE 控制台日志 -->
  <div class="w-[380px] bg-white border-l border-slate-200 flex flex-col font-mono text-sm shadow-xl z-10">
    <div class="h-12 border-b border-slate-200 flex items-center px-4 justify-between bg-slate-50">
      <span class="font-bold text-slate-700">SSE 执行日志 (Live)</span>
      <span class="flex h-3 w-3"><span class="animate-ping absolute inline-flex h-3 w-3 rounded-full bg-green-400 opacity-75"></span><span class="relative inline-flex rounded-full h-3 w-3 bg-green-500"></span></span>
    </div>
    <div class="p-4 flex-1 overflow-y-auto space-y-3 bg-slate-900 text-slate-300">
      <div class="text-xs text-blue-400">[14:22:05.102] [SSE] connected</div>
      <div class="text-xs text-green-400">[14:22:05.150] [EXEC] Memory Hydration Complete (LTM+STM)</div>
      <div class="text-xs">[14:22:05.201] [NODE:start] status=RUNNING</div>
      <div class="text-xs">[14:22:05.222] [NODE:start] status=SUCCEEDED</div>
      <div class="text-xs text-slate-500">  └─ scheduler: advancing Execution...</div>
      <div class="text-xs">[14:22:05.253] [NODE:intent_llm] status=RUNNING</div>
      <div class="text-xs text-purple-300">  └─ stream_delta: "用"... "户"... "想"... "问"... "退"... "款"...</div>
      <div class="text-xs text-purple-300">  └─ intent_result: {"intent": "REFUND_QUERY"}</div>
      <div class="text-xs">[14:22:07.513] [NODE:intent_llm] status=SUCCEEDED</div>
      <div class="text-xs text-yellow-500">  └─ scheduler: Pruned unselected branch -> [send_email_tool] SKIPPED</div>
      <div class="text-xs text-blue-300">[14:22:07.550] [NODE:knowledge_retrieval] status=RUNNING</div>
      <div class="flex items-center space-x-2 mt-2">
         <div class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce"></div>
         <div class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
         <div class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
      </div>
    </div>
  </div>
</body>
</html>
"""

# ── 图2: 思维链日志界面 ──
html_wf_log = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f1f5f9; }
</style>
</head>
<body class="w-[1000px] h-[700px] overflow-hidden p-8 flex flex-col items-center">
  <div class="w-full max-w-4xl bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col h-full">
    <div class="h-14 border-b border-slate-200 px-6 flex items-center justify-between">
      <div class="font-bold text-lg text-slate-800">思维链执行日志 (Chain of Thought)</div>
      <button class="text-sm text-blue-600 bg-blue-50 px-3 py-1.5 rounded-md font-medium">展开全部</button>
    </div>
    
    <div class="flex-1 overflow-y-auto p-6 space-y-6">
      <!-- 记录1 -->
      <div class="flex">
        <div class="flex flex-col items-center mr-4">
          <div class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center text-green-600"><svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg></div>
          <div class="w-0.5 h-full bg-slate-200 my-2"></div>
        </div>
        <div class="flex-1 pb-6">
          <div class="flex justify-between items-center mb-2">
            <h3 class="text-base font-bold text-slate-800">1. 意图识别网络 (LLM)</h3>
            <span class="text-xs text-slate-500">耗时: 1.28s</span>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="border rounded bg-slate-50 border-slate-200">
              <div class="text-xs font-bold text-slate-500 p-2 border-b border-slate-200 bg-slate-100">INPUT</div>
              <pre class="p-3 text-xs font-mono text-slate-700 overflow-x-auto">{"query": "我的订单 2024041901 什么时候能发货？"}</pre>
            </div>
            <div class="border rounded bg-slate-50 border-slate-200">
              <div class="text-xs font-bold text-slate-500 p-2 border-b border-slate-200 bg-slate-100">OUTPUT</div>
              <pre class="p-3 text-xs font-mono text-slate-700 overflow-x-auto">{
  "intent": "LOGISTICS_INQUIRY",
  "entities": { "orderId": "2024041901" },
  "confidence": 0.98
}</pre>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 记录2 -->
      <div class="flex">
        <div class="flex flex-col items-center mr-4">
          <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg></div>
          <div class="w-0.5 h-full bg-slate-200 my-2"></div>
        </div>
        <div class="flex-1 pb-6">
          <div class="flex justify-between items-center mb-2">
            <h3 class="text-base font-bold text-slate-800">2. 订单信息检索 (TOOL)</h3>
            <span class="text-xs text-slate-500">耗时: 0.45s</span>
          </div>
          <div class="border rounded bg-slate-50 border-slate-200">
            <div class="text-xs font-bold text-slate-500 p-2 border-b border-slate-200 bg-slate-100 items-center flex justify-between">
              <span>TOOL CALL: QueryOrderSystem</span>
              <span class="text-green-600">Success</span>
            </div>
            <pre class="p-3 text-xs font-mono text-slate-700 overflow-x-auto">Method: GET /api/orders/2024041901
Params: {}

Response: 
{
  "status": "PACKAGING",
  "estimatedShipDate": "2024-04-20 18:00:00",
  "warehouse": "Shanghai WH-01"
}</pre>
          </div>
        </div>
      </div>
      
      <!-- 记录3 -->
      <div class="flex">
        <div class="flex flex-col items-center mr-4">
          <div class="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-400"><svg class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg></div>
        </div>
        <div class="flex-1">
          <div class="flex justify-between items-center mb-2">
            <h3 class="text-base font-bold text-slate-800">3. 最终回复生成 (LLM)</h3>
            <span class="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded-md font-bold">RUNNING</span>
          </div>
          <div class="border rounded bg-slate-50 border-slate-200 p-4">
            <div class="h-4 bg-slate-200 rounded w-3/4 mb-2 animate-pulse"></div>
            <div class="h-4 bg-slate-200 rounded w-1/2 animate-pulse"></div>
          </div>
        </div>
      </div>
      
    </div>
  </div>
</body>
</html>
"""

# ── 图3: 人工检查点审批面板 ──
html_review_panel = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: rgba(15, 23, 42, 0.4); display: flex; align-items: center; justify-content: center; }
</style>
</head>
<body class="w-[1000px] h-[750px] overflow-hidden p-8">
  <div class="w-[900px] bg-white rounded-xl shadow-2xl overflow-hidden flex flex-col h-[650px] mx-auto mt-[10px]">
    <!-- Header -->
    <div class="h-16 px-6 border-b border-slate-200 flex items-center justify-between bg-slate-50">
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center"><svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg></div>
        <div>
          <h2 class="text-lg font-bold text-slate-800">人工检查点审批 (Human Review)</h2>
          <div class="text-xs text-slate-500">执行追踪 ID: exec_8f4a1_22c · 触发阶段: AFTER_EXECUTION</div>
        </div>
      </div>
      <button class="text-slate-400 hover:text-slate-600"><svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg></button>
    </div>
    
    <!-- Content Split View -->
    <div class="flex-1 flex overflow-hidden">
      <!-- Left: JSON Editor -->
      <div class="w-3/5 border-r border-slate-200 flex flex-col">
        <div class="px-4 py-2 bg-slate-100 border-b border-slate-200 text-sm font-bold text-slate-600 flex justify-between">
          <span>节点输出内容 (Node Outputs) - [邮件生成 LLM]</span>
          <span class="text-xs font-normal bg-orange-100 text-orange-700 px-2 rounded-full hidden">Modified</span>
        </div>
        <div class="flex-1 bg-[#1e1e1e] p-4 text-xs font-mono text-[#d4d4d4] overflow-y-auto">
<span class="text-[#569cd6]">const</span> data = {
  <span class="text-[#9cdcfe]">"recipient"</span>: <span class="text-[#ce9178]">"ceo@company.com"</span>,
  <span class="text-[#9cdcfe]">"subject"</span>: <span class="text-[#ce9178]">"[Urgent] 季度营收异常报告"</span>,
  <span class="text-[#9cdcfe]">"body"</span>: <span class="text-[#ce9178]">"根据最新分析，本季度营收下降了 15%。由于营销预算削减和竞品发力导致的系统性风险正在加大..."</span>,
  <span class="text-[#9cdcfe]">"attachments"</span>: [
    <span class="text-[#ce9178]">"q3_report.pdf"</span>
  ]
};
        </div>
      </div>
      
      <!-- Right: Form -->
      <div class="w-2/5 bg-white p-6 flex flex-col space-y-6">
        <div>
          <h3 class="text-sm font-bold text-slate-700 mb-2">安全风险警告</h3>
          <div class="bg-red-50 text-red-700 p-3 rounded-lg text-sm border border-red-100 border-l-4 border-l-red-500">
            该节点涉及发送外部邮件或高敏感数据。在大模型生成内容不受限的情况下，请您务必人工核对上述参数（如收件人、邮件正文）是否安全合规！
          </div>
        </div>
        
        <div>
          <h3 class="text-sm font-bold text-slate-700 mb-2">审批意见 (可选)</h3>
          <textarea class="w-full h-24 border border-slate-300 rounded-lg p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder-slate-400" placeholder="如果您发现内容有误，可在左侧直接修改。如果需要拒绝操作，请在此填写拒绝原因..."></textarea>
        </div>
        
        <div class="mt-auto pt-6 flex space-x-3 border-t border-slate-100">
          <button class="flex-1 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-700 font-bold hover:bg-slate-50">拒绝 (Reject)</button>
          <button class="flex-1 py-2.5 rounded-lg bg-blue-600 text-white font-bold hover:bg-blue-700 shadow-md">审批通过 (Approve)</button>
        </div>
      </div>
    </div>
  </div>
</body>
</html>
"""

# ── 图4: 恢复执行UI ──
html_resume_ui = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f8fafc; }
  .node { border: 2px solid #e2e8f0; border-radius: 8px; padding: 12px; width: 170px; background: white; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
  .node-success { border-color: #22c55e; border-left: 6px solid #22c55e; }
  .node-running { border-color: #3b82f6; border-left: 6px solid #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.3); }
  .line { position: absolute; height: 2px; background: #22c55e; z-index: -1; }
  .line-active { position: absolute; height: 2px; background: #3b82f6; z-index: -1; box-shadow: 0 0 8px #3b82f6; }
</style>
</head>
<body class="w-[800px] h-[500px] flex overflow-hidden p-8 justify-center items-center">
  <div class="relative w-[700px] h-[400px] bg-white border border-slate-200 rounded-xl shadow p-6">
    <!-- Toast Message -->
    <div class="absolute top-4 right-4 bg-green-50 border border-green-200 text-green-700 px-4 py-2 rounded-lg shadow-sm flex items-center mb-4 transform translate-y-0 transition-all duration-300">
      <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
      <span class="font-bold text-sm">审批通过，工作流已恢复！</span>
    </div>
    
    <div class="mt-8 relative h-[300px]">
      <div class="node node-success position-absolute" style="top: 100px; left: 50px;">
        <div class="flex justify-between font-bold text-xs"><span class="text-slate-500">LLM (Review)</span><span class="text-green-500">✓</span></div>
        <div class="text-slate-800 text-sm mt-1">敏感邮件生成</div>
        <div class="flex items-center space-x-1 mt-2 bg-orange-50 text-orange-700 text-[10px] px-1 py-0.5 rounded w-fit border border-orange-200">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
          <span>人工修改通过</span>
        </div>
      </div>
      
      <div class="line-active" style="top: 135px; left: 220px; width: 80px;"></div>
      
      <div class="node node-running position-absolute" style="top: 100px; left: 300px;">
        <div class="flex justify-between font-bold text-xs"><span class="text-slate-500">TOOL</span><span class="w-3 h-3 animate-spin border-2 border-blue-500 border-t-transparent rounded-full"></span></div>
        <div class="text-slate-800 text-sm mt-1">调用邮件发送接口</div>
      </div>
      
      <div class="line" style="top: 135px; left: 470px; width: 60px; background: #cbd5e1;"></div>
      
      <div class="node position-absolute" style="top: 100px; left: 530px; border-color: #cbd5e1; border-left: 6px solid #cbd5e1;">
        <div class="flex justify-between font-bold text-xs"><span class="text-slate-500">END</span></div>
        <div class="text-slate-800 text-sm mt-1">流程结束</div>
      </div>
    </div>
  </div>
</body>
</html>
"""

# ── 图5: SSE前端效果UI ──
html_sse_ui = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f8fafc; }
</style>
</head>
<body class="w-[900px] h-[550px] p-8 flex justify-center items-center">
  <div class="w-full h-full bg-white rounded-2xl shadow-xl flex flex-col border border-slate-200 overflow-hidden">
    <div class="h-14 bg-gradient-to-r from-blue-600 to-indigo-600 px-6 flex items-center justify-between text-white">
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-white/20 rounded flex items-center justify-center"><svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg></div>
        <span class="font-bold text-lg">AI Agent 智能助手</span>
      </div>
      <div class="flex items-center space-x-2 text-xs font-medium bg-blue-800/40 px-3 py-1.5 rounded-full border border-blue-400/30">
        <span class="relative flex h-2.5 w-2.5"><span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span><span class="relative inline-flex rounded-full h-2.5 w-2.5 bg-green-500"></span></span>
        <span>SSE 连接正常</span>
      </div>
    </div>
    
    <div class="flex-1 p-6 overflow-y-auto space-y-6">
      <!-- 问 -->
      <div class="flex justify-end">
        <div class="bg-blue-600 text-white p-4 rounded-2xl rounded-tr-none max-w-lg shadow-sm text-sm">
          帮我基于上一季度的销售数据写一份英文简报邮件。
        </div>
      </div>
      
      <!-- 答 -->
      <div class="flex">
        <div class="w-10 h-10 rounded-full bg-gradient-to-b from-blue-500 to-indigo-600 mr-4 flex-shrink-0 flex items-center justify-center shadow">
           <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"></path></svg>
        </div>
        <div class="flex-1">
          <!-- Chain of thought / Exec log via SSE -->
          <div class="bg-slate-50 border border-slate-200 p-3 rounded-xl mb-3 text-xs w-full max-w-lg">
            <div class="flex items-center text-slate-500 mb-1">
              <svg class="w-4 h-4 mr-1 animate-spin" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
              <span class="font-bold">执行中: KNOWLEDGE</span>
            </div>
            <div class="text-slate-400 font-mono flex items-center pl-5 border-l-2 border-slate-300 ml-[7px]">
              <span class="mr-2">✓</span><span>已完成：DB_Query (TOOL) 1.2s</span>
            </div>
            <div class="text-blue-500 font-mono pl-5 border-l-2 border-slate-300 ml-[7px] mt-1 relative">
              <span class="absolute left-[-5px] top-[5px] w-2 h-2 bg-blue-500 rounded-full"></span>
              正在检索 Milvus 知识库，获取销售上下文...
            </div>
          </div>
          
          <!-- Delta output -->
          <div class="text-slate-800 text-sm leading-relaxed prose prose-sm max-w-2xl bg-slate-50 p-4 rounded-xl rounded-tl-none border border-slate-200">
            <p>Based on the Q1 sales data retrieved, here is the draft for the brief report:</p>
            <p class="mt-2 font-semibold">Subject: Q1 Sales Performance Summary</p>
            <p class="mt-2">Hi Team,</p>
            <p class="mt-2">I am pleased to share the highlights of our Q1 performance. We achieved a <span class="bg-yellow-100 placeholder-pulse relative border-r-2 border-blue-500">total r</span></p>
          </div>
        </div>
      </div>
      
    </div>
  </div>
</body>
</html>
"""

# ── 图6: Agent 可视化编排界面 ──
html_agent_editor = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f8fafc; }
  .panel { background: white; border: 1px solid #e2e8f0; border-radius: 14px; box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08); }
  .node { position: absolute; width: 180px; border-radius: 10px; padding: 12px; background: white; border: 2px solid #cbd5e1; box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08); }
  .line { position: absolute; height: 3px; background: #94a3b8; }
  .dashed { border-top: 3px dashed #94a3b8; height: 0; background: transparent; }
  .grid { background-size: 24px 24px; background-image: radial-gradient(circle, #dbeafe 1px, rgba(0,0,0,0) 1px); }
</style>
</head>
<body class="w-[1360px] h-[840px] overflow-hidden">
  <div class="w-full h-full flex">
    <aside class="w-[260px] bg-slate-900 text-slate-100 p-5 flex flex-col">
      <div class="text-xl font-bold mb-5">Agent Studio</div>
      <div class="text-xs uppercase tracking-[0.2em] text-slate-400 mb-3">节点库</div>
      <div class="space-y-3 text-sm">
        <div class="bg-slate-800 rounded-xl px-4 py-3 border border-slate-700 flex justify-between"><span>开始节点</span><span class="text-emerald-400">START</span></div>
        <div class="bg-slate-800 rounded-xl px-4 py-3 border border-slate-700 flex justify-between"><span>大模型节点</span><span class="text-blue-400">LLM</span></div>
        <div class="bg-slate-800 rounded-xl px-4 py-3 border border-slate-700 flex justify-between"><span>知识检索节点</span><span class="text-violet-400">KNOWLEDGE</span></div>
        <div class="bg-slate-800 rounded-xl px-4 py-3 border border-slate-700 flex justify-between"><span>条件分支节点</span><span class="text-amber-400">CONDITION</span></div>
        <div class="bg-slate-800 rounded-xl px-4 py-3 border border-slate-700 flex justify-between"><span>工具调用节点</span><span class="text-cyan-400">TOOL</span></div>
        <div class="bg-slate-800 rounded-xl px-4 py-3 border border-slate-700 flex justify-between"><span>结束节点</span><span class="text-rose-400">END</span></div>
      </div>
      <div class="mt-auto panel bg-slate-800/80 border-slate-700 p-4 text-sm text-slate-300">
        <div class="font-semibold text-white mb-2">编排说明</div>
        <p>左侧拖拽节点到画布，中间完成连线，右侧配置节点参数并保存为 graphJson 草稿。</p>
      </div>
    </aside>

    <main class="flex-1 flex flex-col bg-slate-100">
      <header class="h-16 px-6 flex items-center justify-between bg-white border-b border-slate-200">
        <div>
          <div class="text-lg font-bold text-slate-800">售后工单智能处理 Agent</div>
          <div class="text-xs text-slate-500 mt-1">当前版本：草稿 v7 · 最近保存：2026-04-24 20:58</div>
        </div>
        <div class="flex items-center gap-3 text-sm">
          <span class="px-3 py-1 rounded-full bg-amber-100 text-amber-700 font-semibold">Draft</span>
          <button class="px-4 py-2 rounded-lg border border-slate-300 bg-white text-slate-700">预览</button>
          <button class="px-4 py-2 rounded-lg bg-blue-600 text-white font-semibold">保存编排</button>
        </div>
      </header>

      <div class="flex-1 flex">
        <section class="flex-1 relative grid p-6">
          <div class="panel grid relative w-full h-full overflow-hidden">
            <div class="absolute inset-0 grid"></div>
            <div class="absolute top-4 left-4 flex gap-2 text-xs">
              <span class="px-2 py-1 bg-white rounded border border-slate-200">缩放 85%</span>
              <span class="px-2 py-1 bg-white rounded border border-slate-200">自动布局</span>
              <span class="px-2 py-1 bg-white rounded border border-slate-200">对齐网格</span>
            </div>

            <div class="node" style="top: 120px; left: 60px; border-color: #10b981;">
              <div class="text-xs font-bold text-emerald-600">START</div>
              <div class="mt-2 font-semibold text-slate-800">开始节点</div>
              <div class="text-xs text-slate-500 mt-2">接收用户问题与上下文</div>
            </div>
            <div class="line" style="top: 170px; left: 240px; width: 80px;"></div>

            <div class="node" style="top: 120px; left: 320px; border-color: #3b82f6;">
              <div class="text-xs font-bold text-blue-600">LLM</div>
              <div class="mt-2 font-semibold text-slate-800">意图识别</div>
              <div class="text-xs text-slate-500 mt-2">判断是否命中售后退款问题</div>
            </div>
            <div class="line" style="top: 170px; left: 500px; width: 90px;"></div>
            <div class="line" style="top: 170px; left: 590px; width: 3px; height: 180px;"></div>
            <div class="line" style="top: 350px; left: 590px; width: 90px;"></div>

            <div class="node" style="top: 120px; left: 680px; border-color: #f59e0b;">
              <div class="text-xs font-bold text-amber-600">CONDITION</div>
              <div class="mt-2 font-semibold text-slate-800">条件分支</div>
              <div class="text-xs text-slate-500 mt-2">高风险问题进入人工审核</div>
            </div>
            <div class="dashed line" style="top: 170px; left: 860px; width: 100px;"></div>
            <div class="line" style="top: 350px; left: 860px; width: 100px;"></div>

            <div class="node" style="top: 120px; left: 970px; border-color: #06b6d4;">
              <div class="text-xs font-bold text-cyan-600">TOOL</div>
              <div class="mt-2 font-semibold text-slate-800">人工审核通知</div>
              <div class="text-xs text-slate-500 mt-2">推送待审核任务到 Review 面板</div>
            </div>

            <div class="node" style="top: 300px; left: 680px; border-color: #8b5cf6;">
              <div class="text-xs font-bold text-violet-600">KNOWLEDGE</div>
              <div class="mt-2 font-semibold text-slate-800">知识库检索</div>
              <div class="text-xs text-slate-500 mt-2">Milvus 检索退款政策与 FAQ</div>
            </div>
            <div class="line" style="top: 350px; left: 860px; width: 100px;"></div>

            <div class="node" style="top: 300px; left: 970px; border-color: #3b82f6;">
              <div class="text-xs font-bold text-blue-600">LLM</div>
              <div class="mt-2 font-semibold text-slate-800">生成最终回复</div>
              <div class="text-xs text-slate-500 mt-2">结合知识召回结果生成回答</div>
            </div>
            <div class="line" style="top: 350px; left: 1150px; width: 70px;"></div>

            <div class="node" style="top: 300px; left: 1220px; width: 120px; border-color: #ef4444;">
              <div class="text-xs font-bold text-rose-600">END</div>
              <div class="mt-2 font-semibold text-slate-800">结束</div>
              <div class="text-xs text-slate-500 mt-2">输出执行结果</div>
            </div>
          </div>
        </section>

        <aside class="w-[320px] p-6 bg-white border-l border-slate-200">
          <div class="text-lg font-bold text-slate-800 mb-4">节点配置</div>
          <div class="panel p-4 mb-4">
            <div class="text-xs text-slate-500 mb-2">当前选中节点</div>
            <div class="font-semibold text-slate-800">知识库检索</div>
            <div class="text-sm text-slate-500 mt-1">nodeId: knowledge_refund_policy</div>
          </div>
          <div class="space-y-4 text-sm">
            <div>
              <label class="block text-slate-600 mb-1">数据集</label>
              <div class="px-3 py-2 rounded-lg border border-slate-300 bg-slate-50">refund-policy-dataset</div>
            </div>
            <div>
              <label class="block text-slate-600 mb-1">召回条数 TopK</label>
              <div class="px-3 py-2 rounded-lg border border-slate-300 bg-slate-50">5</div>
            </div>
            <div>
              <label class="block text-slate-600 mb-1">查询模板</label>
              <div class="px-3 py-2 rounded-lg border border-slate-300 bg-slate-50 h-24">请基于用户问题与订单上下文检索退款政策、售后流程与异常处理说明。</div>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>
</body>
</html>
"""

# ── 图7: Agent 列表与版本管理界面 ──
html_agent_list = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f8fafc; }
  .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06); }
</style>
</head>
<body class="w-[1280px] h-[820px] p-8 overflow-hidden bg-slate-50">
  <div class="h-full flex flex-col gap-6">
    <div class="flex items-center justify-between">
      <div>
        <div class="text-3xl font-bold text-slate-800">Agent 管理中心</div>
        <div class="text-slate-500 mt-2">统一管理智能体草稿、正式版本与历史回滚记录</div>
      </div>
      <div class="flex gap-3">
        <button class="px-4 py-2 rounded-xl bg-white border border-slate-300 text-slate-700">导出清单</button>
        <button class="px-4 py-2 rounded-xl bg-blue-600 text-white font-semibold">新建 Agent</button>
      </div>
    </div>

    <div class="grid grid-cols-[2fr_1fr] gap-6 flex-1">
      <section class="card p-6 flex flex-col">
        <div class="flex items-center justify-between mb-5">
          <div class="text-lg font-bold text-slate-800">我的 Agent 列表</div>
          <div class="flex gap-2 text-sm">
            <span class="px-3 py-1 rounded-full bg-emerald-100 text-emerald-700">已发布 12</span>
            <span class="px-3 py-1 rounded-full bg-amber-100 text-amber-700">草稿 4</span>
          </div>
        </div>
        <div class="grid gap-4">
          <div class="border border-slate-200 rounded-2xl p-5 bg-slate-50 flex justify-between items-start">
            <div>
              <div class="flex items-center gap-3">
                <div class="text-lg font-semibold text-slate-800">售后工单智能处理 Agent</div>
                <span class="px-2 py-1 rounded-full text-xs bg-blue-100 text-blue-700">当前版本 v7</span>
              </div>
              <div class="text-sm text-slate-500 mt-2">适用于退款、换货、物流异常等场景，支持知识检索与人工审核。</div>
              <div class="flex gap-5 text-xs text-slate-400 mt-3">
                <span>最近更新：2026-04-24 20:58</span>
                <span>发布人：周杰</span>
                <span>调用量：3,248</span>
              </div>
            </div>
            <div class="flex gap-2">
              <button class="px-3 py-2 rounded-lg bg-white border border-slate-300 text-slate-700">编辑</button>
              <button class="px-3 py-2 rounded-lg bg-emerald-600 text-white">发布</button>
            </div>
          </div>

          <div class="border border-slate-200 rounded-2xl p-5 bg-white flex justify-between items-start">
            <div>
              <div class="flex items-center gap-3">
                <div class="text-lg font-semibold text-slate-800">销售简报生成 Agent</div>
                <span class="px-2 py-1 rounded-full text-xs bg-slate-200 text-slate-700">草稿 v3</span>
              </div>
              <div class="text-sm text-slate-500 mt-2">面向周报与月报场景，自动汇总销售数据并生成中英文简报。</div>
              <div class="flex gap-5 text-xs text-slate-400 mt-3">
                <span>最近更新：2026-04-23 18:20</span>
                <span>发布人：周杰</span>
                <span>调用量：982</span>
              </div>
            </div>
            <div class="flex gap-2">
              <button class="px-3 py-2 rounded-lg bg-white border border-slate-300 text-slate-700">编辑</button>
              <button class="px-3 py-2 rounded-lg bg-slate-900 text-white">查看版本</button>
            </div>
          </div>
        </div>
      </section>

      <aside class="card p-6 flex flex-col">
        <div class="text-lg font-bold text-slate-800 mb-4">版本历史</div>
        <div class="space-y-4 text-sm flex-1">
          <div class="border-l-4 border-emerald-500 bg-emerald-50 rounded-r-xl p-4">
            <div class="font-semibold text-emerald-700">v7 已发布</div>
            <div class="text-slate-500 mt-1">2026-04-24 20:58 · 新增退款知识检索节点</div>
            <button class="mt-3 px-3 py-1.5 rounded-lg bg-white border border-emerald-200 text-emerald-700">设为当前</button>
          </div>
          <div class="border-l-4 border-slate-300 bg-slate-50 rounded-r-xl p-4">
            <div class="font-semibold text-slate-700">v6 历史版本</div>
            <div class="text-slate-500 mt-1">2026-04-22 15:10 · 调整人工审核触发条件</div>
            <button class="mt-3 px-3 py-1.5 rounded-lg bg-white border border-slate-300 text-slate-700">回滚到该版本</button>
          </div>
          <div class="border-l-4 border-slate-300 bg-slate-50 rounded-r-xl p-4">
            <div class="font-semibold text-slate-700">v5 历史版本</div>
            <div class="text-slate-500 mt-1">2026-04-20 09:42 · 首次接入知识库检索能力</div>
            <button class="mt-3 px-3 py-1.5 rounded-lg bg-white border border-slate-300 text-slate-700">回滚到该版本</button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</body>
</html>
"""

# ── 图8: 知识库管理界面 ──
html_knowledge_ui = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background-color: #f1f5f9; }
  .card { background: white; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06); }
</style>
</head>
<body class="w-[1280px] h-[820px] overflow-hidden p-8">
  <div class="h-full grid grid-cols-[280px_1fr_360px] gap-6">
    <aside class="card p-5 flex flex-col">
      <div class="text-xl font-bold text-slate-800 mb-4">知识数据集</div>
      <div class="space-y-3 text-sm">
        <div class="p-4 rounded-xl bg-blue-50 border border-blue-200">
          <div class="font-semibold text-blue-700">refund-policy-dataset</div>
          <div class="text-slate-500 mt-1">退款政策 / 售后 FAQ / 人工处理规范</div>
        </div>
        <div class="p-4 rounded-xl bg-slate-50 border border-slate-200">
          <div class="font-semibold text-slate-700">sales-brief-dataset</div>
          <div class="text-slate-500 mt-1">季度销售简报、指标口径、模板素材</div>
        </div>
        <div class="p-4 rounded-xl bg-slate-50 border border-slate-200">
          <div class="font-semibold text-slate-700">hr-policy-dataset</div>
          <div class="text-slate-500 mt-1">请假制度、考勤规则、审批模板</div>
        </div>
      </div>
      <button class="mt-auto px-4 py-3 rounded-xl bg-blue-600 text-white font-semibold">新建数据集</button>
    </aside>

    <main class="card p-6 flex flex-col">
      <div class="flex items-center justify-between mb-5">
        <div>
          <div class="text-2xl font-bold text-slate-800">refund-policy-dataset</div>
          <div class="text-sm text-slate-500 mt-1">文档上传 → 异步解析 → 文本分块 → 向量化入库 → 语义检索</div>
        </div>
        <div class="flex gap-3">
          <button class="px-4 py-2 rounded-xl bg-white border border-slate-300 text-slate-700">批量删除</button>
          <button class="px-4 py-2 rounded-xl bg-blue-600 text-white font-semibold">上传文档</button>
        </div>
      </div>

      <div class="grid grid-cols-3 gap-4 mb-5 text-sm">
        <div class="p-4 rounded-xl bg-emerald-50 border border-emerald-200"><div class="text-slate-500">文档总数</div><div class="text-2xl font-bold text-emerald-700 mt-1">36</div></div>
        <div class="p-4 rounded-xl bg-violet-50 border border-violet-200"><div class="text-slate-500">向量分块</div><div class="text-2xl font-bold text-violet-700 mt-1">1,284</div></div>
        <div class="p-4 rounded-xl bg-amber-50 border border-amber-200"><div class="text-slate-500">处理中</div><div class="text-2xl font-bold text-amber-700 mt-1">2</div></div>
      </div>

      <div class="overflow-hidden rounded-2xl border border-slate-200 flex-1">
        <table class="w-full text-sm">
          <thead class="bg-slate-50 text-slate-500">
            <tr>
              <th class="text-left px-4 py-3">文档名</th>
              <th class="text-left px-4 py-3">状态</th>
              <th class="text-left px-4 py-3">分块数</th>
              <th class="text-left px-4 py-3">上传时间</th>
              <th class="text-left px-4 py-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr class="border-t border-slate-200 bg-white">
              <td class="px-4 py-4">退款政策 V3.pdf</td>
              <td class="px-4 py-4"><span class="px-2 py-1 rounded-full bg-emerald-100 text-emerald-700">已完成</span></td>
              <td class="px-4 py-4">248</td>
              <td class="px-4 py-4">2026-04-24 19:40</td>
              <td class="px-4 py-4 text-blue-600">预览 / 删除</td>
            </tr>
            <tr class="border-t border-slate-200 bg-slate-50">
              <td class="px-4 py-4">售后异常案例.docx</td>
              <td class="px-4 py-4"><span class="px-2 py-1 rounded-full bg-amber-100 text-amber-700">处理中</span></td>
              <td class="px-4 py-4">--</td>
              <td class="px-4 py-4">2026-04-24 19:55</td>
              <td class="px-4 py-4 text-slate-400">解析中</td>
            </tr>
            <tr class="border-t border-slate-200 bg-white">
              <td class="px-4 py-4">客服 FAQ 汇总.md</td>
              <td class="px-4 py-4"><span class="px-2 py-1 rounded-full bg-emerald-100 text-emerald-700">已完成</span></td>
              <td class="px-4 py-4">176</td>
              <td class="px-4 py-4">2026-04-23 21:12</td>
              <td class="px-4 py-4 text-blue-600">预览 / 删除</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <aside class="card p-6 flex flex-col">
      <div class="text-lg font-bold text-slate-800 mb-4">语义检索预览</div>
      <div class="text-sm text-slate-500 mb-3">查询："订单发货后还能退款吗？"</div>
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4 mb-4 text-sm">
        <div class="font-semibold text-slate-700">Top1 · 退款政策 V3</div>
        <p class="text-slate-500 mt-2">若商品已发货，用户可在未签收前提交退款申请；若已签收，则需先发起退货流程并由平台审核。</p>
      </div>
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm">
        <div class="font-semibold text-slate-700">Top2 · 客服 FAQ 汇总</div>
        <p class="text-slate-500 mt-2">针对物流在途场景，客服需先确认运单状态，再根据物流节点判断是否直接退款或转人工处理。</p>
      </div>
      <div class="mt-auto rounded-xl bg-slate-900 text-slate-100 p-4 text-sm">
        <div class="font-semibold mb-2">处理链路</div>
        <div class="text-slate-300 leading-7">MinIO 存储 → AsyncDocumentProcessor 异步解析 → 文本切块 → Embedding 向量化 → Milvus 检索</div>
      </div>
    </aside>
  </div>
</body>
</html>
"""

# ── 图9: 用户认证界面 ──
html_auth_ui = """
<!DOCTYPE html>
<html>
<head>
<script src="https://cdn.tailwindcss.com"></script>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background: linear-gradient(135deg, #dbeafe 0%, #eef2ff 50%, #f8fafc 100%); }
  .card { background: rgba(255,255,255,0.92); backdrop-filter: blur(10px); border: 1px solid rgba(148,163,184,0.25); border-radius: 24px; box-shadow: 0 20px 45px rgba(30, 41, 59, 0.12); }
</style>
</head>
<body class="w-[1100px] h-[760px] overflow-hidden flex items-center justify-center">
  <div class="w-[940px] h-[620px] grid grid-cols-[1.05fr_0.95fr] card overflow-hidden">
    <section class="bg-slate-900 text-white p-10 flex flex-col justify-between">
      <div>
        <div class="text-sm uppercase tracking-[0.3em] text-blue-300">AI Agent Platform</div>
        <div class="text-4xl font-bold mt-6 leading-tight">统一用户认证与安全访问入口</div>
        <p class="text-slate-300 mt-5 leading-7">支持邮箱验证码注册、账号密码登录、密码找回与 JWT 令牌鉴权，为 Agent、知识库和工作流模块提供统一身份支撑。</p>
      </div>
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div class="rounded-2xl bg-white/5 border border-white/10 p-4">
          <div class="text-blue-300 font-semibold">安全机制</div>
          <div class="text-slate-300 mt-2">BCrypt 加密 / JWT / 登录限流</div>
        </div>
        <div class="rounded-2xl bg-white/5 border border-white/10 p-4">
          <div class="text-blue-300 font-semibold">身份能力</div>
          <div class="text-slate-300 mt-2">注册 / 登录 / 找回密码 / Token 校验</div>
        </div>
      </div>
    </section>

    <section class="p-10 flex items-center justify-center bg-white/70">
      <div class="w-full max-w-[360px]">
        <div class="flex gap-2 mb-6 text-sm">
          <span class="px-3 py-1 rounded-full bg-blue-600 text-white">登录</span>
          <span class="px-3 py-1 rounded-full bg-slate-100 text-slate-600">注册</span>
          <span class="px-3 py-1 rounded-full bg-slate-100 text-slate-600">找回密码</span>
        </div>
        <div class="text-3xl font-bold text-slate-800">欢迎回来</div>
        <div class="text-slate-500 mt-2 mb-6">请输入账号信息以访问平台控制台</div>

        <div class="space-y-4 text-sm">
          <div>
            <label class="block text-slate-600 mb-1">邮箱</label>
            <div class="px-4 py-3 rounded-xl border border-slate-300 bg-white">zhoujie@example.com</div>
          </div>
          <div>
            <label class="block text-slate-600 mb-1">密码</label>
            <div class="px-4 py-3 rounded-xl border border-slate-300 bg-white flex justify-between"><span>••••••••••••</span><span class="text-slate-400">显示</span></div>
          </div>
          <div class="flex items-center justify-between text-xs text-slate-500">
            <label class="flex items-center gap-2"><span class="w-4 h-4 rounded border border-slate-300 bg-blue-600"></span>7天内免登录</label>
            <span>忘记密码？</span>
          </div>
          <button class="w-full py-3 rounded-xl bg-blue-600 text-white font-semibold">登录并获取 Token</button>
        </div>

        <div class="mt-6 rounded-2xl bg-emerald-50 border border-emerald-200 p-4 text-sm">
          <div class="font-semibold text-emerald-700">系统提示</div>
          <div class="text-slate-500 mt-2">验证码发送频率已限制为 60 秒一次；连续登录失败将触发限流保护。</div>
        </div>
      </div>
    </section>
  </div>
</body>
</html>
"""

async def capture_html(browser, html, filename, viewport):
    page = await browser.new_page(viewport=viewport)
    await page.set_content(html)
    await page.wait_for_timeout(2000)
    await page.screenshot(path=os.path.join(OUT_DIR, filename))
    await page.close()


async def generate_images():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)

        await capture_html(browser, html_wf_exec, "fig_wf_exec.png", {"width": 1200, "height": 800})
        await capture_html(browser, html_wf_log, "fig_wf_log.png", {"width": 1000, "height": 700})
        await capture_html(browser, html_review_panel, "fig_review_panel.png", {"width": 1000, "height": 750})
        await capture_html(browser, html_resume_ui, "fig_resume_ui.png", {"width": 800, "height": 500})
        await capture_html(browser, html_sse_ui, "fig_sse_ui.png", {"width": 900, "height": 550})
        await capture_html(browser, html_agent_editor, "fig_agent_editor.png", {"width": 1360, "height": 840})
        await capture_html(browser, html_agent_list, "fig_agent_list.png", {"width": 1280, "height": 820})
        await capture_html(browser, html_knowledge_ui, "fig_knowledge_ui.png", {"width": 1280, "height": 820})
        await capture_html(browser, html_auth_ui, "fig_auth_ui.png", {"width": 1100, "height": 760})

        await browser.close()
        print("All 9 mock screenshots generated successfully!")

if __name__ == "__main__":
    asyncio.run(generate_images())
