<template>
  <result type="error">
    <div class="result-success-title">操作失败</div>
    <div class="result-success-describe">请核对并修改以下信息后，再重新提交。</div>
    <template #actions>
      <t-space>
        <t-button theme="default" @click="() => $router.push('/dashboard/base')">返回首页</t-button>
        <t-button @click="() => $router.push('/dashboard/base')">修改</t-button>
      </t-space>
    </template>
  </result>
</template>
<script lang="ts">
export default {
  name: 'ResultFail',
};
</script>
<script setup lang="ts">
import Result from '@/components/result/index.vue';
</script>

<style lang="less" scoped>
.result-success-title {
  margin-top: var(--td-comp-margin-xxl);
  margin-bottom: 8px;
  font: var(--td-font-title-large);
  text-align: center;
  color: var(--td-text-color-primary);
}

.result-success-describe {
  margin-bottom: var(--td-comp-margin-xxl);
  font: var(--td-font-body-medium);
  text-align: center;
  color: var(--td-text-color-secondary);
}
</style>
