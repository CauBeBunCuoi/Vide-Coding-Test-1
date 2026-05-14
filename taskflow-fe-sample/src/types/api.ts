export interface ApiError {
  error: string
  message: string
  details?: {
    fieldErrors?: Array<{ field: string; message: string }>
  }
}

export interface UserResponse {
  id: number
  username: string
  email: string
  createdAt: string
}

export interface TokenResponse {
  accessToken: string
  expiresIn: number
}
