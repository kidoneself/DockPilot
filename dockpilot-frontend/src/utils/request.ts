import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken } from './auth'

// 使用相对路径，通过nginx代理到后端
const getBaseUrl = () => {
  return '/api'  // 相对路径，nginx会代理到localhost:8080
}

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: getBaseUrl(),
  timeout: 30000, // 增加到30秒
  headers: {
    'Content-Type': 'application/json',
  },
})

// 创建专门用于文件上传的axios实例
const uploadService: AxiosInstance = axios.create({
  baseURL: getBaseUrl(),
  timeout: 300000, // 5分钟超时，适合大文件上传
  headers: {
    'Content-Type': 'multipart/form-data',
  },
})

// 请求拦截器
const setupInterceptors = (instance: AxiosInstance) => {
  instance.interceptors.request.use(
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

  instance.interceptors.response.use(
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
}

// 设置拦截器
setupInterceptors(service)
setupInterceptors(uploadService)

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
  },
  // 文件上传专用方法
  upload: <T = any>(url: string, data: FormData, config?: AxiosRequestConfig): Promise<T> => {
    return uploadService.post(url, data, {
      ...config,
      onUploadProgress: (progressEvent) => {
        if (config?.onUploadProgress) {
          config.onUploadProgress(progressEvent)
        }
      }
    })
  }
}

export default request 