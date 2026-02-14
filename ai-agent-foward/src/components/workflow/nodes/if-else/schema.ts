import { z } from 'zod';

export const ifElseNodeSchema = z.object({
  mode: z.enum(['expression', 'llm']),
  expression: z.string()
});

export type IfElseNodeSchema = z.infer<typeof ifElseNodeSchema>;
