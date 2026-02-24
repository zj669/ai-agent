/**
 * 光束动画：通过 CSS 在 SVG 边上叠加移动的光点
 * 在 SwarmGraph 的全局 CSS 中注入
 */
export const BEAM_STYLES = `
  @keyframes swarm-beam {
    0% { stroke-dashoffset: 100; }
    100% { stroke-dashoffset: 0; }
  }
  .swarm-beam-active .react-flow__edge-path {
    stroke-dasharray: 5 15;
    animation: swarm-beam 1s linear infinite;
    stroke: #722ed1;
    stroke-width: 3;
  }
`

/**
 * 给指定边添加光束动画 class
 */
export function triggerBeam(edgeId: string, durationMs = 2000) {
  const el = document.querySelector(`[data-id="${edgeId}"]`)
  if (el) {
    el.classList.add('swarm-beam-active')
    setTimeout(() => el.classList.remove('swarm-beam-active'), durationMs)
  }
}
