import { useEffect, useMemo, useRef, useState } from "react";
import { cn } from "../../../lib/utils";

export interface UpstreamVariable {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  fieldKey: string;
  fieldLabel: string;
  fieldType: string;
  /** 完整引用路径，如 start.output.inputMessage */
  ref: string;
}

interface VariableRefSelectorProps {
  /** 当前选中的引用路径，保存时使用裸引用，如 start.output.inputMessage */
  value: string;
  /** 没有引用时的字面量默认值 */
  literalValue?: string;
  /** 可选的上游变量列表 */
  variables: UpstreamVariable[];
  /** 选中变量时的回调 */
  onSelect: (ref: string) => void;
  /** 清除引用时的回调 */
  onClear: () => void;
  /** 字面量默认值变化时的回调 */
  onLiteralChange?: (value: string | undefined) => void;
  placeholder?: string;
}

const TEMPLATE_REF_PATTERN = /^\{\{\s*([^{}]+?)\s*\}\}$/;

export function formatReferenceTemplate(ref: string): string {
  return ref ? `{{${ref}}}` : "";
}

function parseTemplateRef(value: string): string | null {
  const match = TEMPLATE_REF_PATTERN.exec(value.trim());
  return match ? match[1].trim() : null;
}

function getTrigger(value: string, cursor: number) {
  const beforeCursor = value.slice(0, cursor);
  const start = beforeCursor.lastIndexOf("{{");
  if (start < 0) return null;

  const lastClose = beforeCursor.lastIndexOf("}}");
  if (lastClose > start) return null;

  return {
    start,
    query: beforeCursor.slice(start + 2).trim().toLowerCase(),
  };
}

function variableMatches(variable: UpstreamVariable, query: string): boolean {
  if (!query) return true;
  const haystack = [
    variable.nodeId,
    variable.nodeName,
    variable.nodeType,
    variable.fieldKey,
    variable.fieldLabel,
    variable.fieldType,
    variable.ref,
  ]
    .join(" ")
    .toLowerCase();
  return haystack.includes(query);
}

function VariableRefSelector({
  value,
  literalValue = "",
  variables,
  onSelect,
  onClear,
  onLiteralChange,
  placeholder = "输入默认值，或输入 {{ 选择上游引用",
}: VariableRefSelectorProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const displayValue = value ? formatReferenceTemplate(value) : literalValue;
  const [draft, setDraft] = useState(displayValue);
  const [trigger, setTrigger] = useState<{ start: number; query: string } | null>(
    null,
  );

  useEffect(() => {
    setDraft(displayValue);
  }, [displayValue]);

  useEffect(() => {
    if (!trigger) return;
    const handler = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as HTMLElement)
      ) {
        setTrigger(null);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [trigger]);

  const filteredVariables = useMemo(
    () =>
      variables.filter((variable) =>
        variableMatches(variable, trigger?.query ?? ""),
      ),
    [trigger?.query, variables],
  );

  const grouped = useMemo(
    () =>
      filteredVariables.reduce<Record<string, UpstreamVariable[]>>(
        (acc, variable) => {
          const key = variable.nodeId;
          if (!acc[key]) acc[key] = [];
          acc[key].push(variable);
          return acc;
        },
        {},
      ),
    [filteredVariables],
  );

  const syncTrigger = (nextValue: string, cursor: number) => {
    const nextTrigger = getTrigger(nextValue, cursor);
    setTrigger(nextTrigger);
  };

  const handleValueChange = (nextValue: string, cursor: number) => {
    setDraft(nextValue);
    syncTrigger(nextValue, cursor);

    const parsedRef = parseTemplateRef(nextValue);
    if (parsedRef) {
      onLiteralChange?.(undefined);
      onSelect(parsedRef);
      return;
    }

    if (value) {
      onClear();
    }

    onLiteralChange?.(nextValue || undefined);
  };

  const selectVariable = (variable: UpstreamVariable) => {
    const template = formatReferenceTemplate(variable.ref);
    setDraft(template);
    setTrigger(null);
    onLiteralChange?.(undefined);
    onSelect(variable.ref);

    requestAnimationFrame(() => {
      const input = inputRef.current;
      if (!input) return;
      input.focus();
      input.setSelectionRange(template.length, template.length);
    });
  };

  return (
    <div ref={containerRef} className="relative">
      <input
        ref={inputRef}
        className={cn(
          "w-full rounded border px-2 py-1 text-xs transition",
          value
            ? "border-blue-200 bg-blue-50 text-blue-700"
            : "border-slate-300 bg-white text-slate-700 placeholder:text-slate-300",
        )}
        placeholder={placeholder}
        value={draft}
        onChange={(event) =>
          handleValueChange(
            event.target.value,
            event.target.selectionStart ?? event.target.value.length,
          )
        }
        onClick={(event) =>
          syncTrigger(
            event.currentTarget.value,
            event.currentTarget.selectionStart ??
              event.currentTarget.value.length,
          )
        }
        onKeyUp={(event) =>
          syncTrigger(
            event.currentTarget.value,
            event.currentTarget.selectionStart ??
              event.currentTarget.value.length,
          )
        }
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            setTrigger(null);
          }
          if (
            event.key === "Enter" &&
            trigger &&
            filteredVariables.length > 0
          ) {
            event.preventDefault();
            selectVariable(filteredVariables[0]);
          }
        }}
      />

      {trigger && (
        <div className="absolute left-0 top-full z-50 mt-1 max-h-56 w-72 overflow-y-auto rounded-md border border-slate-200 bg-white shadow-lg">
          {filteredVariables.length === 0 ? (
            <div className="px-3 py-2 text-xs text-slate-400">
              无匹配的上游变量
            </div>
          ) : (
            Object.entries(grouped).map(([nodeId, vars]) => (
              <div key={nodeId}>
                <div className="sticky top-0 border-b border-slate-100 bg-slate-50 px-3 py-1 text-[10px] font-medium text-slate-500">
                  {vars[0].nodeName} ({vars[0].nodeType})
                </div>
                {vars.map((variable) => (
                  <button
                    key={variable.ref}
                    type="button"
                    className={cn(
                      "flex w-full items-center gap-2 px-3 py-1.5 text-left text-xs transition hover:bg-blue-50",
                      variable.ref === value && "bg-blue-50 text-blue-600",
                    )}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => selectVariable(variable)}
                  >
                    <span className="min-w-0 flex-1 truncate text-slate-700">
                      {variable.nodeName} ·{" "}
                      {variable.fieldLabel || variable.fieldKey}
                    </span>
                    <span className="shrink-0 text-[10px] text-slate-400">
                      {variable.fieldType}
                    </span>
                  </button>
                ))}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export default VariableRefSelector;
