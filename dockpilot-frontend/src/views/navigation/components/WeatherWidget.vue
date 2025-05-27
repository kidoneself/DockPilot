<template>
  <div 
    class="weather-widget" 
    :title="`${weatherData.location} - 点击查看详情`" 
    @click="refreshWeather"
  >
    <n-icon size="20" :component="weatherData.icon" />
    <span>{{ weatherData.temperature }}</span>
    <span class="weather-location">{{ weatherData.location }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useMessage } from 'naive-ui'
import {
  SunnyOutline,
  CloudyOutline,
  RainyOutline,
  SnowOutline,
  ThunderstormOutline,
  PartlySunnyOutline
} from '@vicons/ionicons5'

const message = useMessage()

// 天气状态
const weatherData = ref({
  temperature: '22°C',
  location: '获取中...',
  icon: markRaw(SunnyOutline),
  loading: true
})

// 获取用户位置和天气
const getLocationAndWeather = async () => {
  try {
    // 获取用户位置
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords
          
          // 获取天气数据
          const weatherResponse = await fetch(
            'https://api.open-meteo.com/v1/forecast?' +
            `latitude=${latitude}&longitude=${longitude}&` +
            'current=temperature_2m,weather_code&timezone=auto'
          )
          const weatherResult = await weatherResponse.json()
          
          // 更新天气信息
          const temp = Math.round(weatherResult.current.temperature_2m)
          const weatherCode = weatherResult.current.weather_code
          
          weatherData.value = {
            temperature: `${temp}°C`,
            location: getLocationFromTimezone(weatherResult.timezone),
            icon: getWeatherIcon(weatherCode),
            loading: false
          }
        },
        () => {
          // 位置获取失败，使用默认位置（北京）
          getDefaultWeather()
        }
      )
    } else {
      // 不支持地理位置，使用默认
      getDefaultWeather()
    }
  } catch (error) {
    console.error('获取天气失败:', error)
    getDefaultWeather()
  }
}

// 获取默认天气（北京）
const getDefaultWeather = async () => {
  try {
    const response = await fetch(
      'https://api.open-meteo.com/v1/forecast?' +
      'latitude=39.9042&longitude=116.4074&' +
      'current=temperature_2m,weather_code&' +
      'timezone=Asia/Shanghai'
    )
    const result = await response.json()
    
    const temp = Math.round(result.current.temperature_2m)
    const weatherCode = result.current.weather_code
    
    weatherData.value = {
      temperature: `${temp}°C`,
      location: '北京',
      icon: getWeatherIcon(weatherCode),
      loading: false
    }
  } catch (error) {
    console.error('获取默认天气失败:', error)
    weatherData.value = {
      temperature: '22°C',
      location: '位置未知',
      icon: markRaw(SunnyOutline),
      loading: false
    }
  }
}

// 从时区获取位置名称
const getLocationFromTimezone = (timezone: string) => {
  const cityMap: Record<string, string> = {
    'Asia/Shanghai': '上海',
    'Asia/Beijing': '北京',
    'Asia/Tokyo': '东京',
    'America/New_York': '纽约',
    'America/Los_Angeles': '洛杉矶',
    'Europe/London': '伦敦',
    'Europe/Paris': '巴黎',
    'Australia/Sydney': '悉尼'
  }
  
  return cityMap[timezone] || timezone.split('/').pop()?.replace('_', ' ') || timezone
}

// 根据天气代码获取图标
const getWeatherIcon = (weatherCode: number) => {
  // WMO Weather interpretation codes (WW)
  if (weatherCode === 0) return markRaw(SunnyOutline) // 晴天
  if (weatherCode <= 3) return markRaw(PartlySunnyOutline) // 晴到多云
  if (weatherCode <= 48) return markRaw(CloudyOutline) // 雾
  if (weatherCode <= 67) return markRaw(RainyOutline) // 雨
  if (weatherCode <= 77) return markRaw(SnowOutline) // 雪
  if (weatherCode <= 82) return markRaw(RainyOutline) // 阵雨
  if (weatherCode <= 86) return markRaw(SnowOutline) // 阵雪
  if (weatherCode <= 99) return markRaw(ThunderstormOutline) // 雷暴
  
  return markRaw(SunnyOutline)
}

// 刷新天气
const refreshWeather = () => {
  console.log('天气详情:', weatherData.value)
  getLocationAndWeather() // 刷新天气
}

onMounted(() => {
  getLocationAndWeather()
})
</script>

<style scoped>
.weather-widget {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 12px;
  padding: 8px 12px;
  color: #ffffff;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.weather-widget:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.weather-location {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}
</style> 