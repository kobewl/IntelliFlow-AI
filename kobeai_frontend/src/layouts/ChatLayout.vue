<template>
  <div class="chat-layout">
    <slot />
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.chat-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--bg-color);
}
</style>
