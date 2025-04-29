<template>
  <result
    title="浏览器版本过低"
    tip="您的浏览器版本过低，可能存在安全风险，建议升级浏览器"
    type="browser-incompatible"
  >
    <t-space direction="vertical">
      <t-button theme="default" @click="() => $router.push('/')">返回首页</t-button>
      <div>推荐使用以下浏览器访问</div>
      <t-space>
        <t-tooltip content="Chrome">
          <t-button shape="square" variant="text">
            <t-icon name="logo-chrome" />
          </t-button>
        </t-tooltip>
        <t-tooltip content="Firefox">
          <t-button shape="square" variant="text">
            <t-icon name="logo-firefox" />
          </t-button>
        </t-tooltip>
        <t-tooltip content="Edge">
          <t-button shape="square" variant="text">
            <t-icon name="logo-edge" />
          </t-button>
        </t-tooltip>
      </t-space>
    </t-space>
  </result>
</template>

<script lang="ts">
export default {
  name: 'ResultBrowserIncompatible',
};
</script>
<script setup lang="ts">
import Result from '@/components/result/index.vue';
</script>

<style lang="less" scoped>
.result-slot-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  color: var(--td-text-color-secondary);
  height: 80vh;
  max-height: 240px;
}

.recommend-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--td-comp-paddingTB-xl) var(--td-comp-paddingLR-xxl);
  width: 640px;
  background: var(--td-bg-color-container);
  box-shadow: 0 1px 2px rgb(0 0 0 / 10%);
  border-radius: var(--td-radius-medium);
}

.recommend-browser {
  display: flex;

  > div {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
  }

  > div + div {
    margin-left: var(--td-comp-margin-xxxl);
  }
}

.browser-icon {
  width: var(--td-comp-size-xl);
  height: var(--td-comp-size-xl);
  margin-bottom: var(--td-comp-margin-s);
}
</style>
