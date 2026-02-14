interface WorkflowHeaderProps {
  title?: string;
}

export function WorkflowHeader({ title = 'Workflow Editor' }: WorkflowHeaderProps) {
  return <div style={{ fontWeight: 600, fontSize: 14 }}>{title}</div>;
}
