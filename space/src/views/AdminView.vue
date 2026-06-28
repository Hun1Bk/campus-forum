<template>
  <main class="admin-page">
    <section class="admin-shell">
      <div class="page-header">
        <div>
          <h1>管理后台</h1>
          <p>按角色权限管理论坛用户、分区和帖子。</p>
        </div>
        <el-button @click="loadAll">刷新</el-button>
      </div>

      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane v-if="canManageUsers" label="用户管理" name="users">
          <div class="toolbar">
            <el-input v-model="userKeyword" placeholder="搜索用户名" clearable />
          </div>
          <el-table :data="filteredUsers" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="用户" min-width="210">
              <template #default="{ row }">
                <div class="user-cell" @click="openUser(row.id)">
                  <img :src="fullImageUrl(row.photo)" class="avatar" alt="avatar">
                  <span>{{ row.username }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="当前头衔" width="140">
              <template #default="{ row }">
                <el-tag :type="roleTagType(row.role)">{{ row.title || roleText(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="130">
              <template #default="{ row }">{{ roleText(row.role) }}</template>
            </el-table-column>
            <el-table-column label="专属头衔" min-width="140">
              <template #default="{ row }">{{ row.customTitle || "-" }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="560">
              <template #default="{ row }">
                <el-button v-if="canSetTitleTarget(row)" size="small" @click="openTitleDialog(row)">设置头衔</el-button>
                <el-button v-if="canManageTarget(row)" size="small" @click="resetProfile(row)">重置资料</el-button>
                <el-button
                  v-if="canManageTarget(row)"
                  size="small"
                  :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                  @click="toggleStatus(row)"
                >
                  {{ row.status === "ACTIVE" ? "封禁" : "启用" }}
                </el-button>
                <el-button v-if="isOwner && row.role !== 'USER' && row.role !== 'OWNER'" size="small" @click="setRole(row, 'USER')">
                  设为普通用户
                </el-button>
                <el-button v-if="isOwner && row.role !== 'ADMIN' && row.role !== 'OWNER'" size="small" @click="setRole(row, 'ADMIN')">
                  设为管理员
                </el-button>
                <el-button v-if="isOwner && row.role !== 'SUPER_ADMIN' && row.role !== 'OWNER'" size="small" @click="setRole(row, 'SUPER_ADMIN')">
                  设为高级管理员
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="帖子管理" name="posts">
          <el-table :data="posts" stripe>
            <el-table-column prop="pid" label="ID" width="70" />
            <el-table-column prop="username" label="作者" width="140" />
            <el-table-column prop="authorTitle" label="头衔" width="130" />
            <el-table-column prop="sectionName" label="分区" width="130" />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column label="可见性" width="110">
              <template #default="{ row }">
                <el-tag :type="row.visibility === 'PRIVATE' ? 'warning' : 'success'">
                  {{ row.visibility === "PRIVATE" ? "私密帖" : "所有人" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
            <el-table-column prop="timer" label="时间" width="170" />
            <el-table-column prop="viewCount" label="浏览" width="80" />
            <el-table-column prop="hotScore" label="热度" width="90" />
            <el-table-column prop="agreeCount" label="点赞" width="80" />
            <el-table-column prop="commentCount" label="评论" width="80" />
            <el-table-column prop="imageCount" label="图片" width="80" />
            <el-table-column label="置顶" width="90">
              <template #default="{ row }">
                <el-tag :type="row.isTop === 'true' ? 'danger' : 'info'">{{ row.isTop === "true" ? "已置顶" : "普通" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170">
              <template #default="{ row }">
                <el-button size="small" @click="togglePin(row)">
                  {{ row.isTop === "true" ? "取消置顶" : "置顶" }}
                </el-button>
                <el-button size="small" type="danger" @click="deletePost(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="canManageUsers" label="分区管理" name="sections">
          <div class="section-form">
            <el-input v-model="sectionName" placeholder="分区名称" clearable />
            <el-input v-model="sectionDescription" placeholder="分区描述" clearable />
            <el-button type="primary" @click="createSection">创建分区</el-button>
          </div>
          <el-table :data="sections" stripe>
            <el-table-column prop="sid" label="ID" width="80" />
            <el-table-column prop="name" label="名称" width="180" />
            <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" :disabled="Number(row.sid) === 1" @click="deleteSection(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="canManageUsers" label="评论管理" name="comments">
          <el-table :data="comments" stripe>
            <el-table-column prop="cid" label="ID" width="80" />
            <el-table-column prop="username" label="评论者" width="150" />
            <el-table-column prop="authorTitle" label="头衔" width="120" />
            <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
            <el-table-column prop="postContent" label="所属帖子" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="deleteComment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="titleDialogVisible" title="设置专属头衔" width="420px">
      <el-form label-width="84px">
        <el-form-item label="用户">
          <span>{{ titleForm.username }}</span>
        </el-form-item>
        <el-form-item label="专属头衔">
          <el-input
            v-model="titleForm.customTitle"
            maxlength="20"
            show-word-limit
            placeholder="留空则显示角色默认头衔"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="titleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCustomTitle">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useStore } from "vuex";
import { ElMessage, ElMessageBox } from "element-plus";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";
import { API_BASE } from "@/config/api";

const MANAGER_ROLES = ["ADMIN", "SUPER_ADMIN", "OWNER"];

export default {
  name: "AdminView",
  setup() {
    const store = useStore();
    const activeTab = ref(MANAGER_ROLES.includes(store.state.user.role) && store.state.user.role === "ADMIN" ? "posts" : "users");
    const userKeyword = ref("");
    const sectionName = ref("");
    const sectionDescription = ref("");
    const titleDialogVisible = ref(false);
    const titleForm = reactive({
      id: "",
      username: "",
      customTitle: "",
    });
    const users = reactive([]);
    const posts = reactive([]);
    const comments = reactive([]);
    const sections = reactive([]);

    const isOwner = computed(() => store.state.user.role === "OWNER");
    const canManageUsers = computed(() => ["SUPER_ADMIN", "OWNER"].includes(store.state.user.role));
    const isManager = computed(() => MANAGER_ROLES.includes(store.state.user.role));

    const fullImageUrl = (url) => {
      if (!url) return "";
      if (/^https?:\/\//i.test(url)) return url;
      return API_BASE + url;
    };

    const authHeaders = () => ({
      Authorization: "Bearer " + store.state.user.token,
    });

    const ensureManager = () => {
      if (isManager.value) {
        return true;
      }
      ElMessage.error("无管理员权限");
      router.push({ name: "home" });
      return false;
    };

    const openUser = (userId) => {
      if (!userId) return;
      openRouteInNewWindow(router, { name: "user-home", params: { userId } });
    };

    const request = (url, options = {}) => {
      $.ajax({
        url,
        type: options.type || "get",
        headers: authHeaders(),
        data: options.data || {},
        success: options.success,
        error(xhr) {
          const message = xhr.responseJSON?.message || xhr.responseJSON?.error_message || options.errorMessage || "操作失败";
          ElMessage.error(message);
        },
      });
    };

    const loadUsers = () => {
      if (!canManageUsers.value) return;
      request(`${API_BASE}/admin/users/`, {
        success(resp) {
          users.splice(0, users.length, ...resp);
        },
        errorMessage: "用户列表加载失败",
      });
    };

    const loadSections = () => {
      if (!canManageUsers.value) return;
      request(`${API_BASE}/user/section/list/`, {
        success(resp) {
          sections.splice(0, sections.length, ...resp);
        },
        errorMessage: "分区列表加载失败",
      });
    };

    const loadPosts = () => {
      request(`${API_BASE}/admin/posts/`, {
        success(resp) {
          posts.splice(0, posts.length, ...resp);
        },
        errorMessage: "帖子列表加载失败",
      });
    };

    const loadComments = () => {
      if (!canManageUsers.value) return;
      request(`${API_BASE}/admin/comments/`, {
        success(resp) {
          comments.splice(0, comments.length, ...resp.slice().reverse());
        },
        errorMessage: "评论列表加载失败",
      });
    };

    const loadAll = () => {
      if (!ensureManager()) return;
      loadPosts();
      loadUsers();
      loadSections();
      loadComments();
    };

    const filteredUsers = computed(() => {
      const keyword = userKeyword.value.trim().toLowerCase();
      if (!keyword) return users;
      return users.filter((user) => user.username.toLowerCase().includes(keyword));
    });

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

    const statusText = (status) => status === "DISABLED" ? "已封禁" : "正常";

    const canManageTarget = (user) => {
      if (!canManageUsers.value || String(user.id) === String(store.state.user.id)) return false;
      if (isOwner.value) return user.role !== "OWNER";
      return store.state.user.role === "SUPER_ADMIN" && user.role === "USER";
    };

    const canSetTitleTarget = (user) => {
      if (!canManageUsers.value) return false;
      if (isOwner.value && String(user.id) === String(store.state.user.id)) return true;
      return canManageTarget(user);
    };

    const openTitleDialog = (user) => {
      if (!canSetTitleTarget(user)) return;
      titleForm.id = user.id;
      titleForm.username = user.username;
      titleForm.customTitle = user.customTitle || "";
      titleDialogVisible.value = true;
    };

    const saveCustomTitle = () => {
      request(`${API_BASE}/admin/users/custom-title/`, {
        type: "post",
        data: {
          id: titleForm.id,
          customTitle: titleForm.customTitle.trim(),
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("专属头衔已更新");
            titleDialogVisible.value = false;
            loadUsers();
            loadPosts();
            loadComments();
            if (String(titleForm.id) === String(store.state.user.id)) {
              store.dispatch("getInfo", { success() {} });
            }
          } else {
            ElMessage.error(resp.error_message || "设置失败");
          }
        },
      });
    };

    const toggleStatus = (user) => {
      if (!canManageTarget(user)) return;
      const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
      ElMessageBox.confirm(`确认${nextStatus === "ACTIVE" ? "启用" : "封禁"}用户 ${user.username}？`, "确认操作", {
        type: "warning",
      }).then(() => {
        request(`${API_BASE}/admin/users/status/`, {
          type: "post",
          data: {
            id: user.id,
            status: nextStatus,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("用户状态已更新");
              loadUsers();
            } else {
              ElMessage.error(resp.error_message || "操作失败");
            }
          },
        });
      }).catch(() => {});
    };

    const resetProfile = (user) => {
      if (!canManageTarget(user)) return;
      ElMessageBox.confirm(`确认将 ${user.username} 的用户名和头像重置为默认？`, "重置资料", {
        type: "warning",
      }).then(() => {
        request(`${API_BASE}/admin/users/reset-profile/`, {
          type: "post",
          data: { id: user.id },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("用户资料已重置");
              loadUsers();
              loadPosts();
              loadComments();
            } else {
              ElMessage.error(resp.error_message || "重置失败");
            }
          },
        });
      }).catch(() => {});
    };

    const setRole = (user, role) => {
      if (!isOwner.value || user.role === "OWNER") return;
      ElMessageBox.confirm(`确认将 ${user.username} 设置为${roleText(role)}？`, "确认操作", {
        type: "warning",
      }).then(() => {
        request(`${API_BASE}/admin/users/role/`, {
          type: "post",
          data: {
            id: user.id,
            role,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("用户角色已更新");
              loadUsers();
            } else {
              ElMessage.error(resp.error_message || "操作失败");
            }
          },
        });
      }).catch(() => {});
    };

    const createSection = () => {
      const name = sectionName.value.trim();
      if (!name) {
        ElMessage.error("分区名称不能为空");
        return;
      }
      request(`${API_BASE}/admin/sections/create/`, {
        type: "post",
        data: {
          name,
          description: sectionDescription.value.trim(),
        },
        success(resp) {
          if (resp.error_message === "success") {
            sectionName.value = "";
            sectionDescription.value = "";
            ElMessage.success("分区已创建");
            loadSections();
          } else {
            ElMessage.error(resp.error_message || "创建失败");
          }
        },
      });
    };

    const deleteSection = (section) => {
      ElMessageBox.confirm(`删除分区后，该分区帖子会移动到“综合讨论”。确认删除 ${section.name}？`, "删除分区", {
        type: "warning",
      }).then(() => {
        request(`${API_BASE}/admin/sections/delete/`, {
          type: "post",
          data: {
            sid: section.sid,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("分区已删除");
              loadSections();
              loadPosts();
            } else {
              ElMessage.error(resp.error_message || "删除失败");
            }
          },
        });
      }).catch(() => {});
    };

    const togglePin = (post) => {
      const nextTop = post.isTop !== "true";
      request(`${API_BASE}/admin/posts/pin/`, {
        type: "post",
        data: {
          pid: post.pid,
          isTop: nextTop,
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success(nextTop ? "帖子已置顶" : "已取消置顶");
            loadPosts();
          } else {
            ElMessage.error(resp.error_message || "操作失败");
          }
        },
      });
    };

    const deletePost = (post) => {
      ElMessageBox.confirm("删除帖子会同时删除评论、点赞和相关通知，确认继续？", "删除帖子", {
        type: "warning",
      }).then(() => {
        request(`${API_BASE}/admin/posts/delete/`, {
          type: "post",
          data: {
            pid: post.pid,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("帖子已删除");
              loadPosts();
              loadComments();
            } else {
              ElMessage.error(resp.error_message || "删除失败");
            }
          },
        });
      }).catch(() => {});
    };

    const deleteComment = (comment) => {
      ElMessageBox.confirm("确认删除这条评论？", "删除评论", {
        type: "warning",
      }).then(() => {
        request(`${API_BASE}/admin/comments/delete/`, {
          type: "post",
          data: {
            cid: comment.cid,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("评论已删除");
              loadComments();
              loadPosts();
            } else {
              ElMessage.error(resp.error_message || "删除失败");
            }
          },
        });
      }).catch(() => {});
    };

    watch(canManageUsers, (value) => {
      if (!value && activeTab.value !== "posts") {
        activeTab.value = "posts";
      }
    });

    onMounted(loadAll);

    return {
      activeTab,
      userKeyword,
      sectionName,
      sectionDescription,
      titleDialogVisible,
      titleForm,
      users,
      posts,
      comments,
      sections,
      filteredUsers,
      isOwner,
      canManageUsers,
      loadAll,
      fullImageUrl,
      openUser,
      openTitleDialog,
      saveCustomTitle,
      toggleStatus,
      resetProfile,
      setRole,
      createSection,
      deleteSection,
      togglePin,
      deletePost,
      deleteComment,
      roleText,
      roleTagType,
      statusText,
      canManageTarget,
      canSetTitleTarget,
    };
  },
};
</script>

<style scoped>
.admin-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 28px 16px 48px;
}

.admin-shell {
  margin: 0 auto;
  max-width: 1320px;
}

.page-header {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
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

.admin-tabs {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 18px;
}

.toolbar,
.section-form {
  margin-bottom: 14px;
}

.toolbar {
  max-width: 320px;
}

.section-form {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(160px, 220px) minmax(220px, 1fr) auto;
}

.user-cell {
  align-items: center;
  cursor: pointer;
  display: flex;
  gap: 10px;
}

.avatar {
  border-radius: 50%;
  height: 34px;
  object-fit: cover;
  width: 34px;
}

@media (max-width: 760px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .section-form {
    grid-template-columns: 1fr;
  }
}
</style>
