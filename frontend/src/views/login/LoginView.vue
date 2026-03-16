<template>
  <div class="login-page">
    <el-card class="card">
      <h2>AI美业知识官</h2>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="loading"
          style="width: 100%"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const form = reactive({
  username: "admin",
  password: "admin"
});

async function handleLogin() {
  if (loading.value) {
    return;
  }

  if (!form.username || !form.password) {
    ElMessage({
      type: "warning",
      message: "请输入用户名和密码",
      grouping: true
    });
    return;
  }

  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    await auth.fetchInfo();
    if (auth.isAdmin) {
      router.replace("/admin/knowledge");
    } else {
      router.replace("/chat");
    }
  } catch (e: any) {
    ElMessage({
      type: "error",
      message: e?.message || "登录失败",
      grouping: true
    });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(130deg, #f5f7fa, #edf2f7);
}

.card {
  width: 360px;
}

h2 {
  margin: 0 0 16px;
}
</style>
