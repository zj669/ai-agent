import { z } from 'zod';

export const toolNodeSchema = z.object({
  toolName: z.string().min(1),
  inputTemplate: z.string()
});

export type ToolNodeSchema = z.infer<typeof toolNodeSchema>;
