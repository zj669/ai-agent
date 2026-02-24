import { useEffect, useRef } from 'react'

/**
 * 登录/注册页左侧 — 等距视角工作流动画
 * 纯 CSS/SVG 实现，无外部依赖
 */
export default function WorkflowAnimation() {
  const canvasRef = useRef<HTMLDivElement>(null)

  /* 粒子沿路径流动 — 用 CSS offset-path 实现 */
  useEffect(() => {
    // 触发入场动画
    const el = canvasRef.current
    if (el) el.classList.add('wf-enter')
  }, [])

  return (
    <div
      ref={canvasRef}
      style={{
        width: '100%',
        height: '100%',
        background: '#F5F8FF',
        position: 'relative',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {/* 背景网格 */}
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, opacity: 0.35 }}>
        <defs>
          <pattern id="grid" width="32" height="32" patternUnits="userSpaceOnUse">
            <circle cx="16" cy="16" r="0.8" fill="#B0C4E8" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />
      </svg>

      {/* 装饰光晕 */}
      <div style={{
        position: 'absolute', width: 300, height: 300, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(41,112,255,0.08) 0%, transparent 70%)',
        top: '20%', left: '30%', filter: 'blur(40px)',
      }} />
      <div style={{
        position: 'absolute', width: 200, height: 200, borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(122,90,248,0.06) 0%, transparent 70%)',
        bottom: '25%', right: '20%', filter: 'blur(30px)',
      }} />

      {/* 工作流画布 — 等距视角 */}
      <div style={{
        position: 'relative',
        width: 360, height: 240,
        transform: 'perspective(800px) rotateX(6deg) rotateY(-4deg) scale(0.85)',
        transformStyle: 'preserve-3d',
      }}>
        {/* 连线 SVG */}
        <svg
          width="360" height="240"
          viewBox="0 0 420 280"
          fill="none"
          style={{ position: 'absolute', top: 0, left: 0, pointerEvents: 'none' }}
        >
          {/* 开始 → LLM */}
          <path d="M120 45 L120 75 Q120 85 130 85 L190 85" stroke="rgba(41,112,255,0.3)" strokeWidth="2" />
          {/* LLM → 条件 */}
          <path d="M320 85 L340 85 Q350 85 350 95 L350 135" stroke="rgba(41,112,255,0.3)" strokeWidth="2" />
          {/* 条件 → 结束 (if) */}
          <path d="M310 175 L310 205 Q310 215 300 215 L260 215" stroke="rgba(41,112,255,0.3)" strokeWidth="2" />
          {/* 条件 → LLM (else 回环) */}
          <path d="M390 155 L400 155 Q410 155 410 145 L410 55 Q410 45 400 45 L320 45 Q310 45 310 55 L310 65" stroke="rgba(122,90,248,0.2)" strokeWidth="1.5" strokeDasharray="4 4" />

          {/* 流动粒子 */}
          <circle r="3" fill="#2970FF" opacity="0.8">
            <animateMotion dur="2.5s" repeatCount="indefinite" path="M120 45 L120 75 Q120 85 130 85 L190 85" />
          </circle>
          <circle r="3" fill="#2970FF" opacity="0.8">
            <animateMotion dur="2.5s" repeatCount="indefinite" begin="0.8s" path="M320 85 L340 85 Q350 85 350 95 L350 135" />
          </circle>
          <circle r="3" fill="#2970FF" opacity="0.8">
            <animateMotion dur="2s" repeatCount="indefinite" begin="1.6s" path="M310 175 L310 205 Q310 215 300 215 L260 215" />
          </circle>
          <circle r="2.5" fill="#7A5AF8" opacity="0.6">
            <animateMotion dur="4s" repeatCount="indefinite" begin="2s" path="M390 155 L400 155 Q410 155 410 145 L410 55 Q410 45 400 45 L320 45 Q310 45 310 55 L310 65" />
          </circle>
        </svg>

        {/* 开始节点 */}
        <NodeCard
          top={10} left={60}
          icon="▶" iconBg="#12B76A" iconColor="#fff"
          title="开始" subtitle="用户输入"
          delay={0}
        />

        {/* LLM 节点 */}
        <NodeCard
          top={55} left={180}
          icon="🤖" iconBg="#2970FF" iconColor="#fff"
          title="LLM" subtitle="GPT-4o"
          delay={0.15}
          highlight
        />

        {/* 条件节点 */}
        <NodeCard
          top={120} left={280}
          icon="⑂" iconBg="#F79009" iconColor="#fff"
          title="条件判断" subtitle="意图分类"
          delay={0.3}
        />

        {/* 结束节点 */}
        <NodeCard
          top={190} left={170}
          icon="■" iconBg="#F04438" iconColor="#fff"
          title="结束" subtitle="返回结果"
          delay={0.45}
        />

        {/* 拖拽光标 */}
        <div style={{
          position: 'absolute', top: 48, left: 310,
          animation: 'cursorFloat 3s ease-in-out infinite',
          opacity: 0.5,
        }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M5 3l14 8-6 2-4 6-4-16z" fill="#344054" stroke="#344054" strokeWidth="1" />
          </svg>
        </div>
      </div>

      {/* 底部标语 */}
      <div style={{
        position: 'absolute', bottom: 48, left: 0, right: 0,
        textAlign: 'center', color: '#98A2B3', fontSize: 13,
        letterSpacing: 1,
      }}>
        拖拽编排 · 智能协作 · 即刻部署
      </div>

      <style>{`
        @keyframes nodeFloat {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-6px); }
        }
        @keyframes cursorFloat {
          0%, 100% { transform: translate(0, 0); }
          30% { transform: translate(8px, -4px); }
          60% { transform: translate(-4px, 6px); }
        }
        @keyframes nodeEnter {
          from { opacity: 0; transform: translateY(16px) scale(0.92); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
      `}</style>
    </div>
  )
}

/* ---- 节点卡片子组件 ---- */
function NodeCard({ top, left, icon, iconBg, iconColor, title, subtitle, delay = 0, highlight = false }: {
  top: number; left: number
  icon: string; iconBg: string; iconColor: string
  title: string; subtitle: string
  delay?: number; highlight?: boolean
}) {
  return (
    <div style={{
      position: 'absolute', top, left,
      width: 120, padding: '10px 12px',
      background: highlight ? '#fff' : '#fff',
      borderRadius: 10,
      border: '1px solid #EAECF0',
      boxShadow: highlight
        ? '0 4px 16px rgba(41,112,255,0.12), 0 0 0 1.5px rgba(41,112,255,0.2)'
        : '0 2px 8px rgba(16,24,40,0.06)',
      display: 'flex', alignItems: 'center', gap: 8,
      animation: `nodeEnter 0.5s ease-out ${delay}s both, nodeFloat 4s ease-in-out ${delay}s infinite`,
      cursor: 'default',
    }}>
      <div style={{
        width: 28, height: 28, borderRadius: 7,
        background: iconBg, color: iconColor,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 14, flexShrink: 0,
      }}>
        {icon}
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 12, fontWeight: 600, color: '#101828', lineHeight: 1.2 }}>{title}</div>
        <div style={{ fontSize: 10, color: '#667085', lineHeight: 1.3 }}>{subtitle}</div>
      </div>
    </div>
  )
}
