import { useState } from 'react';
import { Upload, Modal, Form, InputNumber, Progress, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';

const { Dragger } = Upload;

interface DocumentUploadProps {
  onUpload: (file: File, chunkSize: number, chunkOverlap: number) => Promise<void>;
  uploading: boolean;
  uploadProgress: number;
}

export const DocumentUpload: React.FC<DocumentUploadProps> = ({
  onUpload,
  uploading,
  uploadProgress
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [form] = Form.useForm();

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.txt,.pdf,.doc,.docx,.md,.csv,.xls,.xlsx',
    beforeUpload: (file) => {
      // Check file size (max 50MB)
      const isLt50M = file.size / 1024 / 1024 < 50;
      if (!isLt50M) {
        message.error('文件大小不能超过 50MB');
        return false;
      }

      setSelectedFile(file);
      setIsModalOpen(true);
      return false; // Prevent auto upload
    },
    showUploadList: false
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      const values = await form.validateFields();
      await onUpload(selectedFile, values.chunkSize, values.chunkOverlap);
      setIsModalOpen(false);
      setSelectedFile(null);
      form.resetFields();
    } catch (error) {
      // Error handled in hook
    }
  };

  const handleCancel = () => {
    setIsModalOpen(false);
    setSelectedFile(null);
    form.resetFields();
  };

  return (
    <>
      <Dragger {...uploadProps} disabled={uploading}>
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
        <p className="ant-upload-hint">
          支持 .txt, .pdf, .doc, .docx, .md, .csv, .xls, .xlsx，单个文件不超过 50MB
        </p>
      </Dragger>

      {uploading && (
        <div style={{ marginTop: 16 }}>
          <Progress percent={uploadProgress} status="active" />
        </div>
      )}

      <Modal
        title="上传配置"
        open={isModalOpen}
        onOk={handleUpload}
        onCancel={handleCancel}
        okText="上传"
        cancelText="取消"
        confirmLoading={uploading}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            chunkSize: 500,
            chunkOverlap: 50
          }}
        >
          <Form.Item label="文件名">
            <div>{selectedFile?.name}</div>
          </Form.Item>

          <Form.Item
            label="分块大小"
            name="chunkSize"
            tooltip="文档将被分割成多个块进行向量化，每块的字符数"
            rules={[
              { required: true, message: '请输入分块大小' },
              { type: 'number', min: 100, max: 2000, message: '分块大小应在 100-2000 之间' }
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="建议 500"
              addonAfter="字符"
            />
          </Form.Item>

          <Form.Item
            label="分块重叠"
            name="chunkOverlap"
            tooltip="相邻块之间重叠的字符数，用于保持上下文连贯性"
            rules={[
              { required: true, message: '请输入分块重叠' },
              { type: 'number', min: 0, max: 500, message: '分块重叠应在 0-500 之间' }
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="建议 50"
              addonAfter="字符"
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
