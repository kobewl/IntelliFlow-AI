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
  avatar?: string;
  userRole: UserRole;
  gender?: number; // 0: 保密, 1: 男, 2: 女
  bio?: string;    // 个人简介
  membershipStartTime?: string;
  membershipEndTime?: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: number;
} 