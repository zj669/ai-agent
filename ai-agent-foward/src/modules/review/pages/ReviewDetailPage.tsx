import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Card,
  Button,
  Space,
  Tag,
  Descriptions,
  Spin,
  message,
  Typography,
  Divider,
  Alert,
  Form,
  Input,
  Modal,
} from "antd";
import {
  ArrowLeftOutlined,
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
} from "@ant-design/icons";
import {
  getReviewDetail,
  rejectReview,
  resumeReview,
  type ReviewDetail,
  type NodeContext,
} from "../../../shared/api/adapters/reviewAdapter";

const { Title, Text } = Typography;
const { TextArea } = Input;
const LLM_OUTPUT_KEYS = ["llm_output", "response", "text"] as const;

function hasRenderableObject(
  value?: Record<string, unknown>,
): value is Record<string, unknown> {
  return !!value && Object.keys(value).length > 0;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof error.message === "string" &&
    error.message.trim()
  ) {
    return error.message;
  }

  return fallback;
}

function normalizeNodeOutputs(
  nodeType: string,
  outputs?: Record<string, unknown>,
) {
  if (!outputs || nodeType !== "LLM") {
    return outputs;
  }

  const next = { ...outputs };
  const primaryKey =
    next.llm_output !== undefined
      ? "llm_output"
      : next.response !== undefined
        ? "response"
        : next.text !== undefined
          ? "text"
          : null;

  if (!primaryKey) {
    return next;
  }

  const primaryValue = next[primaryKey];
  for (const key of LLM_OUTPUT_KEYS) {
    if (key !== primaryKey && next[key] === primaryValue) {
      delete next[key];
    }
  }

  return next;
}

function hydrateLlmOutputAliases(
  nodeType: string,
  outputs: Record<string, unknown>,
) {
  if (nodeType !== "LLM") {
    return outputs;
  }

  const primaryValue = outputs.llm_output ?? outputs.response ?? outputs.text;

  if (primaryValue === undefined) {
    return outputs;
  }

  return {
    ...outputs,
    llm_output: primaryValue,
    response: primaryValue,
    text: primaryValue,
  };
}

export default function ReviewDetailPage() {
  const { executionId } = useParams<{ executionId: string }>();
  const navigate = useNavigate();

  const [detail, setDetail] = useState<ReviewDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingNode, setEditingNode] = useState<NodeContext | null>(null);
  const [editForm] = Form.useForm();
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectComment, setRejectComment] = useState("");

  // 存储所有节点的编辑内容
  const [nodeEdits, setNodeEdits] = useState<
    Record<string, Record<string, unknown>>
  >({});

  const loadDetail = async () => {
    if (!executionId) return;

    setLoading(true);
    try {
      const data = await getReviewDetail(executionId);
      setDetail(data);
    } catch {
      message.error("加载审核详情失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadDetail();
  }, [executionId]);

  const handleEditNode = (node: NodeContext) => {
    setEditingNode(node);
    // 如果已有编辑内容，使用编辑内容；否则使用原始输出
    const currentEdits =
      normalizeNodeOutputs(
        node.nodeType,
        nodeEdits[node.nodeId] || node.outputs || {},
      ) || {};
    editForm.setFieldsValue({
      outputs: JSON.stringify(currentEdits, null, 2),
    });
    setEditModalVisible(true);
  };

  const handleSaveEdit = async () => {
    try {
      const values = await editForm.validateFields();
      const outputs = JSON.parse(values.outputs);

      if (editingNode) {
        setNodeEdits((prev) => ({
          ...prev,
          [editingNode.nodeId]: hydrateLlmOutputAliases(
            editingNode.nodeType,
            outputs,
          ),
        }));
        message.success("编辑已保存（提交审核时生效）");
      }

      setEditModalVisible(false);
      setEditingNode(null);
    } catch (error) {
      if (error instanceof SyntaxError) {
        message.error("JSON 格式错误，请检查");
      }
    }
  };

  const handleApprove = async () => {
    if (!detail) return;

    setSubmitting(true);
    try {
      await resumeReview({
        executionId: detail.executionId,
        nodeId: detail.nodeId,
        expectedVersion: detail.executionVersion,
        nodeEdits: Object.keys(nodeEdits).length > 0 ? nodeEdits : undefined,
      });
      message.success("审核已通过");
      navigate("/reviews");
    } catch (error) {
      message.error(getErrorMessage(error, "操作失败"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!detail) return;

    setSubmitting(true);
    try {
      await rejectReview({
        executionId: detail.executionId,
        nodeId: detail.nodeId,
        reason: rejectComment.trim() || "无原因",
      });
      message.success("已拒绝，工作流已终止");
      navigate("/reviews");
    } catch {
      message.error("操作失败");
    } finally {
      setSubmitting(false);
      setRejectModalVisible(false);
    }
  };

  const getCurrentNode = () => {
    return detail?.nodes.find((n) => n.nodeId === detail.nodeId);
  };

  const getUpstreamNodes = () => {
    return detail?.nodes.filter((n) => n.nodeId !== detail.nodeId) || [];
  };

  const renderNodeCard = (node: NodeContext, isCurrent: boolean) => {
    const hasEdits = !!nodeEdits[node.nodeId];
    const displayOutputs = normalizeNodeOutputs(
      node.nodeType,
      hasEdits ? nodeEdits[node.nodeId] : node.outputs,
    );
    const hasDisplayOutputs = hasRenderableObject(displayOutputs);

    return (
      <Card
        key={node.nodeId}
        size="small"
        style={{ marginBottom: 16 }}
        title={
          <Space>
            <Tag color={isCurrent ? "orange" : "blue"}>{node.nodeType}</Tag>
            <Text strong>{node.nodeName}</Text>
            {isCurrent && <Tag color="red">当前节点</Tag>}
            {hasEdits && <Tag color="green">已编辑</Tag>}
          </Space>
        }
        extra={
          <Space>
            {hasDisplayOutputs && (
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEditNode(node)}
              >
                编辑输出
              </Button>
            )}
          </Space>
        }
      >
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="节点ID">
            <Text code>{node.nodeId}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={node.status === "SUCCEEDED" ? "green" : "orange"}>
              {node.status}
            </Tag>
          </Descriptions.Item>
          {node.inputs && (
            <Descriptions.Item label="输入">
              <pre
                style={{
                  margin: 0,
                  maxHeight: 200,
                  overflow: "auto",
                  backgroundColor: "#f5f5f5",
                  padding: 8,
                  borderRadius: 4,
                }}
              >
                {JSON.stringify(node.inputs, null, 2)}
              </pre>
            </Descriptions.Item>
          )}
          {hasDisplayOutputs && (
            <Descriptions.Item label="输出">
              <pre
                style={{
                  margin: 0,
                  maxHeight: 200,
                  overflow: "auto",
                  backgroundColor: hasEdits ? "#f6ffed" : "#f5f5f5",
                  padding: 8,
                  borderRadius: 4,
                  border: hasEdits ? "1px solid #b7eb8f" : "none",
                }}
              >
                {JSON.stringify(displayOutputs, null, 2)}
              </pre>
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>
    );
  };

  if (loading) {
    return (
      <div style={{ padding: 24, textAlign: "center" }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!detail) {
    return (
      <div style={{ padding: 24 }}>
        <Alert message="审核详情不存在" type="error" />
      </div>
    );
  }

  const currentNode = getCurrentNode();
  const upstreamNodes = getUpstreamNodes();

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size="large" style={{ width: "100%" }}>
        {/* 头部 */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Space>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate("/reviews")}
            >
              返回列表
            </Button>
            <Title level={4} style={{ margin: 0 }}>
              审核详情
            </Title>
          </Space>
          <Space>
            <Button
              danger
              icon={<CloseOutlined />}
              onClick={() => setRejectModalVisible(true)}
              loading={submitting}
            >
              拒绝
            </Button>
            <Button
              type="primary"
              icon={<CheckOutlined />}
              onClick={handleApprove}
              loading={submitting}
              style={{ backgroundColor: "#52c41a", borderColor: "#52c41a" }}
            >
              通过
            </Button>
          </Space>
        </div>

        {/* 基本信息 */}
        <Card title="基本信息">
          <Descriptions column={2} bordered>
            <Descriptions.Item label="执行ID">
              <Text code>{detail.executionId}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="审核阶段">
              <Tag
                color={
                  detail.triggerPhase === "BEFORE_EXECUTION"
                    ? "orange"
                    : "green"
                }
              >
                {detail.triggerPhase === "BEFORE_EXECUTION"
                  ? "执行前"
                  : "执行后"}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="当前节点" span={2}>
              <Space>
                <Text strong>{detail.nodeName}</Text>
                <Text code>{detail.nodeId}</Text>
              </Space>
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 提示信息 */}
        {detail.triggerPhase === "BEFORE_EXECUTION" ? (
          <Alert
            message="执行前审核"
            description="该节点尚未执行，您可以查看上游节点的输出，并在通过审核后节点将开始执行。"
            type="info"
            showIcon
          />
        ) : (
          <Alert
            message="执行后审核"
            description="该节点已执行完成，您可以查看和编辑节点的输出，修改后的数据将作为该节点的最终输出继续流转。"
            type="warning"
            showIcon
          />
        )}

        {/* 当前节点 */}
        {currentNode && (
          <>
            <Divider>当前审核节点</Divider>
            {renderNodeCard(currentNode, true)}
          </>
        )}

        {/* 上游节点 */}
        {upstreamNodes.length > 0 && (
          <>
            <Divider>上游已完成节点</Divider>
            {upstreamNodes.map((node) => renderNodeCard(node, false))}
          </>
        )}

        {/* 编辑提示 */}
        {Object.keys(nodeEdits).length > 0 && (
          <Alert
            message={`已编辑 ${Object.keys(nodeEdits).length} 个节点的输出`}
            description="这些修改将在您提交审核时生效"
            type="success"
            showIcon
          />
        )}
      </Space>

      {/* 编辑模态框 */}
      <Modal
        title={`编辑节点输出 - ${editingNode?.nodeName}`}
        open={editModalVisible}
        onOk={handleSaveEdit}
        onCancel={() => {
          setEditModalVisible(false);
          setEditingNode(null);
        }}
        width={800}
        okText="保存"
        cancelText="取消"
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            label="输出数据（JSON格式）"
            name="outputs"
            rules={[
              { required: true, message: "请输入输出数据" },
              {
                validator: async (_, value) => {
                  try {
                    JSON.parse(value);
                  } catch {
                    throw new Error("请输入有效的 JSON 格式");
                  }
                },
              },
            ]}
          >
            <TextArea
              rows={15}
              style={{ fontFamily: "monospace" }}
              placeholder='{"key": "value"}'
            />
          </Form.Item>
          <Alert
            message="提示"
            description="请确保 JSON 格式正确，修改后的数据将作为该节点的输出继续流转到下游节点。"
            type="info"
            showIcon
          />
        </Form>
      </Modal>

      {/* 拒绝模态框 */}
      <Modal
        title="拒绝审核"
        open={rejectModalVisible}
        onOk={handleReject}
        onCancel={() => setRejectModalVisible(false)}
        okText="确认拒绝"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <Space direction="vertical" style={{ width: "100%" }}>
          <TextArea
            rows={4}
            placeholder="请输入拒绝原因（可选）"
            value={rejectComment}
            onChange={(e) => setRejectComment(e.target.value)}
          />
        </Space>
      </Modal>
    </div>
  );
}
