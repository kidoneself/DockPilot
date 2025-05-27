import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken } from './auth'

// 使用相对路径，通过nginx代理到后端
const getBaseUrl = () => {
  return '/api'  // 相对路径，nginx会代理到localhost:8080
}

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: getBaseUrl(),
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 如果code不是0，说明有错误
    if (res.code !== 0) {
      // 这里可以统一处理错误
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 封装请求方法
const request = {
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return service.get(url, config)
  },
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return service.post(url, data, config)
  },
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return service.put(url, data, config)
  },
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return service.delete(url, config)
  }
}

export default request 