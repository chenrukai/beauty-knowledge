import axios from "axios";

const request = axios.create({
  baseURL: "/api",
  timeout: 30000
});

request.interceptors.request.use((config) => {
  const reqUrl = config.url || "";
  const isLoginApi = reqUrl.includes("/auth/login");
  const token = localStorage.getItem("beauty_token");
  if (token && !isLoginApi) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status;
    const serverMsg = error?.response?.data?.message;
    if (status === 401) {
      localStorage.removeItem("beauty_token");
      localStorage.removeItem("beauty_user");
      if (location.pathname !== "/login") {
        location.href = "/login";
      }
    }
    return Promise.reject(new Error(serverMsg || error?.message || "请求失败"));
  }
);

export default request;
