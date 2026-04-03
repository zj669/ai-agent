import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeHighlight from "rehype-highlight";
import type { Components } from "react-markdown";

interface Props {
  content: string;
  /** Set true for streaming: appends blinking cursor */
  streaming?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

const components: Components = {
  // Override pre to add our own wrapper class
  pre: ({ children, ...props }) => (
    <pre {...props} style={{ margin: 0 }}>
      {children}
    </pre>
  ),
};

export default function MarkdownRenderer({
  content,
  streaming = false,
  className = "ai-markdown-body",
  style,
}: Props) {
  return (
    <div className={className} style={style}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        components={components}
      >
        {content}
      </ReactMarkdown>
      {streaming && <span className="swarm-cursor" aria-hidden="true" />}
    </div>
  );
}
