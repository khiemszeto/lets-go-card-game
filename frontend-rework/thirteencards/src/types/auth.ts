export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
username: string;
  balance: number;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
}

export interface RegisterResponse {
    id: number
    username: string
    balance: number
    createdAt: string
}

export interface MeResponse {
    username: string
    balance: number
}