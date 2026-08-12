<script setup lang="ts">
import { Layout } from 'ant-design-vue'
</script>

<template>
  <Layout.Footer class="global-footer">李志福项目</Layout.Footer>
</template>

<style scoped>
.global-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9;
  height: 48px;
  padding: 0 16px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 48px;
  text-align: center;
  background: #ffffff;
  border-top: 1px solid #edf0f5;
}
</style>
