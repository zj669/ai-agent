import { z } from 'zod';

export const endNodeSchema = z.object({
  title: z.string().min(1)
});

export type EndNodeSchema = z.infer<typeof endNodeSchema>;
