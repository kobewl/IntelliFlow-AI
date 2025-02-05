export enum UserRole {
  NORMAL = 'NORMAL',
  VIP = 'VIP',
  SVIP = 'SVIP',
  ADMIN = 'ADMIN'
}

export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  UNKNOWN = 'UNKNOWN'
}

export interface User {
  id: number;
  username: string;
  email?: string;
  phone?: string;
  avatar?: string;
  userRole: UserRole;
  gender?: Gender;
  bio?: string;    // 个人简介
  membershipStartTime?: string;
  membershipEndTime?: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: number;
}

// 用户角色判断辅助函数
export const isAdmin = (user?: User | null): boolean => {
  return user?.userRole === UserRole.ADMIN;
};

export const isVIP = (user?: User | null): boolean => {
  return user?.userRole === UserRole.VIP;
};

export const isSVIP = (user?: User | null): boolean => {
  return user?.userRole === UserRole.SVIP;
}; 