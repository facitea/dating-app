import axios from "axios";
import { clearToken, getToken, setAuthTokens } from "../utils/auth";

function resolveApiBaseUrl() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL;

  if (envBaseUrl) {
    return envBaseUrl;
  }

  if (import.meta.env.DEV) return `${window.location.origin}/api`;

  const hostname = window.location.hostname || "localhost";
  return `http://${hostname}:8082/api`;
}

const api = axios.create({
  baseURL: resolveApiBaseUrl(),
  withCredentials: true,
});

const refreshApi = axios.create({
  baseURL: resolveApiBaseUrl(),
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = getToken();
  const isAuthRequest = config.url?.startsWith("/auth/");

  if (token && !isAuthRequest && !config.url?.startsWith("/admin/")) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const canTryRefresh = status === 401
      && originalRequest
      && !originalRequest._retry
      && !originalRequest.url?.startsWith("/auth/")
      && !originalRequest.url?.startsWith("/admin/");

    if (canTryRefresh) {
      originalRequest._retry = true;
      try {
        const { data } = await refreshApi.post("/auth/refresh");
        setAuthTokens(data);
        originalRequest.headers.Authorization = `Bearer ${data.token}`;
        return api(originalRequest);
      } catch (refreshError) {
        clearToken();
        return Promise.reject(refreshError);
      }
    }

    if (status === 401 && !originalRequest?.url?.startsWith("/admin/")) {
      clearToken();
    }

    return Promise.reject(error);
  }
);

export default api;
