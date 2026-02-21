import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { fetchWorkflowDetail, publishWorkflow, saveWorkflow } from '../api/workflowService'
import { validateConnection } from '../validation/validateConnection'
import { validateWorkflowGraph } from '../validation/validateWorkflowGraph'

type WorkflowNodeType = 'START' | 'END' | 'LLM' | 'CONDITION' | 'TOOL' | 'HTTP'

type WorkflowNode = {
  id: string
  name: string
  type: WorkflowNodeType
}

type WorkflowEdge = {
  id: string
  source: string
  target: string
}

const INITIAL_NODES: WorkflowNode[] = [
  { id: 'start', name: '开始节点', type: 'START' },
  { id: 'end', name: '结束节点', type: 'END' }
]

function normalizeNodeType(input: unknown): WorkflowNodeType {
  const value = typeof input === 'string' ? input.toUpperCase() : ''
  if (value === 'START' || value === 'END' || value === 'LLM' || value === 'CONDITION' || value === 'TOOL' || value === 'HTTP') {
    return value
  }
  return 'TOOL'
}

function mapGraphToNodes(graph: Record<string, unknown>): WorkflowNode[] {
  const rawNodes = Array.isArray(graph.nodes) ? graph.nodes : []

  return rawNodes
    .map((item, index) => {
      if (!item || typeof item !== 'object') {
        return null
      }

      const node = item as Record<string, unknown>
      const id = typeof node.nodeId === 'string' ? node.nodeId : typeof node.id === 'string' ? node.id : `node-${index + 1}`
      const name =
        typeof node.nodeName === 'string' ? node.nodeName : typeof node.name === 'string' ? node.name : `节点-${index + 1}`
      return {
        id,
        name,
        type: normalizeNodeType(node.nodeType ?? node.type)
      }
    })
    .filter((node): node is WorkflowNode => node !== null)
}

function mapGraphToEdges(graph: Record<string, unknown>): WorkflowEdge[] {
  const rawEdges = Array.isArray(graph.edges) ? graph.edges : []

  return rawEdges
    .map((item, index) => {
      if (!item || typeof item !== 'object') {
        return null
      }

      const edge = item as Record<string, unknown>
      const source = typeof edge.source === 'string' ? edge.source : ''
      const target = typeof edge.target === 'string' ? edge.target : ''
      if (!source || !target) {
        return null
      }

      return {
        id: typeof edge.edgeId === 'string' ? edge.edgeId : typeof edge.id === 'string' ? edge.id : `edge-${index + 1}`,
        source,
        target
      }
    })
    .filter((edge): edge is WorkflowEdge => edge !== null)
}

function buildGraphPayload(nodes: WorkflowNode[], edges: WorkflowEdge[]) {
  return {
    version: '1.0.0',
    nodes: nodes.map((node) => ({
      nodeId: node.id,
      nodeName: node.name,
      nodeType: node.type,
      userConfig: {}
    })),
    edges: edges.map((edge) => ({
      edgeId: edge.id,
      source: edge.source,
      target: edge.target,
      edgeType: 'DEPENDENCY'
    }))
  }
}

function WorkflowEditorPage() {
  const { agentId } = useParams()
  const [nodes, setNodes] = useState<WorkflowNode[]>(INITIAL_NODES)
  const [edges, setEdges] = useState<WorkflowEdge[]>([])
  const [sourceNodeId, setSourceNodeId] = useState('start')
  const [targetNodeId, setTargetNodeId] = useState('end')
  const [connectError, setConnectError] = useState('')
  const [selectedNodeId, setSelectedNodeId] = useState<string>('')
  const [agentName, setAgentName] = useState('')
  const [version, setVersion] = useState<number | null>(null)
  const [loadMessage, setLoadMessage] = useState('')
  const [saveMessage, setSaveMessage] = useState('')
  const [publishMessage, setPublishMessage] = useState('')
  const [operationState, setOperationState] = useState<'idle' | 'saving' | 'publishing'>('idle')
  const [isDirty, setIsDirty] = useState(false)

  const selectedNode = useMemo(() => nodes.find((node) => node.id === selectedNodeId), [nodes, selectedNodeId])
  const numericAgentId = agentId ? Number(agentId) : NaN

  useEffect(() => {
    if (!Number.isFinite(numericAgentId)) {
      setLoadMessage('Agent ID 无效，无法加载 workflow')
      return
    }

    void fetchWorkflowDetail(numericAgentId)
      .then((detail) => {
        setAgentName(detail.name)
        setVersion(detail.version)
        setLoadMessage('')

        if (detail.graph && typeof detail.graph === 'object') {
          const nextNodes = mapGraphToNodes(detail.graph as Record<string, unknown>)
          const nextEdges = mapGraphToEdges(detail.graph as Record<string, unknown>)
          if (nextNodes.length > 0) {
            setNodes(nextNodes)
            setSourceNodeId(nextNodes[0].id)
            setTargetNodeId(nextNodes[nextNodes.length - 1].id)
          }
          setEdges(nextEdges)
          setIsDirty(false)
        }
      })
      .catch(() => {
        setLoadMessage('workflow 加载失败，请稍后重试')
      })
  }, [numericAgentId])

  const handleConnect = () => {
    if (!sourceNodeId || !targetNodeId) {
      setConnectError('请选择起点和终点节点')
      return
    }

    const validation = validateConnection({ source: sourceNodeId, target: targetNodeId })
    if (!validation.ok) {
      setConnectError(validation.message)
      return
    }

    const duplicated = edges.some((edge) => edge.source === sourceNodeId && edge.target === targetNodeId)
    if (duplicated) {
      setConnectError('该连线已存在，请勿重复添加')
      return
    }

    setEdges((previous) => [
      ...previous,
      {
        id: `${sourceNodeId}-${targetNodeId}-${previous.length + 1}`,
        source: sourceNodeId,
        target: targetNodeId
      }
    ])
    setIsDirty(true)
    setSaveMessage('')
    setPublishMessage('')
    setConnectError('')
  }

  const handleSave = async () => {
    setSaveMessage('')
    setPublishMessage('')

    if (!Number.isFinite(numericAgentId) || version === null || !agentName) {
      setSaveMessage('缺少保存所需信息，请刷新页面重试')
      return
    }

    const validation = validateWorkflowGraph({ nodes, edges })
    if (!validation.ok) {
      setSaveMessage(validation.message)
      return
    }

    setOperationState('saving')
    try {
      const result = await saveWorkflow({
        agentId: numericAgentId,
        version,
        name: agentName,
        graph: buildGraphPayload(nodes, edges)
      })
      setVersion(result.version)
      setIsDirty(false)
      setSaveMessage('保存成功')
    } catch {
      setSaveMessage('保存失败，请稍后重试')
    } finally {
      setOperationState('idle')
    }
  }

  const handlePublish = async () => {
    setPublishMessage('')

    if (!Number.isFinite(numericAgentId)) {
      setPublishMessage('Agent ID 无效，无法发布')
      return
    }

    const validation = validateWorkflowGraph({ nodes, edges })
    if (!validation.ok) {
      setPublishMessage(validation.message)
      return
    }

    if (isDirty) {
      setPublishMessage('请先保存后再发布')
      return
    }

    setOperationState('publishing')
    try {
      const result = await publishWorkflow(numericAgentId)
      setVersion(result.version)
      setIsDirty(false)
      setPublishMessage('发布成功')
    } catch {
      setPublishMessage('发布失败，请稍后重试')
    } finally {
      setOperationState('idle')
    }
  }

  return (
    <section>
      <h2 className="text-2xl font-semibold">Workflow 编辑</h2>
      <p className="mt-3 text-sm text-muted-foreground">当前 Agent: {agentId}</p>
      {version !== null ? <p className="mt-1 text-sm text-slate-600">当前版本: {version}</p> : null}
      <p className="mt-1 text-sm text-slate-600">状态: {isDirty ? '未保存' : '已保存'}</p>
      {loadMessage ? <p className="mt-2 text-sm text-red-600">{loadMessage}</p> : null}

      <div className="mt-4 flex gap-2">
        <button
          type="button"
          className="rounded bg-slate-900 px-3 py-1 text-sm text-white disabled:cursor-not-allowed disabled:bg-slate-400"
          onClick={handleSave}
          disabled={operationState !== 'idle'}
        >
          {operationState === 'saving' ? '保存中...' : '保存'}
        </button>
        <button
          type="button"
          className="rounded border border-slate-300 px-3 py-1 text-sm disabled:cursor-not-allowed disabled:text-slate-400"
          onClick={handlePublish}
          disabled={operationState !== 'idle'}
        >
          {operationState === 'publishing' ? '发布中...' : '发布'}
        </button>
      </div>
      {saveMessage ? <p className="mt-2 text-sm text-red-600">{saveMessage}</p> : null}
      {publishMessage ? <p className="mt-1 text-sm text-red-600">{publishMessage}</p> : null}

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <div>
          <h3 className="text-base font-medium">节点列表</h3>
          <ul className="mt-3 space-y-2 text-sm">
            {nodes.map((node) => (
              <li key={node.id} className="flex items-center justify-between rounded border border-slate-200 px-3 py-2">
                <span>
                  {node.name}（{node.type}）
                </span>
                <button
                  type="button"
                  className="rounded border border-slate-300 px-2 py-1 text-xs"
                  onClick={() => setSelectedNodeId(node.id)}
                >
                  节点配置
                </button>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="text-base font-medium">连线交互</h3>
          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
            <select
              aria-label="source-node"
              className="rounded border border-slate-300 px-2 py-1"
              value={sourceNodeId}
              onChange={(event) => setSourceNodeId(event.target.value)}
            >
              {nodes.map((node) => (
                <option key={node.id} value={node.id}>
                  {node.name}
                </option>
              ))}
            </select>
            <span>→</span>
            <select
              aria-label="target-node"
              className="rounded border border-slate-300 px-2 py-1"
              value={targetNodeId}
              onChange={(event) => setTargetNodeId(event.target.value)}
            >
              {nodes.map((node) => (
                <option key={node.id} value={node.id}>
                  {node.name}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="rounded bg-slate-900 px-3 py-1 text-white"
              onClick={handleConnect}
            >
              添加连线
            </button>
          </div>
          {connectError ? <p className="mt-2 text-sm text-red-600">{connectError}</p> : null}

          <h4 className="mt-4 text-sm font-medium">当前连线</h4>
          <ul className="mt-2 space-y-1 text-sm text-slate-700">
            {edges.map((edge) => (
              <li key={edge.id}>
                {edge.source} → {edge.target}
              </li>
            ))}
            {edges.length === 0 ? <li className="text-slate-500">暂无连线</li> : null}
          </ul>
        </div>
      </div>

      <div className="mt-6 rounded border border-slate-200 p-3 text-sm">
        <h3 className="font-medium">节点配置入口</h3>
        {selectedNode ? (
          <div className="mt-2 space-y-1">
            <p>节点 ID: {selectedNode.id}</p>
            <p>节点名称: {selectedNode.name}</p>
            <p>节点类型: {selectedNode.type}</p>
          </div>
        ) : (
          <p className="mt-2 text-slate-500">请选择一个节点进入配置</p>
        )}
      </div>
    </section>
  )
}

export default WorkflowEditorPage
