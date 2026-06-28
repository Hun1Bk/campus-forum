import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import SpaceView from "@/views/SpaceView";
import LoginView from "@/views/LoginView";
import RegisterView from "@/views/RegisterView";
import NotificationView from "@/views/NotificationView";
import ProfileView from "@/views/ProfileView";
import UserHomeView from "@/views/UserHomeView";
import PostDetailView from "@/views/PostDetailView";
import AdminView from "@/views/AdminView";
import store from '../store/index'

const managerRoles = ["ADMIN", "SUPER_ADMIN", "OWNER"];

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: '/space',
    name: 'space',
    component: SpaceView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: '/notifications',
    name: 'notifications',
    component: NotificationView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: '/users/:userId',
    name: 'user-home',
    component: UserHomeView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: '/posts/:pid',
    name: 'post-detail',
    component: PostDetailView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: '/admin',
    name: 'admin',
    component: AdminView,
    meta: {
      requestAuth: true,
      requestAdmin: true,
    }
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: {
      requestAuth: false,
    }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

function loadUserInfoIfNeeded(success, error) {
  const jwtToken = localStorage.getItem("jwt_token");
  if (!jwtToken) {
    error();
    return;
  }
  store.commit("updateToken", jwtToken);
  if (store.state.user.is_login) {
    success();
    return;
  }
  store.dispatch("getInfo", {
    success,
    error() {
      localStorage.removeItem("jwt_token");
      error();
    }
  });
}

router.beforeEach((to, from, next) => {
  if (!to.meta.requestAuth) {
    const jwtToken = localStorage.getItem("jwt_token");
    if (jwtToken && !store.state.user.is_login) {
      store.commit("updateToken", jwtToken);
      store.dispatch("getInfo", {
        success() {},
        error() {
          localStorage.removeItem("jwt_token");
        }
      });
    }
    next();
    return;
  }

  loadUserInfoIfNeeded(
    () => {
      if (to.meta.requestAdmin && !managerRoles.includes(store.state.user.role)) {
        alert("无管理员权限");
        next({ name: "home" });
      } else {
        next();
      }
    },
    () => {
      alert("请先登录");
      next({ name: "login" });
    }
  );
})

export default router
