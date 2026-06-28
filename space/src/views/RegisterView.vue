<template>
  <main class="auth-page">
    <section class="auth-panel">
      <h1>注册校园论坛</h1>
      <form @submit.prevent="register">
        <div class="mb-3">
          <label for="username" class="form-label">用户名</label>
          <input v-model="username" type="text" class="form-control" id="username" autocomplete="name" placeholder="展示名称">
        </div>
        <div class="mb-3">
          <label for="account" class="form-label">账号</label>
          <input v-model="account" type="text" class="form-control" id="account" autocomplete="username" placeholder="用于登录，4-30 位字母数字下划线">
        </div>
        <div class="mb-3">
          <label for="email" class="form-label">邮箱</label>
          <input v-model="email" type="email" class="form-control" id="email" autocomplete="email" placeholder="用于接收注册验证码">
        </div>
        <div class="mb-3">
          <label for="email_code" class="form-label">邮箱验证码</label>
          <div class="code-row">
            <input v-model="emailCode" type="text" class="form-control" id="email_code" maxlength="6" placeholder="6 位验证码">
            <button type="button" class="btn btn-outline-primary code-button" :disabled="codeSending || countdown > 0" @click="sendEmailCode">
              {{ countdown > 0 ? `${countdown}s 后重试` : "获取验证码" }}
            </button>
          </div>
        </div>
        <div class="mb-3">
          <label for="password" class="form-label">密码</label>
          <input v-model="password" type="password" class="form-control" id="password" autocomplete="new-password">
        </div>
        <div class="mb-3">
          <label for="confirm_password" class="form-label">确认密码</label>
          <input v-model="confirmedPassword" type="password" class="form-control" id="confirm_password" autocomplete="new-password">
        </div>
        <button type="submit" class="btn btn-primary">注册</button>
      </form>
    </section>
  </main>
</template>

<script>
import { onBeforeUnmount, ref } from "vue"
import $ from 'jquery'
import router from "@/router/index"
import { ElMessage } from "element-plus";

const API_BASE = "http://localhost:3000";
const EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
const ACCOUNT_PATTERN = /^[A-Za-z0-9_]{4,30}$/;

export default {
  name: "RegisterView",
  setup() {
    const username = ref('');
    const account = ref('');
    const email = ref('');
    const emailCode = ref('');
    const password = ref('');
    const confirmedPassword = ref('');
    const countdown = ref(0);
    const codeSending = ref(false);
    let countdownTimer = null;

    const startCountdown = () => {
      countdown.value = 60;
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }
      countdownTimer = setInterval(() => {
        countdown.value -= 1;
        if (countdown.value <= 0) {
          clearInterval(countdownTimer);
          countdownTimer = null;
        }
      }, 1000);
    };

    const validateEmail = () => {
      if (!EMAIL_PATTERN.test(email.value.trim())) {
        ElMessage.error("请输入正确的邮箱地址");
        return false;
      }
      return true;
    };

    const sendEmailCode = () => {
      if (!validateEmail() || codeSending.value || countdown.value > 0) return;
      codeSending.value = true;
      $.ajax({
        url: `${API_BASE}/user/account/email/code/`,
        type: 'post',
        data: {
          email: email.value.trim(),
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("验证码已发送，请查收邮箱");
            startCountdown();
          } else {
            ElMessage.error(resp.error_message || "验证码发送失败");
          }
        },
        error() {
          ElMessage.error("验证码发送失败");
        },
        complete() {
          codeSending.value = false;
        },
      });
    };

    const register = () => {
      if (!username.value.trim() || !account.value.trim() || !password.value.trim()) {
        ElMessage.error("请输入用户名、账号和密码");
        return;
      }
      if (!ACCOUNT_PATTERN.test(account.value.trim())) {
        ElMessage.error("账号只能包含 4-30 位字母、数字或下划线");
        return;
      }
      if (!validateEmail()) return;
      if (!emailCode.value.trim()) {
        ElMessage.error("请输入邮箱验证码");
        return;
      }

      $.ajax({
        url: `${API_BASE}/user/account/register/`,
        type: 'post',
        data: {
          username: username.value,
          account: account.value.trim(),
          email: email.value.trim(),
          emailCode: emailCode.value.trim(),
          password: password.value,
          confirmedPassword: confirmedPassword.value,
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("注册成功，请登录");
            router.push({ name: 'login' });
          } else {
            ElMessage.error(resp.error_message || "注册失败");
          }
        },
        error() {
          ElMessage.error("注册失败");
        },
      });
    };

    onBeforeUnmount(() => {
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }
    });

    return {
      username,
      account,
      email,
      emailCode,
      password,
      confirmedPassword,
      countdown,
      codeSending,
      sendEmailCode,
      register,
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
  max-width: 420px;
  padding: 28px;
}

.auth-panel h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 22px;
}

button {
  width: 100%;
}

.code-row {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) 130px;
}

.code-button {
  white-space: nowrap;
  width: 100%;
}

@media (max-width: 480px) {
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
