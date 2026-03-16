<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="filterType" placeholder="实体类型" clearable style="width: 130px" @change="fetchList">
        <el-option label="成分" value="ingredient" />
        <el-option label="功效" value="effect" />
        <el-option label="产品" value="product" />
      </el-select>
      <el-select v-model="filterMethod" placeholder="抽取方式" clearable style="width: 130px" @change="fetchList">
        <el-option label="词典匹配" value="dict" />
        <el-option label="LLM抽取" value="llm" />
      </el-select>
      <el-button type="primary" @click="fetchList">查询</el-button>
      <el-button type="success" :disabled="!selectedIds.length" @click="batchAction('CONFIRM')">批量确认</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" @click="batchAction('REJECT')">批量拒绝</el-button>
      <el-tag type="warning">待确认：{{ entityStore.pendingCount }}</el-tag>
    </div>

    <el-table :data="list" border stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="46" />
      <el-table-column prop="entityName" label="实体名称" min-width="140" />
      <el-table-column prop="entityType" label="类型" width="90" />
      <el-table-column prop="extractMethod" label="抽取方式" width="110" />
      <el-table-column prop="sourceText" label="来源上下文" min-width="260" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="抽取时间" width="180" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button text size="small" type="success" @click="singleAction(row.id, 'CONFIRM')">确认</el-button>
          <el-button text size="small" type="danger" @click="singleAction(row.id, 'REJECT')">拒绝</el-button>
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
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useEntityStore } from "@/stores/entity";

const entityStore = useEntityStore();
const list = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);
const filterType = ref("");
const filterMethod = ref("");
const selectedIds = ref<number[]>([]);

onMounted(async () => {
  await entityStore.fetchPendingCount();
  fetchList();
});

async function fetchList() {
  await entityStore.fetchPendingList({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    entityType: filterType.value || undefined,
    extractMethod: filterMethod.value || undefined
  });
  list.value = entityStore.pendingList;
  total.value = entityStore.pendingTotal;
}

function onSelectionChange(rows: any[]) {
  selectedIds.value = rows.map((r) => r.id);
}

async function batchAction(action: "CONFIRM" | "REJECT") {
  if (!selectedIds.value.length) return;
  await entityStore.confirm(selectedIds.value, action);
  ElMessage.success(action === "CONFIRM" ? "已确认" : "已拒绝");
  selectedIds.value = [];
  fetchList();
}

async function singleAction(id: number, action: "CONFIRM" | "REJECT") {
  await entityStore.confirm([id], action);
  ElMessage.success(action === "CONFIRM" ? "已确认" : "已拒绝");
  fetchList();
}
</script>

<style scoped>
.page { padding: 20px; }
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
