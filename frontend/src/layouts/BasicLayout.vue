<script setup lang="ts">
import { computed } from 'vue'
import { Layout } from 'ant-design-vue'
import { RouterView, useRoute } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'

const route = useRoute()
const contentClass = computed(() => (route.meta.fullWidth ? 'basic-layout__content--full' : ''))
</script>

<template>
  <Layout class="basic-layout">
    <GlobalHeader />

    <Layout.Content class="basic-layout__content" :class="contentClass">
      <RouterView />
    </Layout.Content>

    <GlobalFooter />
  </Layout>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  min-height: 100%;
  margin: 0;
}

:global(body) {
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    'Helvetica Neue',
    Arial,
    'Noto Sans',
    sans-serif;
  background: #f5f7fb;
}

.basic-layout {
  min-height: 100vh;
  padding-bottom: 48px;
  background: #f5f7fb;
}

.basic-layout__content {
  width: 100%;
  max-width: 1200px;
  min-height: calc(100vh - 112px);
  margin: 0 auto;
  padding: 24px;
}

.basic-layout__content--full {
  max-width: none;
}

@media (max-width: 768px) {
  .basic-layout__content {
    padding: 16px;
  }
}
</style>
