<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px" @change="fetchList">
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
      />
      <el-button type="primary" @click="fetchList">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column prop="fileId" label="文件ID" width="100" />
      <el-table-column prop="taskType" label="任务类型" width="140" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="进度" width="140">
        <template #default="{ row }">
          <el-progress v-if="row.status === 'PROCESSING'" :percentage="row.progress || 0" :stroke-width="6" />
          <span v-else>{{ row.progress || 0 }}%</span>
        </template>
      </el-table-column>
      <el-table-column prop="retryCount" label="重试次数" width="90" />
      <el-table-column prop="resultMsg" label="结果/错误" min-width="220" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button v-if="row.status === 'FAILED'" text type="warning" @click="retryTask(row.id)">重试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="fetchList"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import request from "@/api/request";

const list = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);
const filterStatus = ref("");
const dateRange = ref<[string, string] | null>(null);
let timer: number | null = null;

onMounted(() => {
  fetchList();
  timer = window.setInterval(fetchList, 10000);
});

onUnmounted(() => {
  if (timer) window.clearInterval(timer);
});

async function fetchList() {
  const res: any = await request.get("/file/task/list", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
  });
  if (res.code === 200) {
    list.value = res.data.records || [];
    total.value = res.data.total || 0;
  }
}

async function retryTask(taskId: number) {
  const res: any = await request.post(`/file/task/${taskId}/retry`);
  if (res.code === 200) {
    ElMessage.success("已重新提交处理");
    fetchList();
  }
}

function resetFilter() {
  filterStatus.value = "";
  dateRange.value = null;
  fetchList();
}
</script>

<style scoped>
.page { padding: 20px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
