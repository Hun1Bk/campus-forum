<template>
  <main class="auth-page">
    <section class="auth-panel">
      <h1>登录校园论坛</h1>

      <el-tabs v-model="loginMode" stretch>
        <el-tab-pane label="密码登录" name="password">
          <form @submit.prevent="passwordLogin">
            <div class="mb-3">
              <label for="account" class="form-label">账号或邮箱</label>
              <input v-model="account" type="text" class="form-control" id="account" autocomplete="username">
            </div>
            <div class="mb-3">
              <label for="password" class="form-label">密码</label>
              <input v-model="password" type="password" class="form-control" id="password" autocomplete="current-password">
            </div>
            <button type="submit" class="btn btn-primary">登录</button>
          </form>
        </el-tab-pane>

        <el-tab-pane label="验证码登录" name="code">
          <form @submit.prevent="emailCodeLogin">
            <div class="mb-3">
              <label for="login_email" class="form-label">邮箱</label>
              <input v-model="loginEmail" type="email" class="form-control" id="login_email" autocomplete="email">
            </div>
            <div class="mb-3">
              <label for="login_code" class="form-label">邮箱验证码</label>
              <div class="code-row">
                <input v-model="loginEmailCode" type="text" class="form-control" id="login_code" maxlength="6">
                <button type="button" class="btn btn-outline-primary code-button" :disabled="loginCodeSending || loginCountdown > 0" @click="sendLoginCode">
                  {{ loginCountdown > 0 ? `${loginCountdown}s` : "获取验证码" }}
                </button>
              </div>
            </div>
            <button type="submit" class="btn btn-primary">验证码登录</button>
          </form>
        </el-tab-pane>
      </el-tabs>

      <button type="button" class="forgot-button" @click="resetVisible = true">忘记密码？</button>
    </section>

    <el-dialog v-model="resetVisible" title="找回密码" width="460px" @closed="closeResetDialog">
      <el-form label-width="92px">
        <el-form-item label="邮箱">
          <el-input v-model="resetEmail" type="email" autocomplete="email" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="dialog-code-row">
            <el-input v-model="resetEmailCode" maxlength="6" />
            <el-button :disabled="resetCodeSending || resetCountdown > 0" :loading="resetCodeSending" @click="sendResetCode">
              {{ resetCountdown > 0 ? `${resetCountdown}s` : "获取验证码" }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="resetConfirmedPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitResetPassword">重置密码</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script>
import $ from "jquery";
import { useStore } from "vuex";
import { onBeforeUnmount, ref } from "vue";
import router from "@/router/index";
import { ElMessage } from "element-plus";
import { API_BASE } from "@/config/api";

const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;

export default {
  name: "LoginView",
  setup() {
    const store = useStore();
    const loginMode = ref("password");
    const account = ref("");
    const password = ref("");
    const loginEmail = ref("");
    const loginEmailCode = ref("");
    const loginCodeSending = ref(false);
    const loginCountdown = ref(0);
    const resetVisible = ref(false);
    const resetEmail = ref("");
    const resetEmailCode = ref("");
    const resetPassword = ref("");
    const resetConfirmedPassword = ref("");
    const resetCodeSending = ref(false);
    const resetCountdown = ref(0);
    const resetSubmitting = ref(false);
    let loginTimer = null;
    let resetTimer = null;

    const jwtToken = localStorage.getItem("jwt_token");
    if (jwtToken) {
      store.commit("updateToken", jwtToken);
      store.dispatch("getInfo", {
        success() {
          router.push({ name: "home" });
        },
        error() {
          localStorage.removeItem("jwt_token");
        },
      });
    }

    const validEmail = (email) => EMAIL_PATTERN.test((email || "").trim());

    const startCountdown = (target, setter) => {
      target.value = 60;
      const timer = setInterval(() => {
        target.value -= 1;
        if (target.value <= 0) {
          clearInterval(timer);
        }
      }, 1000);
      setter(timer);
    };

    const afterLogin = () => {
      store.dispatch("getInfo", {
        success() {
          ElMessage.success("登录成功");
          router.push({ name: "home" });
        },
      });
    };

    const passwordLogin = () => {
      if (!account.value.trim() || !password.value.trim()) {
        ElMessage.error("请输入账号或邮箱和密码");
        return;
      }

      store.dispatch("login", {
        account: account.value.trim(),
        password: password.value,
        success(resp) {
          if (resp.error_message === "success") {
            afterLogin();
          } else {
            ElMessage.error(resp.error_message || "登录失败");
          }
        },
        error() {
          ElMessage.error("账号、邮箱或密码错误");
        },
      });
    };

    const sendLoginCode = () => {
      if (!validEmail(loginEmail.value)) {
        ElMessage.error("请输入正确的邮箱");
        return;
      }
      if (loginCodeSending.value || loginCountdown.value > 0) return;
      loginCodeSending.value = true;
      $.ajax({
        url: `${API_BASE}/user/account/email-login/code/`,
        type: "post",
        data: { email: loginEmail.value.trim() },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("验证码已发送");
            startCountdown(loginCountdown, (timer) => { loginTimer = timer; });
          } else {
            ElMessage.error(resp.error_message || "验证码发送失败");
          }
        },
        error() {
          ElMessage.error("验证码发送失败");
        },
        complete() {
          loginCodeSending.value = false;
        },
      });
    };

    const emailCodeLogin = () => {
      if (!validEmail(loginEmail.value)) {
        ElMessage.error("请输入正确的邮箱");
        return;
      }
      if (!loginEmailCode.value.trim()) {
        ElMessage.error("请输入邮箱验证码");
        return;
      }
      store.dispatch("emailLogin", {
        email: loginEmail.value.trim(),
        emailCode: loginEmailCode.value.trim(),
        success(resp) {
          if (resp.error_message === "success") {
            afterLogin();
          } else {
            ElMessage.error(resp.error_message || "登录失败");
          }
        },
        error() {
          ElMessage.error("验证码登录失败");
        },
      });
    };

    const sendResetCode = () => {
      if (!validEmail(resetEmail.value)) {
        ElMessage.error("请输入正确的邮箱");
        return;
      }
      if (resetCodeSending.value || resetCountdown.value > 0) return;
      resetCodeSending.value = true;
      $.ajax({
        url: `${API_BASE}/user/account/password/reset/code/`,
        type: "post",
        data: { email: resetEmail.value.trim() },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("验证码已发送");
            startCountdown(resetCountdown, (timer) => { resetTimer = timer; });
          } else {
            ElMessage.error(resp.error_message || "验证码发送失败");
          }
        },
        error() {
          ElMessage.error("验证码发送失败");
        },
        complete() {
          resetCodeSending.value = false;
        },
      });
    };

    const submitResetPassword = () => {
      if (!validEmail(resetEmail.value)) {
        ElMessage.error("请输入正确的邮箱");
        return;
      }
      if (!resetEmailCode.value.trim()) {
        ElMessage.error("请输入邮箱验证码");
        return;
      }
      if (!resetPassword.value) {
        ElMessage.error("请输入新密码");
        return;
      }
      if (resetPassword.value !== resetConfirmedPassword.value) {
        ElMessage.error("两次密码不一致");
        return;
      }
      resetSubmitting.value = true;
      $.ajax({
        url: `${API_BASE}/user/account/password/reset/`,
        type: "post",
        data: {
          email: resetEmail.value.trim(),
          emailCode: resetEmailCode.value.trim(),
          password: resetPassword.value,
          confirmedPassword: resetConfirmedPassword.value,
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("密码已重置，请重新登录");
            resetVisible.value = false;
            loginMode.value = "password";
            account.value = resetEmail.value.trim();
          } else {
            ElMessage.error(resp.error_message || "重置失败");
          }
        },
        error() {
          ElMessage.error("重置失败");
        },
        complete() {
          resetSubmitting.value = false;
        },
      });
    };

    const closeResetDialog = () => {
      resetEmailCode.value = "";
      resetPassword.value = "";
      resetConfirmedPassword.value = "";
    };

    onBeforeUnmount(() => {
      if (loginTimer) clearInterval(loginTimer);
      if (resetTimer) clearInterval(resetTimer);
    });

    return {
      loginMode,
      account,
      password,
      loginEmail,
      loginEmailCode,
      loginCodeSending,
      loginCountdown,
      resetVisible,
      resetEmail,
      resetEmailCode,
      resetPassword,
      resetConfirmedPassword,
      resetCodeSending,
      resetCountdown,
      resetSubmitting,
      passwordLogin,
      sendLoginCode,
      emailCodeLogin,
      sendResetCode,
      submitResetPassword,
      closeResetDialog,
    };
  },
};
</script>

<style scoped>
.auth-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 48px 16px;
}

.auth-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin: 0 auto;
  max-width: 460px;
  padding: 28px;
}

.auth-panel h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 22px;
}

.code-row,
.dialog-code-row {
  display: flex;
  gap: 10px;
}

.code-button,
.dialog-code-row .el-button {
  flex: 0 0 120px;
}

button.btn-primary {
  width: 100%;
}

.forgot-button {
  background: transparent;
  border: 0;
  color: #337ecc;
  display: block;
  margin: 16px auto 0;
  padding: 0;
}

@media (max-width: 520px) {
  .code-row,
  .dialog-code-row {
    flex-direction: column;
  }

  .code-button,
  .dialog-code-row .el-button {
    flex: auto;
  }
}
</style>
