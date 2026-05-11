export interface ApiError {
  error: string
  message: string
  details?: {
    fieldErrors?: Array<{ field: string; message: string }>
  }
}

export interface User {}
export interface Project {}
export interface ProjectDetail {}
export interface ProjectMember {}
export interface Task {}
export interface Label {}
export interface Comment {}
