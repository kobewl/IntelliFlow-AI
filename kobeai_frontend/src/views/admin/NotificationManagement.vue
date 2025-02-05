<template>
  <div class="notification-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>通知管理</h2>
          <el-button type="primary" @click="handleAdd">发布通知</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="标题">
            <el-input v-model="searchForm.title" placeholder="请输入通知标题" clearable />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="searchForm.type" placeholder="请选择通知类型" clearable>
              <el-option label="系统通知" value="SYSTEM" />
              <el-option label="活动通知" value="ACTIVITY" />
              <el-option label="更新通知" value="UPDATE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 通知列表 -->
      <el-table :data="notificationList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="type" label="类型">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '已发布' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'" 
              link 
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '下线' : '发布' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 通知编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '发布通知' : '编辑通知'"
      width="700px"
    >
      <el-form
        ref="formRef"
        :model="notificationForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="notificationForm.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="notificationForm.type" placeholder="请选择通知类型">
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="活动通知" value="ACTIVITY" />
            <el-option label="更新通知" value="UPDATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="notificationForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入通知内容"
          />
        </el-form-item>
        <el-form-item label="目标用户" prop="targetUsers">
          <el-select
            v-model="notificationForm.targetUsers"
            multiple
            placeholder="请选择目标用户群体"
          >
            <el-option label="所有用户" value="ALL" />
            <el-option label="普通用户" value="USER" />
            <el-option label="VIP用户" value="VIP" />
            <el-option label="SVIP用户" value="SVIP" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
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
  title: '',
  type: ''
})

// 通知列表数据
const notificationList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref()
const notificationForm = ref({
  id: '',
  title: '',
  type: '',
  content: '',
  targetUsers: [],
  status: 'ACTIVE'
})

// 表单校验规则
const rules = {
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择通知类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入通知内容', trigger: 'blur' },
    { min: 10, message: '内容不能少于10个字符', trigger: 'blur' }
  ],
  targetUsers: [
    { required: true, message: '请选择目标用户群体', trigger: 'change' }
  ]
}

// 获取通知列表
const getNotificationList = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      title: searchForm.value.title,
      type: searchForm.value.type
    }
    const res = await adminApi.getNotificationList(params)
    if (res.code === 200) {
      notificationList.value = res.data.content
      total.value = res.data.totalElements
    }
  } catch (error) {
    console.error('获取通知列表失败:', error)
    ElMessage.error('获取通知列表失败')
  } finally {
    loading.value = false
  }
}

// 通知类型标签
const getTypeTagType = (type: string) => {
  switch (type) {
    case 'SYSTEM':
      return 'danger'
    case 'ACTIVITY':
      return 'warning'
    case 'UPDATE':
      return 'success'
    default:
      return 'info'
  }
}

// 通知类型文本
const getTypeText = (type: string) => {
  switch (type) {
    case 'SYSTEM':
      return '系统通知'
    case 'ACTIVITY':
      return '活动通知'
    case 'UPDATE':
      return '更新通知'
    default:
      return '未知类型'
  }
}

// 格式化日期
const formatDate = (date: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  getNotificationList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    title: '',
    type: ''
  }
  handleSearch()
}

// 添加通知
const handleAdd = () => {
  dialogType.value = 'add'
  notificationForm.value = {
    id: '',
    title: '',
    type: '',
    content: '',
    targetUsers: [],
    status: 'ACTIVE'
  }
  dialogVisible.value = true
}

// 编辑通知
const handleEdit = (row: any) => {
  dialogType.value = 'edit'
  notificationForm.value = { ...row }
  dialogVisible.value = true
}

// 切换通知状态
const handleToggleStatus = async (row: any) => {
  try {
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const res = await adminApi.updateNotificationStatus(row.id, newStatus)
    if (res.code === 200) {
      ElMessage.success(newStatus === 'ACTIVE' ? '通知已发布' : '通知已下线')
      getNotificationList()
    }
  } catch (error) {
    console.error('更新通知状态失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除通知
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除该通知吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await adminApi.deleteNotification(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getNotificationList()
      }
    } catch (error) {
      console.error('删除通知失败:', error)
      ElMessage.error('删除失败')
    }
  })
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    const api = dialogType.value === 'add' ? adminApi.addNotification : adminApi.updateNotification
    const res = await api(notificationForm.value)
    if (res.code === 200) {
      ElMessage.success(dialogType.value === 'add' ? '发布成功' : '更新成功')
      dialogVisible.value = false
      getNotificationList()
    }
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error('提交失败')
  }
}

// 分页相关
const handleSizeChange = (val: number) => {
  pageSize.value = val
  getNotificationList()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  getNotificationList()
}

onMounted(() => {
  getNotificationList()
})
</script>

<style scoped>
.notification-management {
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