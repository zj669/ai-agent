import type { Connection } from '@xyflow/react';
import type { WorkflowValidationResult } from '../types';

export function validateConnection(connection: Connection): WorkflowValidationResult {
  if (!connection.source || !connection.target) {
    return {
      valid: false,
      reason: 'source or target missing'
    };
  }

  if (connection.source === connection.target) {
    return {
      valid: false,
      reason: 'self loop is not allowed'
    };
  }

  return { valid: true };
}
