import axios from "axios";

const request = axios.create({
  baseURL: "/api",
  timeout: 30000
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("beauty_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem("beauty_token");
      localStorage.removeItem("beauty_user");
      if (location.pathname !== "/login") {
        location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default request;
