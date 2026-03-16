<template>
  <div>
    <el-upload :auto-upload="false" :show-file-list="true" :on-change="onFileChange" :limit="1">
      <template #trigger>
        <el-button>选择文件</el-button>
      </template>
      <el-button type="primary" style="margin-left: 8px" :loading="uploading" @click="submit">上传并处理</el-button>
    </el-upload>

    <el-divider />
    <el-alert v-if="taskId" :title="`任务ID: ${taskId}`" type="info" show-icon :closable="false" />
    <div v-if="task" style="margin-top: 8px">
      <div>状态: {{ task.status }}</div>
      <div>进度: {{ task.progress }}%</div>
      <div v-if="task.resultMsg">结果: {{ task.resultMsg }}</div>
      <el-button v-if="task.status === 'FAILED'" size="small" @click="retry">重试</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/api/request";

const props = defineProps<{ knowledgeId: number }>();
const fileRef = ref<File | null>(null);
const uploading = ref(false);
const taskId = ref<number | null>(null);
const task = ref<any>(null);
let timer: number | null = null;

function onFileChange(file: any) {
  fileRef.value = file.raw;
}

async function submit() {
  if (!fileRef.value) {
    ElMessage.warning("请先选择文件");
    return;
  }
  uploading.value = true;
  try {
    const form = new FormData();
    form.append("file", fileRef.value);
    form.append("knowledgeId", String(props.knowledgeId));
    form.append("fileType", "doc");
    const res: any = await request.post("/file/upload", form);
    if (res.code !== 200) throw new Error(res.message || "上传失败");
    taskId.value = res.data.taskId;
    ElMessage.success("上传成功，已进入处理队列");
    startPolling();
  } catch (e: any) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploading.value = false;
  }
}

async function fetchTask() {
  if (!taskId.value) return;
  const res: any = await request.get(`/file/task/${taskId.value}`);
  if (res.code === 200) {
    task.value = res.data;
    if (["SUCCESS", "FAILED"].includes(res.data.status) && timer) {
      window.clearInterval(timer);
      timer = null;
    }
  }
}

function startPolling() {
  if (timer) window.clearInterval(timer);
  fetchTask();
  timer = window.setInterval(fetchTask, 3000);
}

async function retry() {
  if (!taskId.value) return;
  const res: any = await request.post(`/file/task/${taskId.value}/retry`);
  if (res.code === 200) {
    ElMessage.success("已重试");
    startPolling();
  }
}
</script>
