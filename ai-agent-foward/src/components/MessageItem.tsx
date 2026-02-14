import { Avatar } from 'antd';
import { UserOutlined, RobotOutlined, LoadingOutlined } from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Message, MessageRole, MessageStatus } from '../types/chat';
import dayjs from 'dayjs';

interface MessageItemProps {
  message: Message;
}

export const MessageItem: React.FC<MessageItemProps> = ({ message }) => {
  const isUser = message.role === MessageRole.USER;
  const isStreaming = message.status === MessageStatus.STREAMING;

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: 16
      }}
    >
      <div
        style={{
          display: 'flex',
          flexDirection: isUser ? 'row-reverse' : 'row',
          maxWidth: '70%',
          gap: 12
        }}
      >
        <Avatar
          icon={isUser ? <UserOutlined /> : <RobotOutlined />}
          style={{
            backgroundColor: isUser ? '#1890ff' : '#52c41a',
            flexShrink: 0
          }}
        />

        <div
          style={{
            backgroundColor: isUser ? '#e6f7ff' : '#f0f0f0',
            padding: '12px 16px',
            borderRadius: '8px'
          }}
        >
          <div style={{ wordBreak: 'break-word' }}>
            {isUser ? (
              <div>{message.content}</div>
            ) : (
              <ReactMarkdown
                components={{
                  code({ node, inline, className, children, ...props }) {
                    const match = /language-(\w+)/.exec(className || '');
                    return !inline && match ? (
                      <SyntaxHighlighter
                        style={vscDarkPlus}
                        language={match[1]}
                        PreTag="div"
                        {...props}
                      >
                        {String(children).replace(/\n$/, '')}
                      </SyntaxHighlighter>
                    ) : (
                      <code className={className} {...props}>
                        {children}
                      </code>
                    );
                  }
                }}
              >
                {message.content || ''}
              </ReactMarkdown>
            )}

            {isStreaming && (
              <div style={{ marginTop: 8, color: '#999' }}>
                <LoadingOutlined /> 正在输入...
              </div>
            )}
          </div>

          <div style={{ marginTop: 8, fontSize: 12, color: '#999', textAlign: 'right' }}>
            {dayjs(message.createdAt).format('HH:mm:ss')}
          </div>
        </div>
      </div>
    </div>
  );
};
