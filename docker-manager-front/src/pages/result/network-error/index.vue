<template>
  <result title="网络异常" tip="网络异常，请检查您的网络连接是否正常" type="wifi">
    <div>
      <t-button theme="default" @click="() => $router.push('/')">返回首页</t-button>
      <t-button @click="() => $router.push('/')">重新加载</t-button>
    </div>
  </result>
</template>

<script lang="ts">
export default {
  name: 'ResultNetworkError',
};
</script>
<script setup lang="ts">
import Result from '@/components/result/index.vue';
</script>
