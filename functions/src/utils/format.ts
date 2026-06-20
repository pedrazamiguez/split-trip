/**
 * Formatting utilities for FCM notification payloads.
 */

/**
 * Builds a deep link URI for in-app navigation.
 *
 * @param groupId  - The group ID
 * @param path     - Optional sub-path (e.g. "expenses/exp_123")
 * @returns Deep link string like "splittrip://groups/abc123/expenses/exp_456"
 */
export function buildDeepLink(groupId: string, path?: string): string {
  const base = `splittrip://groups/${groupId}`;
  return path ? `${base}/${path}` : base;
}
