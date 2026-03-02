import { useCallback, useEffect, useMemo, useRef, useState, type DragEvent } from 'react'
import { useParams } from 'react-router-dom'
import {
  ReactFlow,
  addEdge,
  useNodesState,
  useEdgesState,
  Controls,
  MiniMap,
  Background,
  BackgroundVariant,
  type Connection,
  type Edge,
  type Node,
  type ReactFlowInstance,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import { fetchWorkflowDetail, publishWorkflow, saveWorkflow, fetchNodeTemplates } from '../api/workflowService'
import { validateConnection } from '../validation/validateConnection'
import { validateWorkflowGraph } from '../validation/validateWorkflowGraph'
import WorkflowNode, { type WorkflowNodeData, type WorkflowNodeType, type SchemaPolicy, type FieldSchema } from '../components/WorkflowNode'
import { useEditorStore } from '../stores/useEditorStore'
import EditorHeader from '../components/EditorHeader'
import AgentConfigPanel from '../components/AgentConfigPanel'
import CanvasToolbar from '../components/CanvasToolbar'
import CustomEdge from '../components/CustomEdge'
import CustomConnectionLine from '../components/CustomConnectionLine'

const nodeTypes = { workflowNode: WorkflowNode }
const edgeTypes = { custom: CustomEdge }

const INITIAL_NODES: Node[] = [
  {
    id: 'start',
    type: 'workflowNode',
    position: { x: 50, y: 250 },
    data: { label: '开始节点', nodeType: 'START' } satisfies WorkflowNodeData,
  },
  {
    id: 'end',
    type: 'workflowNode',
    position: { x: 600, y: 250 },
    data: { label: '结束节点', nodeType: 'END' } satisfies WorkflowNodeData,
  },
]

function normalizeNodeType(input: unknown): WorkflowNodeType {
  const value = typeof input === 'string' ? input.toUpperCase() : ''
  if (value === 'START' || value === 'END' || value === 'LLM' || value === 'CONDITION' || value === 'TOOL' || value === 'HTTP' || value === 'KNOWLEDGE') {
    return value
  }
  return 'TOOL'
}

function mapGraphToFlowNodes(graph: Record<string, unknown>): Node[] {
  const rawNodes = Array.isArray(graph.nodes) ? graph.nodes : []
  return rawNodes
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null
      const node = item as Record<string, unknown>
      const id = typeof node.nodeId === 'string' ? node.nodeId : typeof node.id === 'string' ? node.id : `node-${index + 1}`
      const name = typeof node.nodeName === 'string' ? node.nodeName : typeof node.name === 'string' ? node.name : `节点-${index + 1}`
      const nodeType = normalizeNodeType(node.nodeType ?? node.type)
      const userConfig = (node.userConfig && typeof node.userConfig === 'object') ? node.userConfig as Record<string, unknown> : {}
      const policy = (node.policy && typeof node.policy === 'object') ? node.policy as SchemaPolicy : undefined
      const inputSchema = Array.isArray(node.inputSchema) ? node.inputSchema as FieldSchema[] : undefined
      const outputSchema = Array.isArray(node.outputSchema) ? node.outputSchema as FieldSchema[] : undefined
      const pos = node.position as Record<string, unknown> | undefined
      const x = typeof pos?.x === 'number' ? pos.x : 250
      const y = typeof pos?.y === 'number' ? pos.y : index * 150 + 50
      return {
        id,
        type: 'workflowNode',
        position: { x, y },
        data: { label: name, nodeType, userConfig, policy, inputSchema, outputSchema } satisfies WorkflowNodeData,
      } as Node
    })
    .filter((n): n is Node => n !== null)
}

function mapGraphToFlowEdges(graph: Record<string, unknown>): Edge[] {
  const rawEdges = Array.isArray(graph.edges) ? graph.edges : []
  return rawEdges
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null
      const edge = item as Record<string, unknown>
      const source = typeof edge.source === 'string' ? edge.source : ''
      const target = typeof edge.target === 'string' ? edge.target : ''
      if (!source || !target) return null
      return {
        id: typeof edge.edgeId === 'string' ? edge.edgeId : typeof edge.id === 'string' ? edge.id : `edge-${index + 1}`,
        source,
        target,
        type: 'custom',
      } as Edge
    })
    .filter((e): e is Edge => e !== null)
}

function buildGraphPayload(nodes: Node[], edges: Edge[]) {
  const startNode = nodes.find((n) => (n.data as unknown as WorkflowNodeData).nodeType === 'START')
  return {
    version: '1.0',
    startNodeId: startNode?.id ?? 'start',
    nodes: nodes.map((node) => {
      const d = node.data as unknown as WorkflowNodeData
      return {
        id: node.id,
        nodeId: node.id,
        type: d.nodeType,
        nodeName: d.label,
        nodeType: d.nodeType,
        position: { x: node.position.x, y: node.position.y },
        policy: d.policy,
        inputSchema: d.inputSchema,
        outputSchema: d.outputSchema,
        userConfig: d.userConfig ?? {},
      }
    }),
    edges: edges.map((edge) => ({
      edgeId: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle ?? null,
      edgeType: 'DEPENDENCY',
    })),
  }
}

let nodeCounter = 0

function WorkflowEditorPage() {
  const { agentId } = useParams()
  const [nodes, setNodes, onNodesChange] = useNodesState(INITIAL_NODES)
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [loadMessage, setLoadMessage] = useState('')
  const [saveMessage, setSaveMessage] = useState('')
  const [publishMessage, setPublishMessage] = useState('')
  const [connectError, setConnectError] = useState('')
  const reactFlowWrapper = useRef<HTMLDivElement>(null)
  const [rfInstance, setRfInstance] = useState<ReactFlowInstance | null>(null)

  const store = useEditorStore()
  const numericAgentId = agentId ? Number(agentId) : NaN

  useEffect(() => {
    if (!Number.isFinite(numericAgentId)) {
      setLoadMessage('Agent ID 无效，无法加载 workflow')
      return
    }

    const loadAll = async () => {
      try {
        const [detail, templates] = await Promise.all([
          fetchWorkflowDetail(numericAgentId),
          fetchNodeTemplates(),
        ])

        store.setNodeTemplates(templates)
        store.setAgentInfo({
          agentName: detail.name,
          agentDescription: detail.description ?? '',
          agentIcon: detail.icon ?? '',
          version: detail.version,
        })
        setLoadMessage('')

        if (detail.graph && typeof detail.graph === 'object') {
          let nextNodes = mapGraphToFlowNodes(detail.graph as Record<string, unknown>)
          const nextEdges = mapGraphToFlowEdges(detail.graph as Record<string, unknown>)

          // 用 template 的 initialSchema / defaultSchemaPolicy 补全缺失的 schema 和 policy
          nextNodes = nextNodes.map((n) => {
            const d = n.data as unknown as WorkflowNodeData
            const tpl = templates.find((t) => t.typeCode === d.nodeType)
            if (!tpl) return n
            const initial = tpl.initialSchema as { inputSchema?: FieldSchema[]; outputSchema?: FieldSchema[] } | undefined
            const defaultPolicy = tpl.defaultSchemaPolicy as SchemaPolicy | undefined
            // 如果已有 schema 为空数组但 template 有 system 字段，用 template 的补全
            const mergeSchema = (existing: FieldSchema[] | undefined, tplSchema: FieldSchema[] | undefined): FieldSchema[] => {
              if (!existing || existing.length === 0) return tplSchema ?? []
              // 补全缺失的 system 字段
              const systemFields = (tplSchema ?? []).filter((f) => f.system && !existing.some((e) => e.key === f.key))
              return systemFields.length > 0 ? [...existing, ...systemFields] : existing
            }
            const patched: WorkflowNodeData = {
              ...d,
              inputSchema: mergeSchema(d.inputSchema, initial?.inputSchema),
              outputSchema: mergeSchema(d.outputSchema, initial?.outputSchema),
              policy: d.policy ?? defaultPolicy,
            }
            return { ...n, data: patched }
          })

          if (nextNodes.length > 0) setNodes(nextNodes)
          setEdges(nextEdges)
          store.markClean()
        }
      } catch {
        setLoadMessage('workflow 加载失败，请稍后重试')
      }
    }

    void loadAll()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [numericAgentId, setNodes, setEdges])

  const onConnect = useCallback(
    (connection: Connection) => {
      const validation = validateConnection({ source: connection.source, target: connection.target })
      if (!validation.ok) {
        setConnectError(validation.message)
        return
      }
      const duplicated = edges.some(
        (e) => e.source === connection.source && e.target === connection.target
          && (e.sourceHandle ?? null) === (connection.sourceHandle ?? null)
      )
      if (duplicated) {
        setConnectError('该连线已存在，请勿重复添加')
        return
      }
      setEdges((eds) => addEdge({ ...connection, type: 'custom' }, eds))
      store.markDirty()
      setConnectError('')
      setSaveMessage('')
      setPublishMessage('')
    },
    [edges, setEdges, store],
  )

  const onDragOver = useCallback((event: DragEvent) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
  }, [])

  const onDrop = useCallback(
    (event: DragEvent) => {
      event.preventDefault()
      const nodeType = event.dataTransfer.getData('application/workflow-node-type') as WorkflowNodeType
      if (!nodeType) return
      if (!rfInstance || !reactFlowWrapper.current) return

      const bounds = reactFlowWrapper.current.getBoundingClientRect()
      const position = rfInstance.screenToFlowPosition({
        x: event.clientX - bounds.left,
        y: event.clientY - bounds.top,
      })

      // 从 nodeTemplates 获取默认 schema 和 policy
      const tpl = store.nodeTemplates.find((t) => t.typeCode === nodeType)
      const initialSchema = tpl?.initialSchema as { inputSchema?: FieldSchema[]; outputSchema?: FieldSchema[] } | undefined
      const policy = tpl?.defaultSchemaPolicy as SchemaPolicy | undefined

      nodeCounter += 1
      const ts = Date.now()
      const ifBranchId = `branch-${ts}-0`
      const elseBranchId = `else-${ts}`
      const newNode: Node = {
        id: `${nodeType.toLowerCase()}-${Date.now()}-${nodeCounter}`,
        type: 'workflowNode',
        position,
        data: (nodeType === 'CONDITION'
          ? {
              label: `条件节点`,
              nodeType,
              branches: [
                { id: ifBranchId, name: '如果' },
                { id: elseBranchId, name: '否则' },
              ],
              policy,
              inputSchema: initialSchema?.inputSchema ?? [],
              outputSchema: initialSchema?.outputSchema ?? [],
              userConfig: {
                conditionConfig: {
                  routingStrategy: 'EXPRESSION',
                  branches: [
                    { id: ifBranchId, name: '如果', type: 'if', priority: 1, logic: 'AND', conditions: [{ sourceRef: '', operator: 'EQUALS', value: '', valueType: 'literal' }] },
                    { id: elseBranchId, name: '否则', type: 'else', priority: 0, logic: 'AND', conditions: [] },
                  ],
                },
              },
            }
          : {
              label: `${nodeType} 节点`,
              nodeType,
              policy,
              inputSchema: initialSchema?.inputSchema ?? [],
              outputSchema: initialSchema?.outputSchema ?? [],
            }) satisfies WorkflowNodeData,
      }
      setNodes((nds) => [...nds, newNode])
      store.markDirty()
      setSaveMessage('')
      setPublishMessage('')
    },
    [rfInstance, setNodes, store],
  )

  const validationNodes = useMemo(
    () => nodes.map((n) => ({ id: n.id })),
    [nodes],
  )
  const validationEdges = useMemo(
    () => edges.map((e) => ({ source: e.source, target: e.target })),
    [edges],
  )

  const handleSave = async () => {
    setSaveMessage('')
    setPublishMessage('')
    if (!Number.isFinite(numericAgentId) || store.version === null || !store.agentName) {
      setSaveMessage('缺少保存所需信息，请刷新页面重试')
      return
    }
    const validation = validateWorkflowGraph({ nodes: validationNodes, edges: validationEdges })
    if (!validation.ok) {
      setSaveMessage(validation.message)
      return
    }
    store.setOperationState('saving')
    try {
      const result = await saveWorkflow({
        agentId: numericAgentId,
        version: store.version,
        name: store.agentName,
        description: store.agentDescription,
        icon: store.agentIcon,
        graph: buildGraphPayload(nodes, edges),
      })
      store.setAgentInfo({ version: result.version })
      store.markClean()
      setSaveMessage('保存成功')
    } catch {
      setSaveMessage('保存失败，请稍后重试')
    } finally {
      store.setOperationState('idle')
    }
  }

  const handlePublish = async () => {
    setPublishMessage('')
    setSaveMessage('')
    if (!Number.isFinite(numericAgentId)) {
      setPublishMessage('Agent ID 无效，无法发布')
      return
    }
    const validation = validateWorkflowGraph({ nodes: validationNodes, edges: validationEdges })
    if (!validation.ok) {
      setPublishMessage(validation.message)
      return
    }
    if (store.isDirty) {
      setPublishMessage('请先保存后再发布')
      return
    }
    store.setOperationState('publishing')
    try {
      const result = await publishWorkflow(numericAgentId)
      store.setAgentInfo({ version: result.version })
      store.markClean()
      setPublishMessage('发布成功')
    } catch {
      setPublishMessage('发布失败，请稍后重试')
    } finally {
      store.setOperationState('idle')
    }
  }

  const errorBanner = saveMessage || publishMessage || connectError || loadMessage

  return (
    <section className="flex h-screen flex-col bg-white">
      <EditorHeader
        agentName={store.agentName}
        isDirty={store.isDirty}
        operationState={store.operationState}
        onSave={handleSave}
        onPublish={handlePublish}
      />

      {errorBanner && (
        <div className="border-b border-red-100 bg-red-50 px-4 py-1 text-sm text-red-600">
          {saveMessage || publishMessage || connectError || loadMessage}
        </div>
      )}

      <div className="flex flex-1 overflow-hidden">
        <AgentConfigPanel
          agentName={store.agentName}
          agentDescription={store.agentDescription}
          agentIcon={store.agentIcon}
          collapsed={store.panelCollapsed}
          onToggle={store.togglePanel}
          onChange={(field, value) => { store.setAgentInfo({ [field]: value }); store.markDirty() }}
        />

        <div className="relative flex-1" ref={reactFlowWrapper}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onInit={setRfInstance}
            onDragOver={onDragOver}
            onDrop={onDrop}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            defaultEdgeOptions={{ type: 'custom' }}
            connectionLineComponent={CustomConnectionLine}
            fitView
          >
            <Controls position="bottom-right" />
            <MiniMap position="top-right" />
            <Background variant={BackgroundVariant.Dots} gap={16} size={1} />
          </ReactFlow>
          <CanvasToolbar />
        </div>
      </div>
    </section>
  )
}

export default WorkflowEditorPage