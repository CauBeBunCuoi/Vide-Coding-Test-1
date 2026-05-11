export const TaskStatus = {
  TODO: 'TODO',
  IN_PROGRESS: 'IN_PROGRESS',
  IN_REVIEW: 'IN_REVIEW',
  DONE: 'DONE',
} as const
export type TaskStatus = (typeof TaskStatus)[keyof typeof TaskStatus]

export const TaskPriority = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  URGENT: 'URGENT',
} as const
export type TaskPriority = (typeof TaskPriority)[keyof typeof TaskPriority]

export const MemberRole = {
  OWNER: 'OWNER',
  MEMBER: 'MEMBER',
} as const
export type MemberRole = (typeof MemberRole)[keyof typeof MemberRole]
