<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Avatar, Button, Layout, Menu, message, Space } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import { LoginOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()
const route = useRoute()
const router = useRouter()

const originItems: MenuProps['items'] = [
  {
    key: '/',
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatHistoryManage',
    label: '对话管理',
    title: '对话管理',
  },
]

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      return Boolean(loginUser && loginUser.userRole === 'admin')
    }
    return true
  })
}

const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))
const selectedKeys = computed(() => {
  if (route.path.startsWith('/admin/chatHistory')) {
    return ['/admin/chatHistoryManage']
  }
  if (route.path.startsWith('/admin/app')) {
    return ['/admin/appManage']
  }
  if (route.path.startsWith('/admin/user')) {
    return ['/admin/userManage']
  }
  if (route.path.startsWith('/app/')) {
    return ['/']
  }
  return [route.path]
})

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key))
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<template>
  <Layout.Header class="global-header">
    <div class="global-header__inner">
      <div class="global-header__brand" @click="router.push('/')">
        <span class="global-header__logo-wrap">
          <img class="global-header__logo" src="@/assets/logo.png" alt="logo" />
        </span>
        <span class="global-header__title">AI 应用生成</span>
      </div>

      <Menu
        class="global-header__menu"
        mode="horizontal"
        :items="menuItems"
        :selected-keys="selectedKeys"
        @click="handleMenuClick"
      />

      <div class="global-header__actions">
        <div v-if="loginUserStore.loginUser.id" class="user-login-status">
          <a-dropdown>
            <Space class="user-login-status__trigger">
              <Avatar :src="loginUserStore.loginUser.userAvatar" :size="30" />
              <span class="user-login-status__name">
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </span>
            </Space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <Button v-else class="global-header__login" type="primary" @click="router.push('/user/login')">
          <template #icon>
            <LoginOutlined />
          </template>
          登录
        </Button>
      </div>
    </div>
  </Layout.Header>
</template>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 64px;
  padding: 0;
  line-height: 1;
  background:
    linear-gradient(135deg, rgba(3, 18, 38, 0.94), rgba(8, 36, 72, 0.88)),
    radial-gradient(circle at 18% 50%, rgba(54, 209, 220, 0.3), transparent 34%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(18px);
}

.global-header__inner {
  display: flex;
  align-items: center;
  gap: 22px;
  width: 100%;
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
}

.global-header__brand {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.global-header__logo-wrap {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  box-shadow: 0 8px 22px rgba(22, 119, 255, 0.18);
}

.global-header__logo {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.global-header__title {
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
  white-space: nowrap;
}

.global-header__menu {
  flex: 1 1 auto;
  min-width: 0;
  background: transparent;
  border-bottom: 0;
}

.global-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
}

.user-login-status {
  color: #ffffff;
}

.user-login-status__trigger {
  height: 40px;
  padding: 4px 12px 4px 6px;
  color: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.user-login-status__trigger:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.28);
}

.user-login-status__name {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  color: inherit;
  font-weight: 600;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.global-header__login {
  height: 38px;
  padding-inline: 18px;
  font-weight: 600;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #1677ff, #36d1dc);
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.28);
}

:deep(.ant-menu-title-content) {
  position: relative;
  z-index: 1;
}

:deep(.ant-menu-horizontal) {
  line-height: 40px;
}

:deep(.ant-menu-horizontal::before),
:deep(.ant-menu-horizontal::after) {
  display: none;
}

:deep(.ant-menu-horizontal > .ant-menu-item),
:deep(.ant-menu-horizontal > .ant-menu-submenu) {
  top: 0;
  display: inline-flex;
  align-items: center;
  height: 36px;
  margin: 0 4px;
  padding-inline: 16px;
  color: rgba(255, 255, 255, 0.68);
  font-weight: 600;
  border-radius: 999px;
  transition:
    color 0.2s ease,
    background 0.2s ease;
}

:deep(.ant-menu-horizontal > .ant-menu-item::after),
:deep(.ant-menu-horizontal > .ant-menu-submenu::after) {
  display: none;
}

:deep(.ant-menu-horizontal > .ant-menu-item:hover),
:deep(.ant-menu-horizontal > .ant-menu-submenu:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

:deep(.ant-menu-horizontal > .ant-menu-item-selected) {
  color: #ffffff;
  background: linear-gradient(135deg, #1677ff, #36d1dc);
  box-shadow: 0 8px 20px rgba(22, 119, 255, 0.26);
}

@media (max-width: 768px) {
  .global-header {
    height: auto;
  }

  .global-header__inner {
    flex-wrap: wrap;
    gap: 10px 14px;
    padding: 10px 16px;
  }

  .global-header__title {
    font-size: 16px;
  }

  .global-header__menu {
    order: 3;
    flex-basis: 100%;
  }

  :deep(.ant-menu-horizontal) {
    display: flex;
    overflow-x: auto;
  }

  :deep(.ant-menu-horizontal > .ant-menu-item),
  :deep(.ant-menu-horizontal > .ant-menu-submenu) {
    flex: 0 0 auto;
  }
}
</style>
