<template>
  <main class="profile-page">
    <section class="profile-shell">
      <div class="page-header">
        <div>
          <h1>个人中心</h1>
          <p>管理你的头像、用户名和登录密码，查看自己发布过的帖子。</p>
        </div>
      </div>

      <div class="profile-grid">
        <el-card shadow="never" class="profile-card">
          <div class="profile-preview">
            <img :src="previewPhoto" class="avatar" alt="avatar">
            <div>
              <div class="preview-name">{{ store.state.user.username || "未命名用户" }}</div>
              <div class="preview-tip">{{ store.state.user.title || "普通用户" }}</div>
              <div class="preview-email">登录账号：{{ store.state.user.account || "未设置账号" }}</div>
              <div class="preview-email">绑定邮箱：{{ store.state.user.email || "未绑定邮箱" }}</div>
            </div>
          </div>

          <el-divider />

          <section class="setting-block">
            <div class="block-title">修改名称</div>
            <div class="inline-form">
              <el-input v-model="username" maxlength="20" show-word-limit placeholder="请输入用户名" />
              <el-button type="primary" :loading="nameSaving" @click="saveName">确定修改名称</el-button>
            </div>
          </section>

          <el-divider />

          <section class="setting-block">
            <div class="block-title">修改头像</div>
            <div class="avatar-actions">
              <input ref="avatarInput" class="file-input" type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="handleAvatarChange">
              <el-button @click="chooseAvatar">选择本地头像</el-button>
              <el-button v-if="avatarFile" text type="danger" @click="clearSelectedAvatar">取消选择</el-button>
              <el-button type="primary" :disabled="!avatarFile" :loading="avatarSaving" @click="saveAvatar">确定更换头像</el-button>
            </div>
          </section>

          <el-divider />

          <section class="setting-block">
            <div class="block-title">登录密码</div>
            <div class="inline-form compact-actions">
              <el-button @click="accountVisible = true">修改账号</el-button>
              <el-button @click="passwordVisible = true">修改密码</el-button>
            </div>
          </section>
        </el-card>

        <el-card shadow="never" class="posts-card">
          <template #header>
            <div class="card-header">
              <span>我的帖子</span>
              <el-button text @click="loadMyPosts">刷新</el-button>
            </div>
          </template>
          <el-empty v-if="myPosts.length === 0" description="你还没有发布帖子" />
          <button
            v-for="post in myPosts"
            :key="post.pid"
            class="my-post"
            type="button"
            @click="openPost(post)"
          >
            <div class="my-post-title">
              <span>{{ post.title || "无标题" }}</span>
              <el-tag v-if="post.visibility === 'PRIVATE'" size="small" type="warning">私密帖</el-tag>
            </div>
            <p>{{ contentPreview(post.content) }}</p>
            <small>{{ post.sectionName }} · {{ post.timer }}</small>
          </button>
        </el-card>

        <el-card shadow="never" class="block-card">
          <template #header>
            <div class="card-header">
              <span>屏蔽管理</span>
              <el-button text @click="loadBlockRules">刷新</el-button>
            </div>
          </template>
          <el-empty v-if="blockRules.length === 0" description="暂无屏蔽内容" />
          <div v-for="rule in blockRules" :key="rule.bid" class="block-rule">
            <div>
              <div class="block-rule-title">{{ rule.targetType === "USER" ? "屏蔽用户" : "屏蔽分区" }}</div>
              <div class="block-rule-name">{{ rule.targetName || "未知对象" }}</div>
            </div>
            <el-button text size="small" type="danger" @click="deleteBlockRule(rule)">取消屏蔽</el-button>
          </div>
        </el-card>
      </div>
    </section>

    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="新密码">
          <el-input v-model="password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="confirmedPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closePasswordDialog">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="savePassword">确定修改密码</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="accountVisible" title="修改账号" width="460px" @closed="closeAccountDialog">
      <el-form label-position="top">
        <el-form-item label="当前绑定邮箱">
          <el-input :model-value="store.state.user.email || '未绑定邮箱'" disabled />
        </el-form-item>
        <el-form-item label="新账号">
          <el-input v-model="newAccount" maxlength="30" placeholder="4-30 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="code-row">
            <el-input v-model="accountEmailCode" maxlength="6" placeholder="6 位验证码" />
            <el-button :disabled="accountCodeSending || accountCountdown > 0" :loading="accountCodeSending" @click="sendAccountCode">
              {{ accountCountdown > 0 ? `${accountCountdown}s` : "获取验证码" }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountVisible = false">取消</el-button>
        <el-button type="primary" :loading="accountSaving" @click="saveAccount">确定修改账号</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";
import { API_BASE } from "@/config/api";

const MAX_AVATAR_SIZE = 2 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];
const ACCOUNT_PATTERN = /^[A-Za-z0-9_]{4,30}$/;

export default {
  name: "ProfileView",
  setup() {
    const store = useStore();
    const username = ref("");
    const photo = ref("");
    const avatarInput = ref(null);
    const avatarFile = ref(null);
    const avatarPreviewUrl = ref("");
    const nameSaving = ref(false);
    const avatarSaving = ref(false);
    const passwordVisible = ref(false);
    const password = ref("");
    const confirmedPassword = ref("");
    const passwordSaving = ref(false);
    const accountVisible = ref(false);
    const newAccount = ref("");
    const accountEmailCode = ref("");
    const accountCountdown = ref(0);
    const accountCodeSending = ref(false);
    const accountSaving = ref(false);
    const myPosts = reactive([]);
    const blockRules = reactive([]);
    let accountCountdownTimer = null;

    const authHeaders = () => ({
      Authorization: "Bearer " + store.state.user.token,
    });

    const fullImageUrl = (url) => {
      if (!url) return "";
      if (/^https?:\/\//i.test(url) || /^blob:/i.test(url)) return url;
      return API_BASE + url;
    };

    const previewPhoto = computed(() => avatarPreviewUrl.value || fullImageUrl(photo.value));

    const contentPreview = (value) => {
      const text = value || "";
      return text.length > 50 ? `${text.slice(0, 50)}...` : text;
    };

    const normalizePost = (post) => ({
      ...post,
      title: post.title || (post.content || "").slice(0, 30) || "无标题",
      visibility: post.visibility === "PRIVATE" ? "PRIVATE" : "PUBLIC",
    });

    const revokePreview = () => {
      if (avatarPreviewUrl.value && avatarPreviewUrl.value.startsWith("blob:")) {
        URL.revokeObjectURL(avatarPreviewUrl.value);
      }
      avatarPreviewUrl.value = "";
    };

    const fillFromStore = () => {
      username.value = store.state.user.username;
      photo.value = store.state.user.photo;
      clearSelectedAvatar(false);
    };

    const updateProfile = ({ nextUsername, nextPhoto, nextPassword = "", nextConfirmedPassword = "", loadingRef, successMessage }) => {
      loadingRef.value = true;
      store.dispatch("updateProfile", {
        username: nextUsername,
        photo: nextPhoto,
        password: nextPassword,
        confirmedPassword: nextConfirmedPassword,
        success(resp) {
          loadingRef.value = false;
          if (resp.error_message === "success") {
            username.value = resp.username;
            photo.value = resp.photo;
            ElMessage.success(successMessage);
          } else {
            ElMessage.error(resp.error_message || "保存失败");
          }
        },
        error() {
          loadingRef.value = false;
          ElMessage.error("保存失败，请重新登录后再试");
        },
      });
    };

    const saveName = () => {
      const value = username.value.trim();
      if (!value) {
        ElMessage.error("用户名不能为空");
        return;
      }
      updateProfile({
        nextUsername: value,
        nextPhoto: store.state.user.photo,
        loadingRef: nameSaving,
        successMessage: "用户名已更新",
      });
    };

    const chooseAvatar = () => {
      if (avatarInput.value) {
        avatarInput.value.click();
      }
    };

    const handleAvatarChange = (event) => {
      const file = (event.target.files || [])[0];
      if (!file) return;
      if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
        ElMessage.error("只支持 jpg、png、webp、gif 图片");
        event.target.value = "";
        return;
      }
      if (file.size > MAX_AVATAR_SIZE) {
        ElMessage.error("头像不能超过 2MB");
        event.target.value = "";
        return;
      }
      revokePreview();
      avatarFile.value = file;
      avatarPreviewUrl.value = URL.createObjectURL(file);
      event.target.value = "";
    };

    const clearSelectedAvatar = (showMessage = true) => {
      revokePreview();
      avatarFile.value = null;
      if (avatarInput.value) {
        avatarInput.value.value = "";
      }
      if (showMessage) {
        ElMessage.info("已取消选择头像");
      }
    };

    const saveAvatar = () => {
      if (!avatarFile.value) {
        ElMessage.warning("请先选择头像");
        return;
      }
      avatarSaving.value = true;
      const formData = new FormData();
      formData.append("avatar", avatarFile.value);
      $.ajax({
        url: `${API_BASE}/user/account/avatar/upload/`,
        type: "post",
        headers: authHeaders(),
        data: formData,
        processData: false,
        contentType: false,
        success(resp) {
          if (resp.error_message === "success") {
            updateProfile({
              nextUsername: store.state.user.username,
              nextPhoto: resp.url,
              loadingRef: avatarSaving,
              successMessage: "头像已更新",
            });
            clearSelectedAvatar(false);
          } else {
            avatarSaving.value = false;
            ElMessage.error(resp.error_message || "头像上传失败");
          }
        },
        error() {
          avatarSaving.value = false;
          ElMessage.error("头像上传失败");
        },
      });
    };

    const closePasswordDialog = () => {
      passwordVisible.value = false;
      password.value = "";
      confirmedPassword.value = "";
    };

    const closeAccountDialog = () => {
      newAccount.value = "";
      accountEmailCode.value = "";
    };

    const startAccountCountdown = () => {
      accountCountdown.value = 60;
      if (accountCountdownTimer) {
        clearInterval(accountCountdownTimer);
      }
      accountCountdownTimer = setInterval(() => {
        accountCountdown.value -= 1;
        if (accountCountdown.value <= 0) {
          clearInterval(accountCountdownTimer);
          accountCountdownTimer = null;
        }
      }, 1000);
    };

    const sendAccountCode = () => {
      if (accountCodeSending.value || accountCountdown.value > 0) return;
      accountCodeSending.value = true;
      $.ajax({
        url: `${API_BASE}/user/account/account/code/`,
        type: "post",
        headers: authHeaders(),
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("验证码已发送到绑定邮箱");
            startAccountCountdown();
          } else {
            ElMessage.error(resp.error_message || "验证码发送失败");
          }
        },
        error() {
          ElMessage.error("验证码发送失败");
        },
        complete() {
          accountCodeSending.value = false;
        },
      });
    };

    const saveAccount = () => {
      const value = newAccount.value.trim();
      if (!ACCOUNT_PATTERN.test(value)) {
        ElMessage.error("账号只能包含 4-30 位字母、数字或下划线");
        return;
      }
      if (!accountEmailCode.value.trim()) {
        ElMessage.error("请输入邮箱验证码");
        return;
      }
      accountSaving.value = true;
      $.ajax({
        url: `${API_BASE}/user/account/account/update/`,
        type: "post",
        headers: authHeaders(),
        data: {
          account: value,
          emailCode: accountEmailCode.value.trim(),
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("账号已更新");
            accountVisible.value = false;
            store.dispatch("getInfo", { success() {} });
          } else {
            ElMessage.error(resp.error_message || "账号修改失败");
          }
        },
        error() {
          ElMessage.error("账号修改失败");
        },
        complete() {
          accountSaving.value = false;
        },
      });
    };

    const savePassword = () => {
      if (!password.value) {
        ElMessage.error("请输入新密码");
        return;
      }
      if (password.value !== confirmedPassword.value) {
        ElMessage.error("两次密码不一致");
        return;
      }
      updateProfile({
        nextUsername: store.state.user.username,
        nextPhoto: store.state.user.photo,
        nextPassword: password.value,
        nextConfirmedPassword: confirmedPassword.value,
        loadingRef: passwordSaving,
        successMessage: "密码已更新",
      });
      closePasswordDialog();
    };

    const loadMyPosts = () => {
      $.ajax({
        url: `${API_BASE}/user/post/mine/`,
        type: "get",
        headers: authHeaders(),
        success(resp) {
          myPosts.splice(0, myPosts.length, ...resp.map(normalizePost));
        },
        error() {
          ElMessage.error("我的帖子加载失败");
        },
      });
    };

    const loadBlockRules = () => {
      $.ajax({
        url: `${API_BASE}/user/block/list/`,
        type: "get",
        headers: authHeaders(),
        success(resp) {
          blockRules.splice(0, blockRules.length, ...resp);
        },
        error() {
          ElMessage.error("屏蔽列表加载失败");
        },
      });
    };

    const deleteBlockRule = (rule) => {
      $.ajax({
        url: `${API_BASE}/user/block/delete/`,
        type: "post",
        headers: authHeaders(),
        data: {
          bid: rule.bid,
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("已取消屏蔽");
            loadBlockRules();
          } else {
            ElMessage.error(resp.error_message || "取消失败");
          }
        },
        error() {
          ElMessage.error("取消失败");
        },
      });
    };

    const openPost = (post) => {
      openRouteInNewWindow(router, { name: "post-detail", params: { pid: post.pid } });
    };

    onMounted(() => {
      fillFromStore();
      loadMyPosts();
      loadBlockRules();
    });

    onBeforeUnmount(() => {
      revokePreview();
      if (accountCountdownTimer) {
        clearInterval(accountCountdownTimer);
      }
    });

    return {
      store,
      username,
      avatarInput,
      avatarFile,
      previewPhoto,
      nameSaving,
      avatarSaving,
      passwordVisible,
      password,
      confirmedPassword,
      passwordSaving,
      accountVisible,
      newAccount,
      accountEmailCode,
      accountCountdown,
      accountCodeSending,
      accountSaving,
      myPosts,
      blockRules,
      chooseAvatar,
      handleAvatarChange,
      clearSelectedAvatar,
      saveName,
      saveAvatar,
      closePasswordDialog,
      closeAccountDialog,
      sendAccountCode,
      saveAccount,
      savePassword,
      loadMyPosts,
      loadBlockRules,
      deleteBlockRule,
      openPost,
      contentPreview,
    };
  },
};
</script>

<style scoped>
.profile-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 28px 16px 48px;
}

.profile-shell {
  margin: 0 auto;
  max-width: 1080px;
}

.page-header {
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

.profile-grid {
  align-items: start;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
}

.profile-card,
.posts-card,
.block-card {
  border-radius: 8px;
}

.profile-preview {
  align-items: center;
  display: flex;
  gap: 16px;
}

.avatar {
  border-radius: 50%;
  height: 84px;
  object-fit: cover;
  width: 84px;
}

.preview-name {
  color: #303133;
  font-size: 20px;
  font-weight: 700;
}

.preview-tip {
  color: #909399;
  margin-top: 4px;
}

.preview-email {
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
  word-break: break-all;
}

.setting-block {
  display: grid;
  gap: 10px;
}

.block-title,
.card-header {
  color: #303133;
  font-weight: 700;
}

.inline-form,
.avatar-actions,
.card-header {
  align-items: center;
  display: flex;
  gap: 10px;
}

.inline-form .el-input {
  flex: 1 1 auto;
}

.file-input {
  display: none;
}

.avatar-actions {
  flex-wrap: wrap;
}

.compact-actions {
  justify-content: flex-start;
}

.code-row {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) 120px;
  width: 100%;
}

.card-header {
  justify-content: space-between;
}

.my-post {
  background: transparent;
  border: 0;
  border-top: 1px solid #ebeef5;
  cursor: pointer;
  display: grid;
  gap: 6px;
  padding: 14px 0;
  text-align: left;
  width: 100%;
}

.my-post:first-of-type {
  border-top: 0;
}

.my-post-title {
  align-items: center;
  color: #303133;
  display: flex;
  flex-wrap: wrap;
  font-weight: 700;
  gap: 8px;
}

.my-post p {
  color: #606266;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.my-post small {
  color: #909399;
}

.block-rule {
  align-items: center;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  padding: 12px 0;
}

.block-rule:first-of-type {
  border-top: 0;
}

.block-rule-title {
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.block-rule-name {
  color: #303133;
  font-weight: 700;
  word-break: break-word;
}

@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .inline-form,
  .avatar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .inline-form .el-button,
  .avatar-actions .el-button {
    margin-left: 0;
    width: 100%;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
