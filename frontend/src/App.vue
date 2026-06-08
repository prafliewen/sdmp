<template>
  <div id="app-container">
    <header class="app-header">
      <div class="header-left">
        <h1>SDPM 工作项管理</h1>
      </div>
      <div class="header-right">
        <label>操作人：</label>
        <input v-model="userStore.operator" placeholder="输入用户名" class="user-input" />
      </div>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useUserStore } from './store/user'
const userStore = useUserStore()
</script>