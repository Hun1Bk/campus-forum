<template>
  <main class="auth-page">
    <section class="auth-panel">
      <h1>登录校园论坛</h1>
      <form @submit.prevent="login">
        <div class="mb-3">
          <label for="account" class="form-label">账号</label>
          <input v-model="account" type="text" class="form-control" id="account" autocomplete="username">
        </div>
        <div class="mb-3">
          <label for="password" class="form-label">密码</label>
          <input v-model="password" type="password" class="form-control" id="password" autocomplete="current-password">
        </div>
        <button type="submit" class="btn btn-primary">登录</button>
      </form>
    </section>
  </main>
</template>

<script>
import { useStore } from 'vuex'
import { ref } from "vue";
import router from "@/router/index";
import { ElMessage } from "element-plus";

export default {
  name: "LoginView",
  setup() {
    const account = ref('');
    const password = ref('');
    const store = useStore();
    const jwtToken = localStorage.getItem("jwt_token");

    if (jwtToken) {
      store.commit("updateToken", jwtToken);
      store.dispatch("getInfo", {
        success() {
          router.push({ name: 'home' });
        },
        error() {
          localStorage.removeItem("jwt_token");
        },
      });
    }

    const login = () => {
      if (!account.value.trim() || !password.value.trim()) {
        ElMessage.error("请输入账号和密码");
        return;
      }

      store.dispatch("login", {
        account: account.value,
        password: password.value,
        success() {
          store.dispatch("getInfo", {
            success() {
              ElMessage.success("登录成功");
              router.push({ name: 'home' });
            },
          });
        },
        error() {
          ElMessage.error("账号或密码错误");
        },
      });
    };

    return {
      account,
      password,
      login,
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
</style>
