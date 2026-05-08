<script setup>
import { House, User } from '@element-plus/icons-vue'
</script>

<template>
  <nav class="answer-tabbar">
    <RouterLink to="/answer">
      <el-icon><House /></el-icon>
      <span>首页</span>
    </RouterLink>
    <RouterLink to="/answer/mine">
      <el-icon><User /></el-icon>
      <span>我的</span>
    </RouterLink>
  </nav>
</template>
