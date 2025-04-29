<template>
  <div class="login-wrapper">
    <login-header />

    <div class="login-container">
      <div class="title-container">
        <h1 class="title margin-no">欢迎使用</h1>
        <h1 class="title">TDesign Starter</h1>
      </div>

      <login />
      <tdesign-setting />
    </div>

    <footer class="copyright">Copyright @ 2021-2023 Tencent. All Rights Reserved</footer>
  </div>
</template>
<script lang="ts">
export default {
  name: 'LoginIndex',
};
</script>
<script setup lang="ts">
import TdesignSetting from '@/layouts/setting.vue';
import LoginHeader from './components/Header.vue';
import Login from './components/Login.vue';
</script>

<style lang="less" scoped>
@import './index.less';
</style>
