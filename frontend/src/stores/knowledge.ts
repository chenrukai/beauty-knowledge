import { defineStore } from "pinia";
import request from "@/api/request";

export interface CategoryNode {
  id: number;
  name: string;
  parentId: number;
  level: number;
  sort: number;
  children: CategoryNode[];
}

interface ApiResp<T> {
  code: number;
  message: string;
  data: T;
}

export const useKnowledgeStore = defineStore("knowledge", {
  state: () => ({
    categoryTree: [] as CategoryNode[]
  }),
  actions: {
    async fetchCategoryTree() {
      const res = (await request.get("/category/tree")) as ApiResp<CategoryNode[]>;
      if (res.code === 200) {
        this.categoryTree = res.data || [];
      }
    }
  }
});
