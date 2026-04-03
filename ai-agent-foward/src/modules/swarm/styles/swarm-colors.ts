/** Swarm module color palette — consistent across all swarm UI components */

export const AGENT_GRADIENTS = [
  "linear-gradient(135deg, #1677ff 0%, #0958d9 100%)",
  "linear-gradient(135deg, #722ed1 0%, #5318b2 100%)",
  "linear-gradient(135deg, #13c2c2 0%, #08979c 100%)",
  "linear-gradient(135deg, #eb2f96 0%, #c41d7f 100%)",
  "linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)",
  "linear-gradient(135deg, #52c41a 0%, #389e0d 100%)",
  "linear-gradient(135deg, #2f54eb 0%, #10239e 100%)",
] as const;

export const SWARM_COLORS = {
  /** Primary blue (human user bubble) */
  primary: "#1677ff",
  primaryHover: "#0958d9",

  /** Neutral bubble backgrounds */
  humanBubble: "#1677ff",
  humanBubbleText: "#ffffff",
  agentBubble: "#ffffff",
  agentBubbleText: "#1a1a2e",

  /** Thinking bubble */
  thinkingBubble: "linear-gradient(135deg, rgba(241,245,249,0.98) 0%, rgba(226,232,240,0.94) 100%)",
  thinkingBorder: "#dbeafe",

  /** Shadows */
  humanShadow: "0 2px 8px rgba(22,119,255,0.25)",
  agentShadow: "0 1px 4px rgba(0,0,0,0.08)",

  /** Code block (dark theme) */
  codeBg: "#0d1117",
  codeBorder: "#30363d",
  codeText: "#e6edf3",

  /** Waiting/Processing states */
  waitingBg: "#f0f6ff",
  waitingBorder: "#adc6ff",
  waitingAccent: "#1677ff",
  waitingDoneBg: "#f6ffed",
  waitingDoneBorder: "#b7eb8f",
  waitingDoneAccent: "#52c41a",

  /** Tool call card */
  toolCardBg: "#fafdff",
  toolCardBorder: "#e5e7eb",
} as const;
