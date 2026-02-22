/**
 * 登录页右侧 3D 动画场景
 * - 使用 CSS 3D transforms 实现浮动几何体动画
 * - 检测 WebGL 可用性，为未来升级到 react-three-fiber 预留
 * - WebGL 不可用时自动降级为纯 CSS 动画（当前默认就是 CSS 实现）
 */

function isWebGLAvailable(): boolean {
  try {
    const canvas = document.createElement('canvas')
    return !!(
      window.WebGLRenderingContext &&
      (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
    )
  } catch {
    return false
  }
}

const webglSupported = typeof window !== 'undefined' && isWebGLAvailable()

export default function Auth3DScene() {
  return (
    <div className="relative flex h-full w-full items-center justify-center overflow-hidden bg-gradient-to-br from-[hsl(221,83%,45%)] via-[hsl(221,83%,53%)] to-[hsl(230,70%,60%)]">
      {/* 右侧渐变过渡 — 衔接表单区的淡蓝背景 */}
      <div className="absolute inset-y-0 right-0 z-20 w-28 bg-gradient-to-l from-[#eff4fb] to-transparent" />

      {/* 浮动几何体 */}
      <div className="pointer-events-none absolute inset-0">
        {/* 大立方体 */}
        <div className="absolute left-[15%] top-[20%] animate-float-slow" style={{ perspective: '600px' }}>
          <div className="h-20 w-20 rounded-2xl border border-white/20 bg-white/10 backdrop-blur-sm"
            style={{ transform: 'rotateX(25deg) rotateY(-35deg)' }} />
        </div>
        {/* 小立方体 */}
        <div className="absolute right-[20%] top-[15%] animate-float-mid" style={{ perspective: '600px' }}>
          <div className="h-12 w-12 rounded-xl border border-white/15 bg-white/8"
            style={{ transform: 'rotateX(-20deg) rotateY(40deg)' }} />
        </div>
        {/* 圆形 */}
        <div className="absolute bottom-[25%] left-[25%] animate-float-mid">
          <div className="h-16 w-16 rounded-full border border-white/20 bg-white/10 backdrop-blur-sm" />
        </div>
        {/* 小圆点群 */}
        <div className="absolute right-[30%] bottom-[35%] h-3 w-3 animate-float-slow rounded-full bg-white/30" />
        <div className="absolute left-[40%] top-[40%] h-2 w-2 animate-float-mid rounded-full bg-white/20" />
        <div className="absolute right-[15%] bottom-[45%] h-2.5 w-2.5 animate-float-slow rounded-full bg-white/25" />
        <div className="absolute left-[60%] top-[25%] h-2 w-2 animate-float-mid rounded-full bg-white/15" />
        {/* 菱形 */}
        <div className="absolute right-[25%] bottom-[15%] animate-float-slow" style={{ perspective: '400px' }}>
          <div className="h-14 w-14 border border-white/15 bg-white/8"
            style={{ transform: 'rotateX(45deg) rotateZ(45deg)', borderRadius: '4px' }} />
        </div>
      </div>

      {/* 品牌信息 */}
      <div className="relative z-10 text-center text-white">
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-2xl bg-white/20 text-4xl font-bold backdrop-blur-sm">
          AI
        </div>
        <h2 className="text-3xl font-bold tracking-tight">AI Agent 平台</h2>
        <p className="mt-3 text-base opacity-80">智能工作流编排，让 AI 为你工作</p>
        {!webglSupported && (
          <p className="mt-6 text-xs opacity-40">CSS 3D 模式</p>
        )}
      </div>
    </div>
  )
}
