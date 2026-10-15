export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  roles: string[];
}
