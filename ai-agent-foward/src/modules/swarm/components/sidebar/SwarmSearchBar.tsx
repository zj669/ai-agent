import { Input } from 'antd'
import { SearchOutlined } from '@ant-design/icons'

interface Props {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

export default function SwarmSearchBar({ value, onChange, placeholder = '搜索 Agent / 群组' }: Props) {
  return (
    <Input
      prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
      value={value}
      onChange={e => onChange(e.target.value)}
      placeholder={placeholder}
      allowClear
      size="small"
      style={{ marginBottom: 8 }}
    />
  )
}
