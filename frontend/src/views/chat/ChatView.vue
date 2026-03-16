<template>
  <div class="chat-page">
    <div class="topbar">
      <h3>智能问答</h3>
      <div class="actions">
        <el-button v-if="auth.isAdmin" @click="$router.push('/admin/knowledge')">管理后台</el-button>
        <el-button @click="logout">退出</el-button>
      </div>
    </div>
    <el-card class="chat-card">
      <div class="messages">
        <div v-for="(item, idx) in messages" :key="idx" :class="['msg', item.role]">
          <div class="role">{{ item.role === 'user' ? '我' : '助手' }}</div>
          <div class="content">{{ item.content }}</div>
        </div>
      </div>
      <div class="composer">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="请输入你的问题"
          @keyup.enter.ctrl="send"
        />
        <div class="send-row">
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        </div>
      </div>
    </el-card>
    <el-card v-if="sources.length" class="sources-card">
      <template #header>引用来源</template>
      <div v-for="s in sources" :key="s.chunkId" class="source-item">
        <div class="name">{{ s.fileName }} / 第{{ s.page || "-" }}页</div>
        <div class="text">{{ s.content }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const question = ref("");
const loading = ref(false);
const messages = ref<{ role: "user" | "assistant"; content: string }[]>([]);
const sources = ref<any[]>([]);

async function send() {
  const q = question.value.trim();
  if (!q || loading.value) {
    return;
  }
  loading.value = true;
  question.value = "";
  messages.value.push({ role: "user", content: q });
  messages.value.push({ role: "assistant", content: "" });
  const aiIdx = messages.value.length - 1;
  sources.value = [];
  const token = localStorage.getItem("beauty_token") || "";
  try {
    const resp = await fetch("/api/chat/stream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ question: q })
    });
    if (!resp.ok || !resp.body) {
      throw new Error("请求失败");
    }
    const reader = resp.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split("\n\n");
      buffer = events.pop() || "";
      for (const evt of events) {
        const lines = evt.split("\n");
        const eventLine = lines.find((l) => l.startsWith("event:"));
        const dataLine = lines.find((l) => l.startsWith("data:"));
        if (!eventLine || !dataLine) continue;
        const event = eventLine.replace("event:", "").trim();
        const data = dataLine.replace("data:", "").trim();
        if (event === "token") {
          messages.value[aiIdx].content += data;
        } else if (event === "done") {
          try {
            sources.value = JSON.parse(data);
          } catch {
            sources.value = [];
          }
        }
      }
    }
  } catch (e: any) {
    ElMessage.error(e?.message || "问答失败");
  } finally {
    loading.value = false;
  }
}

async function logout() {
  await auth.logout();
  router.replace("/login");
}
</script>

<style scoped>
.chat-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 20px;
}
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.actions {
  display: flex;
  gap: 8px;
}
.chat-card {
  margin-top: 12px;
}
.messages {
  min-height: 280px;
  max-height: 520px;
  overflow: auto;
  margin-bottom: 16px;
}
.msg {
  margin-bottom: 12px;
}
.msg .role {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}
.msg .content {
  white-space: pre-wrap;
  line-height: 1.6;
}
.composer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.send-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
.sources-card {
  margin-top: 12px;
}
.source-item {
  padding: 10px 0;
  border-bottom: 1px solid #f1f1f1;
}
.source-item:last-child {
  border-bottom: none;
}
.name {
  font-size: 13px;
  color: #666;
}
.text {
  margin-top: 4px;
  color: #222;
}
</style>
