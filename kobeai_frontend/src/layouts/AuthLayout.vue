<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-logo">
        <span>KobeAI</span>
      </div>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 50%, #f0f4ff 100%);
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.06);
  padding: 40px 36px;
}

.auth-logo {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo span {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 28px 20px;
    border-radius: 12px;
  }
}
</style>
