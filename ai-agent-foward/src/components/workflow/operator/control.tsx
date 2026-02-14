interface WorkflowControlProps {
  onUndo?: () => void;
  onRedo?: () => void;
}

export function WorkflowControl({ onUndo, onRedo }: WorkflowControlProps) {
  return (
    <div className="workflow-operator-group">
      <button type="button" onClick={onUndo}>
        Undo
      </button>
      <button type="button" onClick={onRedo}>
        Redo
      </button>
    </div>
  );
}
