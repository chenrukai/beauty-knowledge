import { defineStore } from "pinia";
import request from "@/api/request";

interface ApiResp<T> {
  code: number;
  message: string;
  data: T;
}

interface PageResp<T> {
  records: T[];
  total: number;
}

export const useEntityStore = defineStore("entity", {
  state: () => ({
    pendingList: [] as any[],
    pendingTotal: 0,
    pendingCount: 0
  }),
  actions: {
    async fetchPendingList(params: Record<string, any>) {
      const res = (await request.get("/entity/pending/page", { params })) as ApiResp<PageResp<any>>;
      if (res.code === 200) {
        this.pendingList = res.data.records || [];
        this.pendingTotal = res.data.total || 0;
      }
    },
    async fetchPendingCount() {
      const res = (await request.get("/entity/pending/count")) as ApiResp<number>;
      if (res.code === 200) {
        this.pendingCount = res.data || 0;
      }
    },
    async confirm(ids: number[], action: "CONFIRM" | "REJECT") {
      const res = (await request.post("/entity/pending/confirm", { ids, action })) as ApiResp<number>;
      if (res.code !== 200) {
        throw new Error(res.message || "操作失败");
      }
      await this.fetchPendingCount();
      return res.data;
    }
  }
});
