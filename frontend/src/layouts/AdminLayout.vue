<template>
  <el-container style="min-height: 100vh">
    <el-aside width="220px" class="aside">
      <div class="logo">AI美业知识官</div>
      <el-menu :default-active="activePath" router>
        <el-menu-item index="/admin/knowledge">知识管理</el-menu-item>
        <el-menu-item index="/admin/category">分类管理</el-menu-item>
        <el-menu-item index="/admin/entity">实体确认</el-menu-item>
        <el-menu-item index="/admin/task">任务监控</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>{{ auth.user?.nickname || auth.user?.username }}</div>
        <div class="actions">
          <el-button @click="$router.push('/chat')">问答页</el-button>
          <el-button type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const activePath = computed(() => route.path);

async function handleLogout() {
  await auth.logout();
  router.replace("/login");
}
</script>

<style scoped>
.aside {
  border-right: 1px solid #efefef;
  background: #fff;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-weight: 700;
}
.header {
  border-bottom: 1px solid #efefef;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 8px;
}
</style>
