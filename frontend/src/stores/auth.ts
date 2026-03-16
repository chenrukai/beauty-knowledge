import { defineStore } from "pinia";
import request from "@/api/request";

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  role: string;
}

interface LoginResp {
  token: string;
  tokenType: string;
  expireIn: number;
  userInfo: UserInfo;
}

interface ApiResp<T> {
  code: number;
  message: string;
  data: T;
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("beauty_token") || "",
    user: (localStorage.getItem("beauty_user")
      ? JSON.parse(localStorage.getItem("beauty_user") as string)
      : null) as UserInfo | null
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === "admin"
  },
  actions: {
    async login(username: string, password: string) {
      const res = (await request.post("/auth/login", { username, password })) as ApiResp<LoginResp>;
      if (res.code !== 200) {
        throw new Error(res.message || "登录失败");
      }
      this.token = res.data.token;
      this.user = res.data.userInfo;
      localStorage.setItem("beauty_token", this.token);
      localStorage.setItem("beauty_user", JSON.stringify(this.user));
    },
    async fetchInfo() {
      const res = (await request.get("/auth/info")) as ApiResp<UserInfo>;
      if (res.code === 200) {
        this.user = res.data;
        localStorage.setItem("beauty_user", JSON.stringify(this.user));
      }
    },
    async logout() {
      try {
        await request.post("/auth/logout");
      } finally {
        this.token = "";
        this.user = null;
        localStorage.removeItem("beauty_token");
        localStorage.removeItem("beauty_user");
      }
    }
  }
});
