import { z } from 'zod';

export const llmNodeSchema = z.object({
  model: z.string().min(1),
  prompt: z.string()
});

export type LlmNodeSchema = z.infer<typeof llmNodeSchema>;
