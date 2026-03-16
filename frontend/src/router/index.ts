import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const routes = [
  { path: "/login", component: () => import("@/views/login/LoginView.vue") },
  { path: "/chat", component: () => import("@/views/chat/ChatView.vue"), meta: { auth: true } },
  {
    path: "/admin",
    component: () => import("@/layouts/AdminLayout.vue"),
    meta: { auth: true, admin: true },
    redirect: "/admin/knowledge",
    children: [
      { path: "knowledge", component: () => import("@/views/admin/knowledge/KnowledgeList.vue") },
      { path: "category", component: () => import("@/views/admin/category/CategoryTree.vue") },
      { path: "entity", component: () => import("@/views/admin/entity/ExtractConfirm.vue") },
      { path: "task", component: () => import("@/views/admin/task/TaskMonitor.vue") }
    ]
  },
  { path: "/", redirect: "/chat" }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (to.path === "/login" && auth.isLogin) {
    return "/chat";
  }
  if (to.meta.auth && !auth.isLogin) {
    return "/login";
  }
  if (to.meta.auth && auth.isLogin && !auth.user) {
    await auth.fetchInfo().catch(() => undefined);
  }
  if (to.meta.admin && !auth.isAdmin) {
    return "/chat";
  }
  return true;
});

export default router;
