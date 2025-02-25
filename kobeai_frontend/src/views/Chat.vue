<template>
  <div class="page-container">
    <ChatContainer />
  </div>
</template>

<script setup lang="ts">
import ChatContainer from '../components/ChatContainer.vue'
</script>

<script lang="ts">
export default {
  name: 'ChatView'
}
</script>

<style scoped>
.page-container {
  height: 100vh;
  width: 100%;
  overflow: hidden;
}
</style>