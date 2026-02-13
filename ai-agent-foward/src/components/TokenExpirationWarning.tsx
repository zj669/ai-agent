import { useEffect, useState } from 'react';
import { Modal } from 'antd';
import { useAuthStore } from '../stores/authStore';
import { useNavigate } from 'react-router-dom';

export const TokenExpirationWarning: React.FC = () => {
  const { checkTokenExpiration, logout } = useAuthStore();
  const [showWarning, setShowWarning] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    // Check token expiration every minute
    const interval = setInterval(() => {
      const needsWarning = checkTokenExpiration();
      if (needsWarning && !showWarning) {
        setShowWarning(true);
      }
    }, 60000); // Check every 1 minute

    // Initial check
    const needsWarning = checkTokenExpiration();
    if (needsWarning) {
      setShowWarning(true);
    }

    return () => clearInterval(interval);
  }, [checkTokenExpiration, showWarning]);

  const handleContinue = () => {
    setShowWarning(false);
    // Navigate to login page to refresh session
    navigate('/login', { state: { refresh: true } });
  };

  const handleLogout = async () => {
    setShowWarning(false);
    await logout();
    navigate('/login');
  };

  return (
    <Modal
      title="会话即将过期"
      open={showWarning}
      onOk={handleContinue}
      onCancel={handleLogout}
      okText="继续工作"
      cancelText="退出登录"
      closable={false}
    >
      <p>您的登录会话将在 5 分钟内过期，请保存您的工作。</p>
      <p>点击"继续工作"重新登录以延长会话。</p>
    </Modal>
  );
};
