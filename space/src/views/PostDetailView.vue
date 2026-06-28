<template>
  <main class="detail-page">
    <section class="detail-shell">
      <el-empty v-if="loadError" :description="loadError" />
      <article v-else-if="post" class="post-card">
        <header class="post-header">
          <button type="button" class="avatar-button" @click="openUser(post.id)">
            <img :src="fullImageUrl(post.photo)" class="avatar" alt="avatar">
          </button>
          <div class="post-meta">
            <div class="identity-line">
              <button type="button" class="name-button" @click="openUser(post.id)">{{ post.username }}</button>
              <el-tag size="small" :type="roleTagType(post.authorRole)">{{ post.authorTitle }}</el-tag>
              <el-tag size="small" effect="plain">{{ post.sectionName }}</el-tag>
              <el-tag v-if="post.visibility === 'PRIVATE'" size="small" type="warning">私密帖</el-tag>
              <el-tag v-if="post.isTop" size="small" type="danger">置顶</el-tag>
            </div>
            <div class="time">发布于 {{ post.timer }}</div>
          </div>
        </header>

        <h1>{{ post.title || "无标题" }}</h1>
        <p class="content">{{ post.content }}</p>

        <div class="post-images" v-if="post.imageUrls && post.imageUrls.length">
          <el-image
            v-for="image in post.imageUrls"
            :key="image"
            class="post-image"
            :src="fullImageUrl(image)"
            :preview-src-list="fullImageUrls(post.imageUrls)"
            fit="cover"
          />
        </div>

        <div class="post-stats">
          <span>浏览 {{ post.viewCount || 1 }}</span>
          <span>点赞 {{ post.cnt || 0 }}</span>
          <span>评论 {{ flatCommentCount }}</span>
          <span v-if="canSeeHotScore(post)">热度 {{ formatHot(post.hotScore) }}</span>
        </div>

        <div class="post-actions">
          <el-button
            text
            :type="post.like ? 'primary' : 'default'"
            :icon="post.like ? StarFilled : Star"
            @click="toggleAgree"
          >
            {{ post.cnt || 0 }} 点赞
          </el-button>
        </div>
      </article>

      <section v-if="post" class="comment-card">
        <div class="comment-title">评论</div>
        <div v-if="store.state.user.is_login" class="comment-form">
          <button type="button" class="avatar-button" @click="openUser(store.state.user.id)">
            <img :src="fullImageUrl(store.state.user.photo)" class="comment-avatar" alt="avatar">
          </button>
          <el-input v-model="newComment" type="textarea" rows="2" placeholder="写下你的评论..." />
          <el-button type="primary" plain @click="writeComment()">提交评论</el-button>
        </div>
        <el-alert v-else title="登录后可以参与评论" type="info" :closable="false" class="comment-login" />

        <el-empty v-if="comments.length === 0" description="暂无评论" />
        <div v-for="comment in visibleComments" :key="comment.cid" class="comment-item">
          <button type="button" class="avatar-button" @click="openUser(comment.id)">
            <img :src="fullImageUrl(comment.photo)" class="comment-avatar" alt="avatar">
          </button>
          <div class="comment-main">
            <div class="identity-line compact">
              <button type="button" class="name-button comment-user" @click="openUser(comment.id)">{{ comment.username }}</button>
              <el-tag size="small" :type="roleTagType(comment.authorRole)">{{ comment.authorTitle || "普通用户" }}</el-tag>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <el-button v-if="store.state.user.is_login" text size="small" @click="toggleReply(comment)">回复</el-button>
              <el-button v-if="canDeleteComment(comment)" text size="small" type="danger" @click="deleteComment(comment)">删除</el-button>
            </div>
            <div v-if="activeReplyKey === comment.cid" class="reply-form">
              <el-input v-model="replyInputs[comment.cid]" size="small" placeholder="写下你的回复..." />
              <el-button size="small" type="primary" @click="writeComment(comment)">发送</el-button>
            </div>

            <div v-for="reply in comment.replies" :key="reply.cid" class="reply-item">
              <button type="button" class="avatar-button" @click="openUser(reply.id)">
                <img :src="fullImageUrl(reply.photo)" class="comment-avatar small" alt="avatar">
              </button>
              <div class="comment-main">
                <div class="identity-line compact">
                  <button type="button" class="name-button comment-user" @click="openUser(reply.id)">{{ reply.username }}</button>
                  <el-tag size="small" :type="roleTagType(reply.authorRole)">{{ reply.authorTitle || "普通用户" }}</el-tag>
                  <span v-if="reply.replyToUsername" class="reply-to">回复 {{ reply.replyToUsername }}</span>
                </div>
                <div class="comment-content">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button v-if="store.state.user.is_login" text size="small" @click="toggleReply(reply)">回复</el-button>
                  <el-button v-if="canDeleteComment(reply)" text size="small" type="danger" @click="deleteComment(reply)">删除</el-button>
                </div>
                <div v-if="activeReplyKey === reply.cid" class="reply-form">
                  <el-input v-model="replyInputs[reply.cid]" size="small" placeholder="写下你的回复..." />
                  <el-button size="small" type="primary" @click="writeComment(reply)">发送</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="commentLimit < comments.length" class="load-tip">继续向下滚动加载更多评论</div>
      </section>
    </section>
  </main>
</template>

<script>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import { Star, StarFilled } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";
import { API_BASE } from "@/config/api";

const MANAGER_ROLES = ["ADMIN", "SUPER_ADMIN", "OWNER"];

export default {
  name: "PostDetailView",
  setup() {
    const route = useRoute();
    const store = useStore();
    const post = ref(null);
    const comments = reactive([]);
    const newComment = ref("");
    const replyInputs = reactive({});
    const activeReplyKey = ref("");
    const commentLimit = ref(10);
    const loadError = ref("");

    const isManager = computed(() => MANAGER_ROLES.includes(store.state.user.role));
    const visibleComments = computed(() => comments.slice(0, commentLimit.value));
    const flatCommentCount = computed(() => comments.reduce((sum, comment) => sum + 1 + (comment.replies || []).length, 0));

    const authHeaders = () => ({ Authorization: "Bearer " + store.state.user.token });
    const optionalAuthHeaders = () => (store.state.user.token ? authHeaders() : {});

    const fullImageUrl = (url) => {
      if (!url) return "";
      if (/^https?:\/\//i.test(url) || /^blob:/i.test(url)) return url;
      return API_BASE + url;
    };
    const fullImageUrls = (urls) => (urls || []).map(fullImageUrl);

    const normalizeImageUrls = (value) => {
      if (Array.isArray(value)) return value;
      if (!value) return [];
      try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed : [];
      } catch (e) {
        return [];
      }
    };

    const normalizePost = (value) => ({
      ...value,
      cnt: Number(value.agreeCount || value.cnt || 0),
      imageUrls: normalizeImageUrls(value.imageUrls),
      viewCount: Number(value.viewCount || 1),
      hotScore: value.hotScore === undefined ? undefined : Number(value.hotScore || 0),
      isTop: value.isTop === true || value.isTop === "true",
      visibility: value.visibility === "PRIVATE" ? "PRIVATE" : "PUBLIC",
    });

    const normalizeComments = (items) => {
      const map = {};
      const roots = [];
      (items || []).forEach((comment) => {
        const item = {
          ...comment,
          cid: String(comment.cid || ""),
          id: String(comment.id || ""),
          parentId: comment.parentId ? String(comment.parentId) : "",
          replies: [],
        };
        map[item.cid] = item;
      });
      Object.values(map).forEach((comment) => {
        if (comment.parentId && map[comment.parentId]) {
          map[comment.parentId].replies.push(comment);
        } else {
          roots.push(comment);
        }
      });
      return roots;
    };

    const roleTagType = (role) => {
      if (role === "OWNER") return "danger";
      if (role === "SUPER_ADMIN") return "success";
      if (role === "ADMIN") return "warning";
      return "info";
    };
    const canSeeHotScore = (item) => isManager.value && item.hotScore !== undefined;
    const formatHot = (value) => Number(value || 0).toFixed(2);

    const loadPost = () => {
      loadError.value = "";
      $.ajax({
        url: `${API_BASE}/user/post/detail/`,
        type: "get",
        headers: optionalAuthHeaders(),
        data: { pid: route.params.pid },
        success(resp) {
          if (resp.error_message === "success") {
            post.value = normalizePost(resp);
            loadComments();
            loadLikeState();
          } else {
            loadPostFallback(resp.error_message || "帖子加载失败");
          }
        },
        error() {
          loadPostFallback("帖子加载失败");
        },
      });
    };

    const loadPostFallback = (message) => {
      $.ajax({
        url: `${API_BASE}/user/post/get/`,
        type: "get",
        headers: optionalAuthHeaders(),
        success(resp) {
          const target = (resp || []).find((item) => String(item.pid) === String(route.params.pid));
          if (!target) {
            loadError.value = message;
            return;
          }
          post.value = normalizePost(target);
          recordViewFallback();
          loadComments();
          loadLikeState();
        },
        error() {
          loadError.value = message;
        },
      });
    };

    const recordViewFallback = () => {
      if (!post.value) return;
      $.ajax({
        url: `${API_BASE}/user/post/view/`,
        type: "post",
        headers: optionalAuthHeaders(),
        data: { pid: post.value.pid },
        success(resp) {
          if (resp.error_message === "success") {
            post.value.viewCount = Number(resp.viewCount || post.value.viewCount || 1);
            if (resp.hotScore !== undefined) {
              post.value.hotScore = Number(resp.hotScore || 0);
            }
          }
        },
      });
    };

    const loadComments = () => {
      $.ajax({
        url: `${API_BASE}/user/post/getComment/`,
        type: "get",
        data: { pid: route.params.pid },
        success(resp) {
          comments.splice(0, comments.length, ...normalizeComments(resp));
        },
      });
    };

    const loadLikeState = () => {
      if (!store.state.user.id || !post.value) return;
      $.ajax({
        url: `${API_BASE}/user/agree/get/`,
        type: "get",
        data: { id: store.state.user.id },
        success(resp) {
          post.value.like = Boolean(resp[post.value.pid]);
        },
      });
    };

    const requireLogin = () => {
      if (store.state.user.is_login) return true;
      ElMessage.warning("请先登录");
      router.push({ name: "login" });
      return false;
    };

    const countAgree = () => {
      if (!post.value) return;
      $.ajax({
        url: `${API_BASE}/user/agree/count/`,
        type: "get",
        data: { pid: post.value.pid },
        success(resp) {
          post.value.cnt = Number(resp.cnt || 0);
        },
      });
    };

    const toggleAgree = () => {
      if (!requireLogin() || !post.value) return;
      $.ajax({
        url: `${API_BASE}${post.value.like ? "/user/agree/delete/" : "/user/agree/add/"}`,
        type: "get",
        headers: authHeaders(),
        data: {
          pid: post.value.pid,
          id: store.state.user.id,
        },
        success(resp) {
          if (resp.error_message === "success") {
            post.value.like = !post.value.like;
            countAgree();
          } else {
            ElMessage.error(resp.error_message || "操作失败");
          }
        },
      });
    };

    const toggleReply = (comment) => {
      activeReplyKey.value = activeReplyKey.value === comment.cid ? "" : comment.cid;
    };

    const writeComment = (parent = null) => {
      if (!requireLogin() || !post.value) return;
      const key = parent ? parent.cid : "";
      const value = (parent ? replyInputs[key] : newComment.value || "").trim();
      if (!value) {
        ElMessage.error("评论不能为空");
        return;
      }
      $.ajax({
        url: `${API_BASE}/user/post/comment/`,
        type: "post",
        headers: authHeaders(),
        data: {
          content: value,
          pid: post.value.pid,
          id: store.state.user.id,
          parentId: parent ? parent.cid : "",
        },
        success(resp) {
          if (resp.error_message === "success") {
            if (parent) {
              replyInputs[key] = "";
              activeReplyKey.value = "";
            } else {
              newComment.value = "";
            }
            loadComments();
            ElMessage.success("评论成功");
          } else {
            ElMessage.error(resp.error_message || "评论失败");
          }
        },
      });
    };

    const canDeleteComment = (comment) => {
      if (!store.state.user.is_login || !post.value) return false;
      return isManager.value || String(post.value.id) === String(store.state.user.id) || String(comment.id) === String(store.state.user.id);
    };

    const deleteComment = (comment) => {
      if (!canDeleteComment(comment)) return;
      $.ajax({
        url: `${API_BASE}/user/post/comment/delete/`,
        type: "post",
        headers: authHeaders(),
        data: { cid: comment.cid },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("评论已删除");
            loadComments();
          } else {
            ElMessage.error(resp.error_message || "删除失败");
          }
        },
      });
    };

    const openUser = (userId) => {
      if (!userId) return;
      openRouteInNewWindow(router, { name: "user-home", params: { userId } });
    };

    const onScroll = () => {
      const doc = document.documentElement;
      if (window.innerHeight + window.scrollY >= doc.scrollHeight - 80) {
        commentLimit.value = Math.min(commentLimit.value + 10, comments.length);
      }
    };

    onMounted(() => {
      loadPost();
      window.addEventListener("scroll", onScroll);
    });

    onBeforeUnmount(() => {
      window.removeEventListener("scroll", onScroll);
    });

    return {
      Star,
      StarFilled,
      store,
      post,
      comments,
      visibleComments,
      flatCommentCount,
      newComment,
      replyInputs,
      activeReplyKey,
      commentLimit,
      loadError,
      fullImageUrl,
      fullImageUrls,
      roleTagType,
      canSeeHotScore,
      formatHot,
      toggleAgree,
      toggleReply,
      writeComment,
      canDeleteComment,
      deleteComment,
      openUser,
    };
  },
};
</script>

<style scoped>
.detail-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 28px 16px 56px;
}

.detail-shell {
  margin: 0 auto;
  max-width: 880px;
}

.post-card,
.comment-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 18px;
}

.comment-card {
  margin-top: 16px;
}

.post-header,
.comment-form,
.comment-item,
.reply-item {
  display: flex;
  gap: 12px;
}

.post-header {
  align-items: center;
}

.avatar-button,
.name-button {
  background: transparent;
  border: 0;
  color: inherit;
  cursor: pointer;
  font: inherit;
  padding: 0;
}

.avatar,
.comment-avatar {
  border-radius: 50%;
  object-fit: cover;
}

.avatar {
  height: 52px;
  width: 52px;
}

.comment-avatar {
  height: 36px;
  width: 36px;
}

.comment-avatar.small {
  height: 30px;
  width: 30px;
}

.identity-line {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.identity-line.compact {
  gap: 6px;
}

.username,
.comment-user,
.name-button {
  font-weight: 700;
}

.time,
.post-stats,
.load-tip {
  color: #909399;
  font-size: 13px;
}

.post-card h1 {
  color: #303133;
  font-size: 28px;
  line-height: 1.35;
  margin: 18px 0 10px;
  word-break: break-word;
}

.content {
  color: #303133;
  line-height: 1.8;
  margin: 0 0 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 14px;
  max-width: 620px;
}

.post-image {
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  width: 100%;
}

.post-stats,
.post-actions,
.comment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.post-actions {
  border-top: 1px solid #ebeef5;
  margin-top: 10px;
  padding-top: 8px;
}

.comment-title {
  color: #303133;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.comment-form {
  align-items: flex-start;
  margin-bottom: 14px;
}

.comment-login {
  margin-bottom: 12px;
}

.comment-main {
  flex: 1 1 auto;
  min-width: 0;
}

.comment-item {
  border-top: 1px solid #ebeef5;
  padding: 12px 0;
}

.comment-content {
  color: #303133;
  margin-top: 4px;
  word-break: break-word;
}

.reply-form {
  align-items: center;
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.reply-item {
  background: #f8f9fb;
  border-radius: 8px;
  margin-top: 10px;
  padding: 10px;
}

.reply-to {
  color: #909399;
  font-size: 12px;
}

.load-tip {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  text-align: center;
}

@media (max-width: 640px) {
  .post-images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .comment-form,
  .reply-form {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
