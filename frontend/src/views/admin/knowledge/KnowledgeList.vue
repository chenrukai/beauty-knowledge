<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索标题" clearable style="width: 220px" @keyup.enter="fetchList" />
      <el-button type="primary" @click="fetchList">查询</el-button>
      <el-button @click="openCreate">新增知识</el-button>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="categoryId" label="分类ID" width="100" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="openEdit(row)">编辑</el-button>
          <el-button text size="small" @click="openFiles(row)">文件</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" /></el-form-item>
        <el-form-item label="分类ID"><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 140px">
            <el-option value="article" label="article" />
            <el-option value="pdf" label="pdf" />
            <el-option value="image" label="image" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 140px">
            <el-option :value="1" label="发布" />
            <el-option :value="0" label="草稿" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="fileDrawer" title="文件管理" size="520px">
      <FileUpload v-if="currentKnowledgeId" :knowledge-id="currentKnowledgeId" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/api/request";
import FileUpload from "./FileUpload.vue";

const list = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);
const keyword = ref("");
const dialogVisible = ref(false);
const dialogTitle = ref("新增知识");
const editId = ref<number | null>(null);
const fileDrawer = ref(false);
const currentKnowledgeId = ref<number>(0);

const form = reactive({
  title: "",
  summary: "",
  categoryId: 1,
  type: "article",
  content: "",
  status: 1
});

fetchList();

async function fetchList() {
  const res: any = await request.get("/knowledge/page", {
    params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined }
  });
  if (res.code === 200) {
    list.value = res.data.records || [];
    total.value = res.data.total || 0;
  }
}

function openCreate() {
  editId.value = null;
  dialogTitle.value = "新增知识";
  Object.assign(form, { title: "", summary: "", categoryId: 1, type: "article", content: "", status: 1 });
  dialogVisible.value = true;
}

function openEdit(row: any) {
  editId.value = row.id;
  dialogTitle.value = "编辑知识";
  Object.assign(form, {
    title: row.title,
    summary: row.summary,
    categoryId: row.categoryId || 1,
    type: row.type || "article",
    content: row.content || "",
    status: row.status ?? 1
  });
  dialogVisible.value = true;
}

async function submit() {
  const url = editId.value ? `/knowledge/${editId.value}` : "/knowledge";
  const method = editId.value ? request.put : request.post;
  const res: any = await method(url, { ...form });
  if (res.code === 200) {
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } else {
    ElMessage.error(res.message || "保存失败");
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm("确认删除该知识？", "提示", { type: "warning" });
  const res: any = await request.delete(`/knowledge/${id}`);
  if (res.code === 200) {
    ElMessage.success("删除成功");
    fetchList();
  }
}

function openFiles(row: any) {
  currentKnowledgeId.value = row.id;
  fileDrawer.value = true;
}
</script>

<style scoped>
.page { padding: 20px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
