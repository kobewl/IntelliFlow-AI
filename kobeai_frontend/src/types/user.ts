export enum UserRole {
  NORMAL = 'NORMAL',
  VIP = 'VIP',
  SVIP = 'SVIP',
  ADMIN = 'ADMIN'
}

export interface User {
  id: number;
  username: string;
  email?: string;
  phone?: string;
  userRole: UserRole;
  membershipStartTime?: string;
  membershipEndTime?: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: number;
} 