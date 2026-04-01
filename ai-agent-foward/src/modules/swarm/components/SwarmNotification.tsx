import { useEffect, useState } from "react";
import { Alert } from "antd";

export type NotificationType = "spawn" | "completed" | "failed" | "killed";

export interface SwarmNotificationData {
  id: string;
  agentId: number;
  agentRole: string;
  type: NotificationType;
  timestamp: number;
}

interface Props {
  notifications: SwarmNotificationData[];
  onDismiss: (id: string) => void;
}

const TYPE_META: Record<
  NotificationType,
  { color: string; icon: string; message: string }
> = {
  spawn: {
    color: "cyan",
    icon: "+",
    message: "Worker 已派发",
  },
  completed: {
    color: "green",
    icon: "\u2713",
    message: "Worker 已完成",
  },
  failed: {
    color: "red",
    icon: "\u2717",
    message: "Worker 执行失败",
  },
  killed: {
    color: "orange",
    icon: "\u2715",
    message: "Worker 被终止",
  },
};

function SwarmToast({
  notification,
  onDismiss,
}: {
  notification: SwarmNotificationData;
  onDismiss: () => void;
}) {
  const meta = TYPE_META[notification.type];

  useEffect(() => {
    const timer = window.setTimeout(() => {
      onDismiss();
    }, 5000);
    return () => window.clearTimeout(timer);
  }, [onDismiss]);

  return (
    <Alert
      message={
        <span style={{ fontSize: 13 }}>
          <strong>{notification.agentRole}</strong>
          <span style={{ marginLeft: 8, color: "#8c8c8c", fontSize: 12 }}>
            {meta.message}
          </span>
        </span>
      }
      type={meta.color as "success" | "info" | "warning" | "error"}
      showIcon
      closable
      onClose={onDismiss}
      style={{
        borderRadius: 8,
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.08)",
        marginBottom: 8,
        minWidth: 280,
      }}
    />
  );
}

export default function SwarmNotification({ notifications, onDismiss }: Props) {
  const [visible, setVisible] = useState<Set<string>>(new Set());

  useEffect(() => {
    setVisible(new Set(notifications.map((n) => n.id)));
  }, [notifications]);

  if (visible.size === 0) return null;

  return (
    <>
      <style>{`
        .swarm-notification-container {
          position: fixed;
          bottom: 24px;
          right: 24px;
          z-index: 10000;
          display: flex;
          flex-direction: column;
          align-items: flex-end;
        }
      `}</style>
      <div className="swarm-notification-container">
        {notifications
          .filter((n) => visible.has(n.id))
          .slice(-5)
          .reverse()
          .map((notification) => (
            <SwarmToast
              key={notification.id}
              notification={notification}
              onDismiss={() => onDismiss(notification.id)}
            />
          ))}
      </div>
    </>
  );
}
