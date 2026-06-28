<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light forum-navbar">
    <div class="container">
      <router-link class="navbar-brand" :to="{ name: 'home' }">校园论坛</router-link>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarText">
        <span class="navbar-toggler-icon"></span>
      </button>

      <div class="collapse navbar-collapse" id="navbarText">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'home' }">首页</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'space' }">社区</router-link>
          </li>
          <li class="nav-item" v-if="isManager">
            <button type="button" class="nav-link nav-button" @click="goAdmin">管理后台</button>
          </li>
        </ul>

        <ul class="navbar-nav mb-2 mb-lg-0" v-if="!store.state.user.is_login">
          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'login' }">登录</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" :to="{ name: 'register' }">注册</router-link>
          </li>
        </ul>

        <div class="user-actions" v-else>
          <router-link class="notification-link" :to="{ name: 'notifications' }">
            <el-badge :value="notificationCount" :hidden="notificationCount === 0">
              <el-icon :size="22"><Bell /></el-icon>
            </el-badge>
          </router-link>

          <el-dropdown>
            <span class="el-dropdown-link">
              <img :src="fullImageUrl(store.state.user.photo)" class="avatar" alt="avatar">
              <span>{{ store.state.user.username }}</span>
              <el-tag size="small" :type="roleTagType(store.state.user.role)">
                {{ store.state.user.title || roleText(store.state.user.role) }}
              </el-tag>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="isManager" @click="goAdmin">管理后台</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </nav>
</template>

<script>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import { ArrowDown, Bell } from "@element-plus/icons-vue";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";

const MANAGER_ROLES = ["ADMIN", "SUPER_ADMIN", "OWNER"];
const API_BASE = "http://localhost:3000";

export default {
  name: "NavBarView",
  components: {
    ArrowDown,
    Bell,
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    const notificationCount = ref(0);
    let timer = null;

    const isManager = computed(() => MANAGER_ROLES.includes(store.state.user.role));

    const fullImageUrl = (url) => {
      if (!url) return "";
      if (/^https?:\/\//i.test(url) || /^blob:/i.test(url)) return url;
      return API_BASE + url;
    };

    const roleText = (role) => {
      if (role === "OWNER") return "站长";
      if (role === "SUPER_ADMIN") return "高级管理员";
      if (role === "ADMIN") return "管理员";
      return "普通用户";
    };

    const roleTagType = (role) => {
      if (role === "OWNER") return "danger";
      if (role === "SUPER_ADMIN") return "success";
      if (role === "ADMIN") return "warning";
      return "info";
    };

    const loadUnreadCount = () => {
      if (!store.state.user.is_login || !store.state.user.token) {
        notificationCount.value = 0;
        return;
      }

      $.ajax({
        url: "http://localhost:3000/user/notification/unread-count/",
        type: "get",
        headers: {
          Authorization: "Bearer " + store.state.user.token,
        },
        success(resp) {
          notificationCount.value = resp.cnt || 0;
        },
        error() {
          notificationCount.value = 0;
        },
      });
    };

    const goProfile = () => {
      openRouteInNewWindow(router, { name: "profile" });
    };

    const goAdmin = () => {
      openRouteInNewWindow(router, { name: "admin" });
    };

    const logout = () => {
      store.dispatch("logout");
      notificationCount.value = 0;
      router.push({ name: "login" });
    };

    onMounted(() => {
      loadUnreadCount();
      timer = setInterval(loadUnreadCount, 30000);
    });

    onBeforeUnmount(() => {
      if (timer) clearInterval(timer);
    });

    watch(
      () => [route.fullPath, store.state.user.is_login],
      () => loadUnreadCount()
    );

    return {
      store,
      notificationCount,
      isManager,
      fullImageUrl,
      roleText,
      roleTagType,
      goProfile,
      goAdmin,
      logout,
    };
  },
};
</script>

<style scoped>
.forum-navbar {
  border-bottom: 1px solid #e9ecef;
}

.navbar-brand {
  font-weight: 700;
}

.user-actions {
  align-items: center;
  display: flex;
  gap: 22px;
}

.notification-link {
  align-items: center;
  color: #303133;
  display: inline-flex;
  text-decoration: none;
}

.nav-button {
  background: transparent;
  border: 0;
}

.el-dropdown-link {
  align-items: center;
  color: var(--el-color-primary);
  cursor: pointer;
  display: flex;
  gap: 8px;
}

.avatar {
  border-radius: 50%;
  height: 40px;
  object-fit: cover;
  width: 40px;
}
</style>
