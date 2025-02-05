<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>用户管理</h2>
          <el-button type="primary" @click="handleAdd">添加用户</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
              <el-option label="普通用户" value="NORMAL" />
              <el-option label="VIP用户" value="VIP" />
              <el-option label="SVIP用户" value="SVIP" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 用户列表 -->
      <el-table :data="userList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="userRole" label="角色">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.userRole)">{{ getRoleText(row.userRole) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleSetRole(row)">设置角色</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加用户' : '编辑用户'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="userForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 角色设置对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="设置用户角色"
      width="400px"
    >
      <el-form
        ref="roleFormRef"
        :model="roleForm"
        label-width="100px"
      >
        <el-form-item label="角色">
          <el-select v-model="roleForm.userRole">
            <el-option label="普通用户" value="NORMAL" />
            <el-option label="VIP用户" value="VIP" />
            <el-option label="SVIP用户" value="SVIP" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="会员期限" v-if="roleForm.userRole === 'VIP' || roleForm.userRole === 'SVIP'">
          <el-date-picker
            v-model="roleForm.membershipEndTime"
            type="datetime"
            placeholder="选择会员到期时间"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRoleForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { adminApi } from '../../api/admin'

// 搜索表单
const searchForm = ref({
  username: '',
  role: ''
})

// 用户列表数据
const userList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add')
const userForm = ref({
  id: '',
  username: '',
  email: '',
  phone: '',
  password: ''
})

// 角色设置对话框
const roleDialogVisible = ref(false)
const roleForm = ref({
  id: '',
  userRole: '',
  membershipEndTime: ''
})

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ]
}

// 获取用户列表
const getUserList = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      username: searchForm.value.username,
      role: searchForm.value.role
    }
    const res = await adminApi.getUserList(params)
    if (res.code === 200) {
      userList.value = res.data.content
      total.value = res.data.totalElements
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 角色标签类型
const getRoleTagType = (role: string) => {
  switch (role) {
    case 'ADMIN':
      return 'danger'
    case 'SVIP':
      return 'success'
    case 'VIP':
      return 'warning'
    default:
      return 'info'
  }
}

// 角色文本
const getRoleText = (role: string) => {
  const roleMap = {
    'NORMAL': '普通用户',
    'VIP': 'VIP会员',
    'SVIP': 'SVIP会员',
    'ADMIN': '管理员'
  }
  return roleMap[role] || '未知角色'
}

// 格式化日期
const formatDate = (date: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getUserList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    username: '',
    role: ''
  }
  handleSearch()
}

// 添加用户
const handleAdd = () => {
  dialogType.value = 'add'
  userForm.value = {
    id: '',
    username: '',
    email: '',
    phone: '',
    password: ''
  }
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row: any) => {
  dialogType.value = 'edit'
  userForm.value = {
    ...row,
    password: ''
  }
  dialogVisible.value = true
}

// 设置角色
const handleSetRole = (row: any) => {
  roleForm.value = {
    id: row.id,
    userRole: row.userRole,
    membershipEndTime: row.membershipEndTime
  }
  roleDialogVisible.value = true
}

// 删除用户
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除该用户吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await adminApi.deleteUser(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getUserList()
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败')
    }
  })
}

// 表单引用
const formRef = ref()
const roleFormRef = ref()

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (dialogType.value === 'add') {
          await adminApi.addUser(userForm.value)
          ElMessage.success('添加成功')
        } else {
          await adminApi.updateUser(Number(userForm.value.id), userForm.value)
          ElMessage.success('更新成功')
        }
        dialogVisible.value = false
        getUserList()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败')
      }
    }
  })
}

// 提交角色表单
const submitRoleForm = async () => {
  if (!roleFormRef.value) return
  await roleFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        await adminApi.setUserRole(Number(roleForm.value.id), roleForm.value)
        ElMessage.success('设置成功')
        roleDialogVisible.value = false
        getUserList()
      } catch (error) {
        console.error('设置失败:', error)
        ElMessage.error('设置失败')
      }
    }
  })
}

// 分页相关
const handleSizeChange = (val: number) => {
  pageSize.value = val
  getUserList()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  getUserList()
}

onMounted(() => {
  getUserList()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 