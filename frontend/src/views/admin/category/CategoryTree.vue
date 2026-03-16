<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" @click="openAdd(null)">新增根分类</el-button>
    </div>
    <el-tree :data="knowledgeStore.categoryTree" :props="{ label: 'name', children: 'children' }" node-key="id" default-expand-all>
      <template #default="{ data }">
        <div class="tree-node">
          <span>{{ data.name }}</span>
          <div class="actions">
            <el-button text size="small" @click.stop="openAdd(data.id)">新增子分类</el-button>
            <el-button text size="small" @click.stop="openEdit(data)">编辑</el-button>
            <el-button text size="small" type="danger" @click.stop="handleDelete(data.id)">删除</el-button>
          </div>
        </div>
      </template>
    </el-tree>

    <el-dialog v-model="visible" :title="title" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/api/request";
import { useKnowledgeStore } from "@/stores/knowledge";

const knowledgeStore = useKnowledgeStore();
const visible = ref(false);
const title = ref("新增分类");
const editId = ref<number | null>(null);
const parentId = ref<number | null>(null);

const form = reactive({
  name: "",
  sort: 0,
  icon: "",
  description: ""
});

onMounted(() => knowledgeStore.fetchCategoryTree());

function openAdd(pid: number | null) {
  editId.value = null;
  parentId.value = pid;
  title.value = pid ? "新增子分类" : "新增根分类";
  Object.assign(form, { name: "", sort: 0, icon: "", description: "" });
  visible.value = true;
}

function openEdit(row: any) {
  editId.value = row.id;
  parentId.value = row.parentId ?? 0;
  title.value = "编辑分类";
  Object.assign(form, {
    name: row.name,
    sort: row.sort || 0,
    icon: row.icon || "",
    description: row.description || ""
  });
  visible.value = true;
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning("请输入分类名称");
    return;
  }
  const payload = { ...form, parentId: parentId.value ?? 0 };
  const res: any = editId.value
    ? await request.put(`/category/${editId.value}`, payload)
    : await request.post("/category", payload);
  if (res.code === 200) {
    ElMessage.success("操作成功");
    visible.value = false;
    knowledgeStore.fetchCategoryTree();
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm("确认删除该分类？", "提示", { type: "warning" });
  const res: any = await request.delete(`/category/${id}`);
  if (res.code === 200) {
    ElMessage.success("删除成功");
    knowledgeStore.fetchCategoryTree();
  }
}
</script>

<style scoped>
.page { padding: 20px; }
.toolbar { margin-bottom: 16px; }
.tree-node {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 8px;
}
.actions {
  opacity: 0;
  transition: opacity .2s;
}
.tree-node:hover .actions {
  opacity: 1;
}
</style>
