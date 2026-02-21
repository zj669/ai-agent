import { useEffect, useState } from 'react'
import {
  createDataset,
  fetchDatasetList,
  fetchDocumentList,
  uploadDocument,
  type DatasetListItem,
  type DocumentListItem
} from '../api/knowledgeService'

function KnowledgePage() {
  const [datasets, setDatasets] = useState<DatasetListItem[]>([])
  const [documents, setDocuments] = useState<DocumentListItem[]>([])
  const [selectedDatasetId, setSelectedDatasetId] = useState<string>('')

  const [datasetName, setDatasetName] = useState('')
  const [datasetDescription, setDatasetDescription] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)

  const [isCreating, setIsCreating] = useState(false)
  const [isUploading, setIsUploading] = useState(false)

  const [listError, setListError] = useState('')
  const [createError, setCreateError] = useState('')
  const [uploadError, setUploadError] = useState('')

  const [createMessage, setCreateMessage] = useState('')
  const [uploadMessage, setUploadMessage] = useState('')

  useEffect(() => {
    void fetchDatasetList()
      .then((items) => {
        setDatasets(items)
        setListError('')

        if (items.length === 0) {
          setSelectedDatasetId('')
          return
        }

        setSelectedDatasetId((current) => {
          if (current && items.some((item) => item.datasetId === current)) {
            return current
          }

          return items[0].datasetId
        })
      })
      .catch(() => {
        setDatasets([])
        setDocuments([])
        setSelectedDatasetId('')
        setListError('知识库列表加载失败，请稍后重试')
      })
  }, [])

  useEffect(() => {
    if (!selectedDatasetId) {
      setDocuments([])
      return
    }

    void fetchDocumentList(selectedDatasetId)
      .then((docItems) => {
        setDocuments(docItems)
      })
      .catch(() => {
        setDocuments([])
      })
  }, [selectedDatasetId])

  const handleCreateDataset = async () => {
    if (isCreating) {
      return
    }

    const name = datasetName.trim()
    if (!name) {
      setCreateError('请输入知识库名称')
      return
    }

    setCreateError('')
    setCreateMessage('')
    setIsCreating(true)

    try {
      const created = await createDataset({
        name,
        description: datasetDescription.trim() || undefined
      })

      setDatasets((prev) => [created, ...prev])
      setSelectedDatasetId(created.datasetId)
      setDatasetName('')
      setDatasetDescription('')
      setCreateMessage('知识库创建成功')
    } catch {
      setCreateError('创建知识库失败，请稍后重试')
    } finally {
      setIsCreating(false)
    }
  }

  const handleUpload = async () => {
    if (isUploading) {
      return
    }

    if (!selectedDatasetId) {
      setUploadError('请先创建或选择知识库')
      return
    }

    if (!selectedFile) {
      setUploadError('请选择上传文件')
      return
    }

    setUploadError('')
    setUploadMessage('')
    setIsUploading(true)

    try {
      await uploadDocument({
        datasetId: selectedDatasetId,
        file: selectedFile
      })

      const latestDocuments = await fetchDocumentList(selectedDatasetId)
      setDocuments(latestDocuments)
      setUploadMessage('上传成功')
      setSelectedFile(null)
    } catch {
      setUploadError('上传失败，请稍后重试')
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <section>
      <h2 className="text-2xl font-semibold">知识库</h2>

      <div className="mt-4 rounded border border-slate-200 p-3">
        <h3 className="text-sm font-medium">创建知识库</h3>
        <label className="mt-3 block text-sm" htmlFor="dataset-name">
          名称
        </label>
        <input
          id="dataset-name"
          aria-label="dataset-name"
          className="mt-1 w-full rounded border border-slate-300 px-2 py-1 text-sm"
          value={datasetName}
          onChange={(event) => setDatasetName(event.target.value)}
        />

        <label className="mt-3 block text-sm" htmlFor="dataset-description">
          描述
        </label>
        <input
          id="dataset-description"
          aria-label="dataset-description"
          className="mt-1 w-full rounded border border-slate-300 px-2 py-1 text-sm"
          value={datasetDescription}
          onChange={(event) => setDatasetDescription(event.target.value)}
        />

        <button
          type="button"
          className="mt-3 rounded bg-slate-900 px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:bg-slate-400"
          onClick={handleCreateDataset}
          disabled={isCreating}
        >
          {isCreating ? '创建中...' : '创建知识库'}
        </button>

        {createMessage ? <p className="mt-2 text-sm text-green-700">{createMessage}</p> : null}
        {createError ? <p className="mt-2 text-sm text-red-600">{createError}</p> : null}
      </div>

      <div className="mt-6 rounded border border-slate-200 p-3">
        <h3 className="text-sm font-medium">知识库列表</h3>
        {listError ? <p className="mt-2 text-sm text-red-600">{listError}</p> : null}

        <ul className="mt-3 space-y-2 text-sm">
          {datasets.map((dataset) => (
            <li key={dataset.datasetId} className="rounded border border-slate-200 p-2">
              <div className="font-medium">{dataset.name}</div>
              {dataset.description ? <div className="text-slate-500">{dataset.description}</div> : null}
            </li>
          ))}
        </ul>
      </div>

      <div className="mt-6 rounded border border-slate-200 p-3">
        <h3 className="text-sm font-medium">上传文档</h3>

        <label className="mt-3 block text-sm" htmlFor="dataset-select">
          目标知识库
        </label>
        <select
          id="dataset-select"
          className="mt-1 w-full rounded border border-slate-300 px-2 py-1 text-sm"
          value={selectedDatasetId}
          onChange={(event) => setSelectedDatasetId(event.target.value)}
        >
          <option value="">请选择知识库</option>
          {datasets.map((dataset) => (
            <option key={dataset.datasetId} value={dataset.datasetId}>
              {dataset.name}
            </option>
          ))}
        </select>

        <label className="mt-3 block text-sm" htmlFor="document-file">
          文档文件
        </label>
        <input
          id="document-file"
          aria-label="document-file"
          type="file"
          className="mt-1 w-full text-sm"
          onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
        />

        <button
          type="button"
          className="mt-3 rounded bg-slate-900 px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:bg-slate-400"
          onClick={handleUpload}
          disabled={isUploading}
        >
          {isUploading ? '上传中...' : '上传文档'}
        </button>

        {uploadMessage ? <p className="mt-2 text-sm text-green-700">{uploadMessage}</p> : null}
        {uploadError ? <p className="mt-2 text-sm text-red-600">{uploadError}</p> : null}
      </div>

      <div className="mt-6 rounded border border-slate-200 p-3">
        <h3 className="text-sm font-medium">文档状态</h3>
        <ul className="mt-3 space-y-2 text-sm">
          {documents.map((document) => (
            <li key={document.documentId} className="rounded border border-slate-200 p-2">
              <div className="font-medium">{document.filename}</div>
              <div className="text-slate-600">状态: {document.status}</div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}

export default KnowledgePage
