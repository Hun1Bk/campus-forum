<template>
  <main class="home-page">
    <section class="home-shell">
      <div class="home-header">
        <div>
          <h1>校园论坛</h1>
          <p>查看重要讨论、热门帖子和热门分区，再进入社区参与交流。</p>
        </div>
        <el-button type="primary" @click="goSpace">进入社区</el-button>
      </div>

      <div class="dashboard-grid">
        <section class="panel pinned-panel">
          <div class="panel-title">置顶讨论</div>
          <el-empty v-if="pinnedPosts.length === 0" description="暂无置顶帖" />
          <button
            v-for="post in pinnedPosts"
            :key="post.pid"
            class="post-row"
            type="button"
            @click="openPost(post)"
          >
            <span class="post-content">{{ post.title || "无标题" }}</span>
            <span class="post-meta">{{ post.sectionName }} · {{ post.authorTitle }} · 浏览 {{ post.viewCount || 1 }}</span>
          </button>
        </section>

        <section class="panel">
          <div class="panel-title">热度排行榜</div>
          <el-empty v-if="hotPosts.length === 0" description="暂无热帖" />
          <button
            v-for="(post, index) in hotPosts"
            :key="post.pid"
            class="hot-row"
            type="button"
            @click="openPost(post)"
          >
              <span class="rank">{{ index + 1 }}</span>
            <span class="hot-body">
              <span class="post-content">{{ post.title || "无标题" }}</span>
              <span class="post-meta">
                {{ post.sectionName }} · {{ post.authorTitle }} · 浏览 {{ post.viewCount || 1 }}
                <template v-if="canSeeHotScore(post)"> · 热度 {{ formatHot(post.hotScore) }}</template>
              </span>
            </span>
          </button>
        </section>

        <section class="panel section-panel">
          <div class="panel-title">热门分区</div>
          <button
            v-for="section in hotSections"
            :key="section.sid"
            class="section-row"
            type="button"
            @click="goSection(section.sid)"
          >
            <span>{{ section.name }}</span>
            <small>热度 {{ formatHot(section.hotScore) }} · {{ section.postCount || 0 }} 个帖子</small>
          </button>
        </section>
      </div>
    </section>
  </main>
</template>

<script>
import { computed, onMounted, reactive } from "vue";
import { useStore } from "vuex";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";

const API_BASE = "http://localhost:3000";
const MANAGER_ROLES = ["ADMIN", "SUPER_ADMIN", "OWNER"];

export default {
  name: "HomeView",
  setup() {
    const store = useStore();
    const pinnedPosts = reactive([]);
    const hotPosts = reactive([]);
    const hotSections = reactive([]);
    const isManager = computed(() => MANAGER_ROLES.includes(store.state.user.role));

    const authHeaders = () => {
      if (!store.state.user.token) return {};
      return { Authorization: "Bearer " + store.state.user.token };
    };

    const normalizePost = (post) => ({
      ...post,
      title: post.title || (post.content || "").slice(0, 30) || "无标题",
      viewCount: Number(post.viewCount || 1),
      hotScore: post.hotScore === undefined ? undefined : Number(post.hotScore || 0),
    });

    const loadPinnedPosts = () => {
      $.ajax({
        url: `${API_BASE}/user/post/pinned/`,
        type: "get",
        headers: authHeaders(),
        success(resp) {
          pinnedPosts.splice(0, pinnedPosts.length, ...resp.map(normalizePost));
        },
      });
    };

    const loadHotPosts = () => {
      $.ajax({
        url: `${API_BASE}/user/post/hot/`,
        type: "get",
        headers: authHeaders(),
        success(resp) {
          hotPosts.splice(0, hotPosts.length, ...resp.map(normalizePost));
        },
      });
    };

    const loadHotSections = () => {
      $.ajax({
        url: `${API_BASE}/user/section/hot/`,
        type: "get",
        success(resp) {
          hotSections.splice(0, hotSections.length, ...resp.map((section) => ({
            ...section,
            hotScore: Number(section.hotScore || 0),
            postCount: Number(section.postCount || 0),
          })));
        },
      });
    };

    const canSeeHotScore = (post) => isManager.value && post.hotScore !== undefined;
    const formatHot = (value) => Number(value || 0).toFixed(2);

    const goSpace = () => {
      router.push({ name: "space" });
    };

    const goSection = (sectionId) => {
      router.push({ name: "space", query: { sectionId } });
    };

    const openPost = (post) => {
      openRouteInNewWindow(router, { name: "post-detail", params: { pid: post.pid } });
    };

    onMounted(() => {
      loadPinnedPosts();
      loadHotPosts();
      loadHotSections();
    });

    return {
      pinnedPosts,
      hotPosts,
      hotSections,
      canSeeHotScore,
      formatHot,
      goSpace,
      goSection,
      openPost,
    };
  },
};
</script>

<style scoped>
.home-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 30px 16px 48px;
}

.home-shell {
  margin: 0 auto;
  max-width: 1180px;
}

.home-header {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  margin-bottom: 20px;
}

.home-header h1 {
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 6px;
}

.home-header p {
  color: #606266;
  margin: 0;
}

.dashboard-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
}

.panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
}

.pinned-panel {
  grid-row: span 2;
}

.panel-title {
  color: #303133;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.post-row,
.hot-row,
.section-row {
  background: transparent;
  border: 0;
  border-top: 1px solid #ebeef5;
  cursor: pointer;
  display: grid;
  gap: 5px;
  padding: 13px 0;
  text-align: left;
  width: 100%;
}

.post-row:first-of-type,
.hot-row:first-of-type,
.section-row:first-of-type {
  border-top: 0;
}

.hot-row {
  align-items: start;
  grid-template-columns: 26px minmax(0, 1fr);
}

.rank {
  background: #f56c6c;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  width: 22px;
}

.hot-body {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.post-content {
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-meta,
.section-row small {
  color: #909399;
  font-size: 13px;
}

.section-row span {
  color: #303133;
  font-weight: 700;
}

@media (max-width: 860px) {
  .home-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .pinned-panel {
    grid-row: auto;
  }
}
</style>
