import { useEffect, useRef } from 'react'

/**
 * 登录/注册页左侧 — 等距视角工作流动画
 * 深蓝背景 + 网格点阵 + 粒子流动 + 节点微浮动
 * 纯 CSS/SVG 实现，无外部依赖
 */
export default function WorkflowAnimation() {
  const canvasRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = canvasRef.current
    if (el) el.classList.add('wf-enter')
  }, [])

  return (
    <div
      ref={canvasRef}
      style={{
        width: '100%',
        height: '100%',
        background: 'linear-gradient(160deg, #0B1A2E 0%, #0F2847 40%, #0A1628 100%)',
        position: 'relative',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {/* 背景网格点阵 — 中心清晰，四周淡出 */}
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0 }}>
        <defs>
          <pattern id="grid" width="32" height="32" patternUnits="userSpaceOnUse">
            <circle cx="16" cy="16" r="0.8" fill="rgba(100,160,255,0.3)" />
          </pattern>
          <radialGradient id="gridFade" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="white" stopOpacity="0.6" />
            <stop offset="40%" stopColor="white" stopOpacity="0.4" />
            <stop offset="100%" stopColor="white" stopOpacity="0.05" />
          </radialGradient>
          <mask id="gridMask">
            <rect width="100%" height="100%" fill="url(#gridFade)" />
          </mask>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" mask="url(#gridMask)" />
      </svg>

      {/* 中心放射状发光 */}
      <div style={{
        position: 'absolute',
        width: '70%', height: '70%',
        borderRadius: '50%',
        background: 'radial-gradient(ellipse at center, rgba(41,112,255,0.12) 0%, rgba(41,112,255,0.04) 40%, transparent 70%)',
        filter: 'blur(40px)',
        pointerEvents: 'none',
      }} />
      <div style={{
        position: 'absolute',
        width: '40%', height: '40%',
        borderRadius: '50%',
        background: 'radial-gradient(ellipse at center, rgba(122,90,248,0.08) 0%, transparent 70%)',
        filter: 'blur(25px)',
        pointerEvents: 'none',
        transform: 'translate(10%, -5%)',
      }} />

      {/* 延伸虚线 — 暗示无限延展的系统 */}
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
        <line x1="0" y1="20%" x2="25%" y2="35%" stroke="rgba(41,112,255,0.12)" strokeWidth="1" strokeDasharray="6 8" />
        <line x1="5%" y1="45%" x2="20%" y2="42%" stroke="rgba(41,112,255,0.08)" strokeWidth="1" strokeDasharray="4 10" />
        <line x1="75%" y1="65%" x2="100%" y2="80%" stroke="rgba(41,112,255,0.12)" strokeWidth="1" strokeDasharray="6 8" />
        <line x1="80%" y1="50%" x2="95%" y2="55%" stroke="rgba(41,112,255,0.08)" strokeWidth="1" strokeDasharray="4 10" />
        <line x1="10%" y1="75%" x2="28%" y2="68%" stroke="rgba(122,90,248,0.08)" strokeWidth="1" strokeDasharray="5 9" />
        <line x1="72%" y1="25%" x2="92%" y2="18%" stroke="rgba(122,90,248,0.08)" strokeWidth="1" strokeDasharray="5 9" />

        {/* 幽灵节点 */}
        <rect x="8%" y="28%" width="36" height="20" rx="5" fill="rgba(41,112,255,0.06)" stroke="rgba(41,112,255,0.1)" strokeWidth="0.5" />
        <rect x="85%" y="70%" width="36" height="20" rx="5" fill="rgba(41,112,255,0.06)" stroke="rgba(41,112,255,0.1)" strokeWidth="0.5" />
        <rect x="88%" y="25%" width="28" height="16" rx="4" fill="rgba(122,90,248,0.05)" stroke="rgba(122,90,248,0.08)" strokeWidth="0.5" />
        <rect x="5%" y="68%" width="28" height="16" rx="4" fill="rgba(122,90,248,0.05)" stroke="rgba(122,90,248,0.08)" strokeWidth="0.5" />
      </svg>

      {/* 工作流画布 — 等距视角 */}
      <div style={{
        position: 'relative',
        width: 420, height: 280,
        transform: 'perspective(800px) rotateX(6deg) rotateY(-4deg) scale(0.98)',
        transformStyle: 'preserve-3d',
      }}>
        {/* 连线 SVG */}
        <svg
          width="420" height="280"
          viewBox="0 0 420 280"
          fill="none"
          style={{ position: 'absolute', top: 0, left: 0, pointerEvents: 'none' }}
        >
          {/* 开始 → LLM */}
          <path d="M120 45 L120 75 Q120 85 130 85 L190 85" stroke="rgba(41,112,255,0.4)" strokeWidth="2" />
          {/* LLM → 条件 */}
          <path d="M320 85 L340 85 Q350 85 350 95 L350 135" stroke="rgba(41,112,255,0.4)" strokeWidth="2" />
          {/* 条件 → 结束 */}
          <path d="M310 175 L310 205 Q310 215 300 215 L260 215" stroke="rgba(41,112,255,0.4)" strokeWidth="2" />
          {/* 条件 → LLM 回环 */}
          <path d="M390 155 L400 155 Q410 155 410 145 L410 55 Q410 45 400 45 L320 45 Q310 45 310 55 L310 65" stroke="rgba(122,90,248,0.3)" strokeWidth="1.5" strokeDasharray="4 4" />

          {/* 流动粒子 */}
          <circle r="3" fill="#4DA6FF" opacity="0.9">
            <animateMotion dur="2.5s" repeatCount="indefinite" path="M120 45 L120 75 Q120 85 130 85 L190 85" />
          </circle>
          <circle r="3" fill="#4DA6FF" opacity="0.9">
            <animateMotion dur="2.5s" repeatCount="indefinite" begin="0.8s" path="M320 85 L340 85 Q350 85 350 95 L350 135" />
          </circle>
          <circle r="3" fill="#4DA6FF" opacity="0.9">
            <animateMotion dur="2s" repeatCount="indefinite" begin="1.6s" path="M310 175 L310 205 Q310 215 300 215 L260 215" />
          </circle>
          <circle r="2.5" fill="#A78BFA" opacity="0.7">
            <animateMotion dur="4s" repeatCount="indefinite" begin="2s" path="M390 155 L400 155 Q410 155 410 145 L410 55 Q410 45 400 45 L320 45 Q310 45 310 55 L310 65" />
          </circle>

          {/* 发光粒子尾迹 */}
          <circle r="6" fill="#4DA6FF" opacity="0.2" filter="blur(3px)">
            <animateMotion dur="2.5s" repeatCount="indefinite" path="M120 45 L120 75 Q120 85 130 85 L190 85" />
          </circle>
          <circle r="6" fill="#4DA6FF" opacity="0.2" filter="blur(3px)">
            <animateMotion dur="2.5s" repeatCount="indefinite" begin="0.8s" path="M320 85 L340 85 Q350 85 350 95 L350 135" />
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
          opacity: 0.4,
        }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M5 3l14 8-6 2-4 6-4-16z" fill="#94A3B8" stroke="#94A3B8" strokeWidth="1" />
          </svg>
        </div>
      </div>

      {/* 底部标语 */}
      <div style={{
        position: 'absolute', bottom: 48, left: 0, right: 0,
        textAlign: 'center', color: 'rgba(148,163,184,0.6)', fontSize: 13,
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

/* ---- 节点卡片子组件（深色主题） ---- */
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
      background: 'rgba(15,30,56,0.85)',
      backdropFilter: 'blur(8px)',
      borderRadius: 10,
      border: highlight
        ? '1px solid rgba(41,112,255,0.4)'
        : '1px solid rgba(100,160,255,0.15)',
      boxShadow: highlight
        ? '0 4px 20px rgba(41,112,255,0.2), 0 0 0 1px rgba(41,112,255,0.15)'
        : '0 2px 12px rgba(0,0,0,0.3)',
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
        <div style={{ fontSize: 12, fontWeight: 600, color: '#E2E8F0', lineHeight: 1.2 }}>{title}</div>
        <div style={{ fontSize: 10, color: '#94A3B8', lineHeight: 1.3 }}>{subtitle}</div>
      </div>
    </div>
  )
}
