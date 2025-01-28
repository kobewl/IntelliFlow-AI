<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-header">
        <h1>KobeAI</h1>
      </div>
      <div class="auth-content">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-bg-color-page);
}

.auth-container {
  width: 100%;
  max-width: 400px;
  padding: 40px 20px;
}

.auth-header {
  text-align: center;
  margin-bottom: 40px;
}

.auth-header h1 {
  margin: 0;
  font-size: 32px;
  color: var(--el-color-primary);
}

.auth-content {
  background-color: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 30px;
}

@media (max-width: 768px) {
  .auth-container {
    padding: 20px;
  }
  
  .auth-header {
    margin-bottom: 30px;
  }
  
  .auth-header h1 {
    font-size: 28px;
  }
  
  .auth-content {
    padding: 20px;
  }
}
</style> 