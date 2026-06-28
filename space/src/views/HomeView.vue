<template>
  <main class="home-page">
    <section class="home-shell">
      <div class="home-header">
        <div>
          <h1>校园论坛</h1>
          <p>查看重要讨论、热门帖子和热门分区，再进入社区参与交流。</p>
        </div>
        <div class="home-actions">
          <a
            class="github-link"
            href="https://github.com/Hun1Bk/campus-forum"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="打开 GitHub 仓库 Hun1Bk/campus-forum"
          >
            <svg class="github-icon" viewBox="0 0 16 16" aria-hidden="true">
              <path
                fill="currentColor"
                d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82A7.65 7.65 0 0 1 8 3.87c.68 0 1.36.09 2 .26 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"
              />
            </svg>
            <span>GitHub</span>
          </a>
          <el-button type="primary" @click="goSpace">进入社区</el-button>
        </div>
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
import { API_BASE } from "@/config/api";

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

.home-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.github-link {
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  color: #24292f;
  display: inline-flex;
  font-weight: 700;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  text-decoration: none;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.github-link:hover {
  background: #f6f8fa;
  border-color: #bfc4cc;
  color: #0969da;
}

.github-icon {
  height: 18px;
  width: 18px;
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

  .home-actions {
    justify-content: flex-start;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .pinned-panel {
    grid-row: auto;
  }
}
</style>
