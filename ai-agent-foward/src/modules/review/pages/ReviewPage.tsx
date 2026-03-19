import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Table,
  Button,
  Space,
  Tag,
  Badge,
  Popconfirm,
  Empty,
  Spin,
  Typography,
  Input,
  message,
} from "antd";
import {
  AuditOutlined,
  CheckOutlined,
  CloseOutlined,
  ReloadOutlined,
  RobotOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import {
  getPendingReviews,
  rejectReview,
  resumeReview,
  type PendingReview,
} from "../../../shared/api/adapters/reviewAdapter";

const { Text, Title } = Typography;
const { TextArea } = Input;

export default function ReviewPage() {
  const navigate = useNavigate();
  const [reviews, setReviews] = useState<PendingReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [rejectComments, setRejectComments] = useState<Record<string, string>>(
    {},
  );

  const loadReviews = async () => {
    setLoading(true);
    try {
      const data = await getPendingReviews();
      setReviews(data);
    } catch {
      message.error("加载审核列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadReviews();
  }, []);

  const rowKey = (r: PendingReview) => `${r.executionId}-${r.nodeId}`;

  const handleApprove = async (review: PendingReview) => {
    setSubmitting(true);
    try {
      await resumeReview({
        executionId: review.executionId,
        nodeId: review.nodeId,
        expectedVersion: review.executionVersion,
      });
      message.success("已批准");
      void loadReviews();
    } catch {
      message.error("操作失败");
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async (review: PendingReview) => {
    const key = rowKey(review);
    const comment = rejectComments[key];
    setSubmitting(true);
    try {
      await rejectReview({
        executionId: review.executionId,
        nodeId: review.nodeId,
        expectedVersion: review.executionVersion,
        reason: comment?.trim() || "无原因",
      });
      message.success("已拒绝，工作流已终止");
      setRejectComments((prev) => {
        const next = { ...prev };
        delete next[key];
        return next;
      });
      void loadReviews();
    } catch {
      message.error("操作失败");
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: "Agent",
      dataIndex: "agentName",
      key: "agentName",
      render: (v: string) => (
        <Space>
          <RobotOutlined style={{ color: "#1677ff" }} />
          <span>{v || "-"}</span>
        </Space>
      ),
    },
    {
      title: "节点",
      dataIndex: "nodeName",
      key: "nodeName",
      render: (v: string) => <Tag color="blue">{v || "-"}</Tag>,
    },
    {
      title: "审核阶段",
      dataIndex: "triggerPhase",
      key: "triggerPhase",
      width: 120,
      render: (v: string) => (
        <Tag color={v === "BEFORE_EXECUTION" ? "orange" : "green"}>
          {v === "BEFORE_EXECUTION" ? "执行前" : "执行后"}
        </Tag>
      ),
    },
    {
      title: "暂停时间",
      dataIndex: "pausedAt",
      key: "pausedAt",
      width: 180,
      render: (v: string) => {
        try {
          return new Date(v).toLocaleString("zh-CN", { hour12: false });
        } catch {
          return v;
        }
      },
    },
    {
      title: "状态",
      key: "status",
      width: 120,
      render: () => <Badge status="processing" text="待审核" />,
    },
    {
      title: "操作",
      key: "action",
      width: 280,
      render: (_: unknown, record: PendingReview) => {
        const key = rowKey(record);
        return (
          <Space>
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => navigate(`/reviews/${record.executionId}`)}
            >
              查看详情
            </Button>
            <Popconfirm
              title="确认通过此审核？"
              onConfirm={() => void handleApprove(record)}
              okText="确认"
              cancelText="取消"
            >
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                loading={submitting}
                style={{ backgroundColor: "#52c41a", borderColor: "#52c41a" }}
              >
                通过
              </Button>
            </Popconfirm>
            <Popconfirm
              title="确认拒绝此审核？"
              description={
                <TextArea
                  rows={2}
                  placeholder="拒绝原因（可选）"
                  value={rejectComments[key] ?? ""}
                  onChange={(e) =>
                    setRejectComments((prev) => ({
                      ...prev,
                      [key]: e.target.value,
                    }))
                  }
                  style={{ marginTop: 8, width: 240 }}
                  onClick={(e) => e.stopPropagation()}
                />
              }
              onConfirm={() => void handleReject(record)}
              okText="确认拒绝"
              okButtonProps={{ danger: true }}
              cancelText="取消"
            >
              <Button
                danger
                size="small"
                icon={<CloseOutlined />}
                loading={submitting}
              >
                拒绝
              </Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 24,
        }}
      >
        <div>
          <Title level={4} style={{ margin: 0 }}>
            <AuditOutlined style={{ marginRight: 8 }} />
            审核中心
          </Title>
          <Text type="secondary" style={{ marginTop: 4, display: "block" }}>
            共 {reviews.length} 条待审核
          </Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => void loadReviews()}
          loading={loading}
        >
          刷新
        </Button>
      </div>

      <Spin spinning={loading}>
        <Table
          columns={columns}
          dataSource={reviews}
          rowKey={rowKey}
          pagination={{ pageSize: 10, showTotal: (total) => `共 ${total} 条` }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无待审核项"
              />
            ),
          }}
        />
      </Spin>
    </div>
  );
}
