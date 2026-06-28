<template>
  <main class="notification-page">
    <section class="notification-shell">
      <div class="page-header">
        <div>
          <h1>通知</h1>
          <p>查看点赞和评论提醒。</p>
        </div>
        <el-button type="primary" plain :disabled="notifications.length === 0" @click="readAll">全部已读</el-button>
      </div>

      <el-empty v-if="notifications.length === 0" description="暂无通知" />

      <article
        v-for="notification in notifications"
        :key="notification.nid"
        class="notification-item"
        :class="{ unread: notification.isRead === 'false' }"
        @click="openNotification(notification)"
      >
        <button type="button" class="avatar-button" @click.stop="openUser(notification.actorId)">
          <img :src="fullImageUrl(notification.actorPhoto)" class="avatar" alt="avatar">
        </button>
        <div class="notification-body">
          <div class="notification-title">
            <strong>{{ notification.actorUsername }}</strong>
            <el-tag size="small" :type="roleTagType(notification.actorRole)">{{ notification.actorTitle || '普通用户' }}</el-tag>
            <span>{{ actionText(notification.type) }}</span>
          </div>
          <div class="notification-content">{{ notification.content }}</div>
          <div class="post-preview">{{ notification.postContent }}</div>
          <div class="notification-time">{{ notification.createTime }}</div>
        </div>
        <el-tag v-if="notification.isRead === 'false'" type="danger" size="small">未读</el-tag>
      </article>
    </section>
  </main>
</template>

<script>
import { onMounted, reactive } from "vue";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";
import { API_BASE } from "@/config/api";


export default {
  name: "NotificationView",
  setup() {
    const store = useStore();
    const notifications = reactive([]);

    const authHeaders = () => ({
      Authorization: "Bearer " + store.state.user.token,
    });

    const fullImageUrl = (url) => {
      if (!url) return "";
      if (/^https?:\/\//i.test(url) || /^blob:/i.test(url)) return url;
      return API_BASE + url;
    };

    const ensureLogin = () => {
      if (store.state.user.is_login) return true;
      ElMessage.warning("请先登录");
      router.push({ name: "login" });
      return false;
    };

    const loadNotifications = () => {
      if (!ensureLogin()) return;
      $.ajax({
        url: `${API_BASE}/user/notification/list/`,
        type: "get",
        headers: authHeaders(),
        success(resp) {
          notifications.splice(0, notifications.length, ...resp);
        },
        error() {
          ElMessage.error("通知加载失败");
        },
      });
    };

    const readOne = (notification) => {
      if (notification.isRead !== "false") return;
      $.ajax({
        url: `${API_BASE}/user/notification/read/`,
        type: "post",
        headers: authHeaders(),
        data: {
          nid: notification.nid,
        },
        success(resp) {
          if (resp.error_message === "success") {
            notification.isRead = "true";
          }
        },
      });
    };

    const openNotification = (notification) => {
      readOne(notification);
      if (notification.pid) {
        openRouteInNewWindow(router, { name: "post-detail", params: { pid: notification.pid } });
      }
    };

    const openUser = (userId) => {
      if (!userId) return;
      openRouteInNewWindow(router, { name: "user-home", params: { userId } });
    };

    const readAll = () => {
      if (!ensureLogin()) return;
      $.ajax({
        url: `${API_BASE}/user/notification/readAll/`,
        type: "post",
        headers: authHeaders(),
        success(resp) {
          if (resp.error_message === "success") {
            notifications.forEach((notification) => {
              notification.isRead = "true";
            });
            ElMessage.success("已全部标记为已读");
          }
        },
      });
    };

    const actionText = (type) => {
      if (type === "like") return "点赞了你的帖子";
      if (type === "comment") return "评论了你的帖子";
      return "与你的帖子互动";
    };

    const roleTagType = (role) => {
      if (role === "OWNER") return "danger";
      if (role === "SUPER_ADMIN") return "success";
      if (role === "ADMIN") return "warning";
      return "info";
    };

    onMounted(loadNotifications);

    return {
      notifications,
      readOne,
      openNotification,
      openUser,
      fullImageUrl,
      readAll,
      actionText,
      roleTagType,
    };
  },
};
</script>

<style scoped>
.notification-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 28px 16px 48px;
}

.notification-shell {
  max-width: 820px;
  margin: 0 auto;
}

.page-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
}

.page-header p {
  color: #606266;
  margin: 0;
}

.notification-item {
  align-items: flex-start;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  gap: 14px;
  margin-bottom: 12px;
  padding: 16px;
}

.notification-item.unread {
  border-color: #f56c6c;
}

.avatar {
  border-radius: 50%;
  height: 42px;
  object-fit: cover;
  width: 42px;
}

.avatar-button {
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 0;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-title {
  color: #303133;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.notification-content {
  color: #606266;
  margin-top: 4px;
}

.post-preview {
  background: #f8f9fb;
  border-radius: 6px;
  color: #606266;
  margin-top: 8px;
  overflow: hidden;
  padding: 8px 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}

@media (max-width: 640px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
