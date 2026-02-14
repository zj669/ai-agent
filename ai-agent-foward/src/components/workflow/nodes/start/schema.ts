import { z } from 'zod';

export const startNodeSchema = z.object({
  title: z.string().min(1)
});

export type StartNodeSchema = z.infer<typeof startNodeSchema>;
