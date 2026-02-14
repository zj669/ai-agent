interface WorkflowBasePanelProps {
  title?: string;
  description?: string;
}

export function WorkflowBasePanel({ title = 'Node Panel', description }: WorkflowBasePanelProps) {
  return (
    <div style={{ padding: 12 }}>
      <h3 style={{ margin: 0, fontSize: 14 }}>{title}</h3>
      {description ? <p style={{ marginTop: 8, color: '#64748b' }}>{description}</p> : null}
    </div>
  );
}
