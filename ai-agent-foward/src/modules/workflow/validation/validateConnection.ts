export type ValidateConnectionInput = {
  source: string
  target: string
}

export type ValidateConnectionResult =
  | { ok: true }
  | { ok: false; code: 'SELF_LOOP'; message: string }

export function validateConnection(input: ValidateConnectionInput): ValidateConnectionResult {
  if (input.source === input.target) {
    return {
      ok: false,
      code: 'SELF_LOOP',
      message: '不允许节点自连'
    }
  }

  return { ok: true }
}
