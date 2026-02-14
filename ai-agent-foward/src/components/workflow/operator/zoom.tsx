interface WorkflowZoomProps {
  zoom?: number;
  onZoomIn?: () => void;
  onZoomOut?: () => void;
}

export function WorkflowZoom({ zoom = 100, onZoomIn, onZoomOut }: WorkflowZoomProps) {
  return (
    <div className="workflow-operator-group">
      <button type="button" onClick={onZoomOut}>
        -
      </button>
      <span>{zoom}%</span>
      <button type="button" onClick={onZoomIn}>
        +
      </button>
    </div>
  );
}
