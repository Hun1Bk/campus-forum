<template>
  <main class="forum-page">
    <section class="forum-shell">
      <div class="forum-header">
        <div>
          <h1>社区动态</h1>
          <p>发布帖子、参与讨论，并按分区浏览同学们的交流内容。</p>
        </div>
        <el-button type="primary" :icon="Edit" @click="openComposer">发帖</el-button>
      </div>

      <div class="forum-layout">
        <section class="feed-column">
          <div class="section-filter">
            <el-button :type="activeSectionId === 'all' ? 'primary' : 'default'" @click="selectSection('all')">
              全部分区
            </el-button>
            <el-button
              v-for="section in sections"
              :key="section.sid"
              :type="String(activeSectionId) === String(section.sid) ? 'primary' : 'default'"
              @click="selectSection(section.sid)"
            >
              {{ section.name }}
            </el-button>
          </div>

          <div class="search-bar">
            <el-input
              v-model="searchKeyword"
              clearable
              placeholder="搜索帖子标题或内容"
              @keyup.enter="searchPosts"
              @clear="searchPosts"
            />
            <el-button type="primary" @click="searchPosts">搜索</el-button>
          </div>

          <el-card shadow="never" class="composer-card" v-if="store.state.user.is_login">
            <div class="composer-top">
              <el-input
                v-model="title"
                maxlength="80"
                show-word-limit
                placeholder="请输入帖子标题"
                class="title-input"
              />
              <el-select v-model="selectedSectionId" placeholder="选择分区" class="section-select">
                <el-option
                  v-for="section in sections"
                  :key="section.sid"
                  :label="section.name"
                  :value="section.sid"
                />
              </el-select>
              <el-radio-group v-model="visibility" class="visibility-group">
                <el-radio-button label="PUBLIC">所有人可见</el-radio-button>
                <el-radio-button label="PRIVATE">仅自己可见</el-radio-button>
              </el-radio-group>
            </div>
            <el-input
              v-model="content"
              type="textarea"
              rows="4"
              maxlength="500"
              show-word-limit
              placeholder="分享你的想法..."
            />

            <input
              ref="imageInput"
              class="file-input"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              multiple
              @change="handleImageChange"
            >

            <div class="selected-images" v-if="selectedImages.length">
              <div v-for="(image, index) in selectedImages" :key="image.url" class="selected-image">
                <img :src="image.url" alt="selected">
                <button type="button" class="remove-image" @click="removeSelectedImage(index)">
                  <el-icon><CloseBold /></el-icon>
                </button>
              </div>
            </div>

            <div class="composer-actions">
              <el-button :icon="Picture" @click="chooseImages">
                添加图片 {{ selectedImages.length }}/9
              </el-button>
              <el-button type="primary" :icon="Promotion" :loading="publishing" @click="write">发布</el-button>
            </div>
          </el-card>

          <el-alert
            v-else
            title="登录后可以发帖、点赞和评论"
            type="info"
            show-icon
            :closable="false"
            class="login-alert"
          />

          <el-empty v-if="posts.length === 0" description="还没有帖子，来发布第一条动态吧。" />

          <article v-for="(post, index) in posts" :key="post.pid" class="post-card" :class="{ pinned: post.isTop }" @click="openPostPage(post)">
            <header class="post-header">
              <button type="button" class="avatar-button" @click.stop="openUserPage(post.id)">
                <img :src="fullImageUrl(post.photo)" class="avatar" alt="avatar">
              </button>
              <div class="post-meta">
                <div class="identity-line">
                  <button type="button" class="name-button" @click.stop="openUserPage(post.id)">{{ post.username }}</button>
                  <el-tag size="small" :type="roleTagType(post.authorRole)">{{ post.authorTitle }}</el-tag>
                  <el-tag size="small" effect="plain">{{ post.sectionName }}</el-tag>
                  <el-tag v-if="post.visibility === 'PRIVATE'" size="small" type="warning">私密帖</el-tag>
                  <el-tag v-if="post.isTop" size="small" type="danger">置顶</el-tag>
                </div>
                <div class="time">发布于 {{ post.timer }}</div>
              </div>
              <el-dropdown
                v-if="canShowPostMenu(post)"
                trigger="click"
                class="post-menu"
                @click.stop
                @command="(command) => handlePostMenu(command, post)"
              >
                <el-button text circle :icon="MoreFilled" aria-label="更多操作" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="canEditPost(post)" command="edit" :icon="Edit">修改</el-dropdown-item>
                    <el-dropdown-item v-if="canDeletePost(post)" command="delete" :icon="Delete">删除</el-dropdown-item>
                    <el-dropdown-item v-if="canBlockUser(post)" command="blockUser" divided>屏蔽该用户</el-dropdown-item>
                    <el-dropdown-item v-if="canBlockSection(post)" command="blockSection">屏蔽该分区</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </header>

            <h2 class="post-title">{{ post.title || "无标题" }}</h2>
            <p class="post-content">{{ contentPreview(post.content) }}</p>

            <div class="post-images" v-if="post.imageUrls && post.imageUrls.length" @click.stop>
              <el-image
                v-for="image in post.imageUrls"
                :key="image"
                class="post-image"
                :src="fullImageUrl(image)"
                :preview-src-list="fullImageUrls(post.imageUrls)"
                fit="cover"
                lazy
              />
            </div>

            <div class="post-stats">
              <span>浏览 {{ post.viewCount || 1 }}</span>
              <span v-if="canSeeHotScore(post)">热度 {{ formatHot(post.hotScore) }}</span>
            </div>

            <div class="post-actions" @click.stop>
              <el-button text :icon="View" @click="viewPost(post)">查看</el-button>
              <el-button
                text
                :type="post.like ? 'primary' : 'default'"
                :icon="post.like ? StarFilled : Star"
                @click="toggleAgree(post, index)"
              >
                {{ post.cnt }} 点赞
              </el-button>
              <el-button text :icon="ChatLineRound" @click="toggleCommentBox(post)">
                {{ post.comments.length }} 评论
              </el-button>
            </div>

            <section class="comment-panel" v-show="post.boxshow" @click.stop>
              <div class="comment-form" v-if="store.state.user.is_login">
                <button type="button" class="avatar-button" @click="openUserPage(store.state.user.id)">
                  <img :src="fullImageUrl(store.state.user.photo)" class="comment-avatar" alt="avatar">
                </button>
                <el-input
                  v-model="newComments[post.pid]"
                  type="textarea"
                  rows="2"
                  placeholder="写下你的评论..."
                />
                <el-button type="primary" plain @click="writeComment(post)">提交评论</el-button>
              </div>

              <el-alert
                v-else
                title="登录后可以参与评论"
                type="info"
                :closable="false"
                class="comment-login"
              />

              <div v-for="comment in post.comments" :key="comment.cid || `${comment.username}-${comment.content}`" class="comment-item">
                <button type="button" class="avatar-button" @click="openUserPage(comment.id)">
                  <img :src="fullImageUrl(comment.photo)" class="comment-avatar" alt="avatar">
                </button>
                <div class="comment-main">
                  <div class="identity-line compact">
                    <span class="comment-user">{{ comment.username }}</span>
                    <el-tag size="small" :type="roleTagType(comment.authorRole)">{{ comment.authorTitle || '普通用户' }}</el-tag>
                  </div>
                  <div class="comment-content">{{ comment.content }}</div>
                  <div class="comment-actions">
                    <el-button v-if="store.state.user.is_login" text size="small" @click="toggleReply(post, comment)">回复</el-button>
                    <el-button v-if="canDeleteComment(post, comment)" text size="small" type="danger" @click="deleteComment(post, comment)">删除</el-button>
                  </div>
                  <div v-if="activeReplyKey === replyKey(post, comment)" class="reply-form">
                    <el-input v-model="replyInputs[replyKey(post, comment)]" size="small" placeholder="写下你的回复..." />
                    <el-button size="small" type="primary" @click="writeComment(post, comment)">发送</el-button>
                  </div>
                  <div v-for="reply in comment.replies" :key="reply.cid" class="reply-item">
                    <button type="button" class="avatar-button" @click="openUserPage(reply.id)">
                      <img :src="fullImageUrl(reply.photo)" class="comment-avatar small" alt="avatar">
                    </button>
                    <div class="comment-main">
                      <div class="identity-line compact">
                        <span class="comment-user">{{ reply.username }}</span>
                        <el-tag size="small" :type="roleTagType(reply.authorRole)">{{ reply.authorTitle || '普通用户' }}</el-tag>
                        <span v-if="reply.replyToUsername" class="reply-to">回复 {{ reply.replyToUsername }}</span>
                      </div>
                      <div class="comment-content">{{ reply.content }}</div>
                      <div class="comment-actions">
                        <el-button v-if="store.state.user.is_login" text size="small" @click="toggleReply(post, reply)">回复</el-button>
                        <el-button v-if="canDeleteComment(post, reply)" text size="small" type="danger" @click="deleteComment(post, reply)">删除</el-button>
                      </div>
                      <div v-if="activeReplyKey === replyKey(post, reply)" class="reply-form">
                        <el-input v-model="replyInputs[replyKey(post, reply)]" size="small" placeholder="写下你的回复..." />
                        <el-button size="small" type="primary" @click="writeComment(post, reply)">发送</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </article>
        </section>

        <aside class="hot-panel">
          <div class="hot-title">热度排行榜</div>
          <el-empty v-if="hotPosts.length === 0" description="暂无热帖" />
          <button
            v-for="(post, index) in hotPosts"
            :key="post.pid"
            class="hot-item"
            type="button"
            @click="openPostPage(post)"
          >
              <span class="hot-rank">{{ index + 1 }}</span>
            <span class="hot-body">
              <span class="hot-content">{{ post.title || "无标题" }}</span>
              <span class="hot-meta">
                {{ post.sectionName }} · {{ post.authorTitle }} · 浏览 {{ post.viewCount || 1 }}
                <template v-if="canSeeHotScore(post)"> · 热度 {{ formatHot(post.hotScore) }}</template>
              </span>
            </span>
          </button>

        </aside>
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="帖子详情" width="720px">
      <article v-if="activePost" class="detail-post">
        <header class="post-header">
          <img :src="fullImageUrl(activePost.photo)" class="avatar" alt="avatar">
          <div>
            <div class="identity-line">
              <span class="username">{{ activePost.username }}</span>
              <el-tag size="small" :type="roleTagType(activePost.authorRole)">{{ activePost.authorTitle }}</el-tag>
              <el-tag size="small" effect="plain">{{ activePost.sectionName }}</el-tag>
              <el-tag v-if="activePost.visibility === 'PRIVATE'" size="small" type="warning">私密帖</el-tag>
              <el-tag v-if="activePost.isTop" size="small" type="danger">置顶</el-tag>
            </div>
            <div class="time">发布于 {{ activePost.timer }}</div>
          </div>
        </header>
        <h2 class="post-title detail-title">{{ activePost.title || "无标题" }}</h2>
        <p class="post-content">{{ activePost.content }}</p>
        <div class="post-images" v-if="activePost.imageUrls && activePost.imageUrls.length">
          <el-image
            v-for="image in activePost.imageUrls"
            :key="image"
            class="post-image"
            :src="fullImageUrl(image)"
            :preview-src-list="fullImageUrls(activePost.imageUrls)"
            fit="cover"
          />
        </div>
        <div class="post-stats">
          <span>浏览 {{ activePost.viewCount || 1 }}</span>
          <span>点赞 {{ activePost.cnt || activePost.agreeCount || 0 }}</span>
          <span v-if="canSeeHotScore(activePost)">热度 {{ formatHot(activePost.hotScore) }}</span>
        </div>
      </article>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑帖子" width="720px" @closed="clearEditImages">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="80" show-word-limit placeholder="请输入帖子标题" />
        </el-form-item>
        <el-form-item label="分区">
          <el-select v-model="editForm.sectionId" placeholder="选择分区" class="section-select">
            <el-option
              v-for="section in sections"
              :key="section.sid"
              :label="section.name"
              :value="section.sid"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="editForm.visibility">
            <el-radio-button label="PUBLIC">所有人可见</el-radio-button>
            <el-radio-button label="PRIVATE">仅自己可见</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="图片">
          <input
            ref="editImageInput"
            class="file-input"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            multiple
            @change="handleEditImageChange"
          >
          <el-button :icon="Picture" @click="chooseEditImages">
            添加图片 {{ editForm.keepImages.length + editImages.length }}/9
          </el-button>
          <div class="selected-images edit-images" v-if="editForm.keepImages.length || editImages.length">
            <div v-for="(image, index) in editForm.keepImages" :key="image" class="selected-image">
              <img :src="fullImageUrl(image)" alt="old">
              <button type="button" class="remove-image" @click="removeKeepImage(index)">
                <el-icon><CloseBold /></el-icon>
              </button>
            </div>
            <div v-for="(image, index) in editImages" :key="image.url" class="selected-image">
              <img :src="image.url" alt="new">
              <button type="button" class="remove-image" @click="removeEditImage(index)">
                <el-icon><CloseBold /></el-icon>
              </button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import { ChatLineRound, CloseBold, Delete, Edit, MoreFilled, Picture, Promotion, Star, StarFilled, View } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import $ from "jquery";
import router from "@/router/index";
import { openRouteInNewWindow } from "@/utils/openRoute";
import { API_BASE } from "@/config/api";

const MAX_IMAGE_COUNT = 9;
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];
const MANAGER_ROLES = ["ADMIN", "SUPER_ADMIN", "OWNER"];

export default {
  name: "SpaceView",
  components: {
    CloseBold,
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    const posts = reactive([]);
    const hotPosts = reactive([]);
    const sections = reactive([]);
    const newComments = reactive({});
    const replyInputs = reactive({});
    const activeReplyKey = ref("");
    const title = ref("");
    const content = ref("");
    const visibility = ref("PUBLIC");
    const imageInput = ref(null);
    const selectedImages = ref([]);
    const selectedSectionId = ref("");
    const activeSectionId = ref(route.query.sectionId ? String(route.query.sectionId) : "all");
    const searchKeyword = ref(route.query.keyword ? String(route.query.keyword) : "");
    const publishing = ref(false);
    const detailVisible = ref(false);
    const activePost = ref(null);
    const editVisible = ref(false);
    const editImageInput = ref(null);
    const editImages = ref([]);
    const editing = ref(false);
    const editForm = reactive({
      pid: "",
      title: "",
      content: "",
      sectionId: "",
      visibility: "PUBLIC",
      keepImages: [],
    });

    const isManager = computed(() => MANAGER_ROLES.includes(store.state.user.role));

    const authHeaders = () => ({
      Authorization: "Bearer " + store.state.user.token,
    });

    const optionalAuthHeaders = () => {
      if (!store.state.user.token) return {};
      return authHeaders();
    };

    const requireLogin = () => {
      if (store.state.user.is_login) return true;
      ElMessage.warning("请先登录");
      router.push({ name: "login" });
      return false;
    };

    const roleTagType = (role) => {
      if (role === "OWNER") return "danger";
      if (role === "SUPER_ADMIN") return "success";
      if (role === "ADMIN") return "warning";
      return "info";
    };

    const canSeeHotScore = (post) => isManager.value && post.hotScore !== undefined;
    const formatHot = (value) => Number(value || 0).toFixed(2);

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

    const normalizePost = (post) => {
      post.comments = post.comments || [];
      post.boxshow = Boolean(post.boxshow);
      post.like = Boolean(post.like);
      post.cnt = Number(post.cnt || post.agreeCount || 0);
      post.imageUrls = normalizeImageUrls(post.imageUrls);
      post.viewCount = Number(post.viewCount || 1);
      post.hotScore = post.hotScore === undefined ? undefined : Number(post.hotScore || 0);
      post.isTop = post.isTop === true || post.isTop === "true";
      post.authorTitle = post.authorTitle || "普通用户";
      post.title = post.title || (post.content || "").slice(0, 30) || "无标题";
      post.visibility = post.visibility === "PRIVATE" ? "PRIVATE" : "PUBLIC";
      return post;
    };

    const normalizeComments = (comments) => {
      const map = {};
      const roots = [];
      (comments || []).forEach((comment) => {
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

    const contentPreview = (value) => {
      const text = value || "";
      return text.length > 50 ? `${text.slice(0, 50)}...` : text;
    };

    const validateImages = (files, usedCount) => {
      if (usedCount + files.length > MAX_IMAGE_COUNT) {
        ElMessage.error("每条帖子最多上传 9 张图片");
        return false;
      }
      for (const file of files) {
        if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
          ElMessage.error("只支持 jpg、png、webp、gif 图片");
          return false;
        }
        if (file.size > MAX_IMAGE_SIZE) {
          ElMessage.error("单张图片不能超过 5MB");
          return false;
        }
      }
      return true;
    };

    const clearSelectedImages = () => {
      selectedImages.value.forEach((image) => URL.revokeObjectURL(image.url));
      selectedImages.value = [];
      if (imageInput.value) {
        imageInput.value.value = "";
      }
    };

    const clearEditImages = () => {
      editImages.value.forEach((image) => URL.revokeObjectURL(image.url));
      editImages.value = [];
      if (editImageInput.value) {
        editImageInput.value.value = "";
      }
    };

    const chooseImages = () => {
      if (!requireLogin()) return;
      if (imageInput.value) {
        imageInput.value.click();
      }
    };

    const handleImageChange = (event) => {
      const files = Array.from(event.target.files || []);
      if (!files.length) return;
      if (!validateImages(files, selectedImages.value.length)) {
        event.target.value = "";
        return;
      }
      selectedImages.value = selectedImages.value.concat(files.map((file) => ({
        file,
        url: URL.createObjectURL(file),
      })));
      event.target.value = "";
    };

    const removeSelectedImage = (index) => {
      const image = selectedImages.value[index];
      if (image) {
        URL.revokeObjectURL(image.url);
      }
      selectedImages.value.splice(index, 1);
    };

    const chooseEditImages = () => {
      if (editImageInput.value) {
        editImageInput.value.click();
      }
    };

    const handleEditImageChange = (event) => {
      const files = Array.from(event.target.files || []);
      if (!files.length) return;
      if (!validateImages(files, editForm.keepImages.length + editImages.value.length)) {
        event.target.value = "";
        return;
      }
      editImages.value = editImages.value.concat(files.map((file) => ({
        file,
        url: URL.createObjectURL(file),
      })));
      event.target.value = "";
    };

    const removeKeepImage = (index) => {
      editForm.keepImages.splice(index, 1);
    };

    const removeEditImage = (index) => {
      const image = editImages.value[index];
      if (image) {
        URL.revokeObjectURL(image.url);
      }
      editImages.value.splice(index, 1);
    };

    const loadSections = () => {
      $.ajax({
        url: `${API_BASE}/user/section/list/`,
        type: "get",
        success(resp) {
          sections.splice(0, sections.length, ...resp);
          if (!selectedSectionId.value && sections.length > 0) {
            selectedSectionId.value = sections[0].sid;
          }
        },
      });
    };

    const getComment = (pid, targetPost = null) => {
      $.ajax({
        url: `${API_BASE}/user/post/getComment/`,
        type: "get",
        data: {
          pid: parseInt(pid),
        },
        success(resp) {
          const normalized = normalizeComments(resp);
          const index = posts.findIndex((value) => Number(value.pid) === Number(pid));
          if (index !== -1) {
            posts[index].comments = normalized;
          }
          if (targetPost) {
            targetPost.comments = normalized;
          }
        },
      });
    };

    const countAgree = (pid) => {
      $.ajax({
        url: `${API_BASE}/user/agree/count/`,
        type: "get",
        data: {
          pid,
        },
        success(resp) {
          const index = posts.findIndex((value) => Number(value.pid) === Number(pid));
          if (index !== -1) {
            posts[index].cnt = resp.cnt;
          }
        },
      });
    };

    const getAgree = () => {
      if (!store.state.user.id) return;
      $.ajax({
        url: `${API_BASE}/user/agree/get/`,
        type: "get",
        data: {
          id: store.state.user.id,
        },
        success(resp) {
          posts.forEach((post) => {
            post.like = Boolean(resp[post.pid]);
          });
        },
      });
    };

    const loadHotPosts = () => {
      $.ajax({
        url: `${API_BASE}/user/post/hot/`,
        type: "get",
        headers: optionalAuthHeaders(),
        success(resp) {
          hotPosts.splice(0, hotPosts.length, ...resp.map((post) => normalizePost(post)));
        },
      });
    };

    const openInitialPostIfNeeded = () => {
      const pid = route.query.pid;
      if (!pid) return;
      openRouteInNewWindow(router, { name: "post-detail", params: { pid } });
      router.replace({ name: "space" });
    };

    const getPost = () => {
      const data = {};
      if (activeSectionId.value !== "all") {
        data.sectionId = activeSectionId.value;
      }
      const keyword = searchKeyword.value.trim();
      if (keyword) {
        data.keyword = keyword;
      }
      $.ajax({
        url: `${API_BASE}/user/post/get/`,
        type: "get",
        headers: optionalAuthHeaders(),
        data,
        success(resp) {
          posts.splice(0, posts.length);
          resp.forEach((post) => {
            const item = normalizePost(post);
            posts.push(item);
            getComment(item.pid);
            countAgree(item.pid);
          });
          getAgree();
          openInitialPostIfNeeded();
        },
      });
    };

    const selectSection = (sectionId) => {
      activeSectionId.value = String(sectionId);
      const query = {};
      if (sectionId !== "all") query.sectionId = sectionId;
      if (searchKeyword.value.trim()) query.keyword = searchKeyword.value.trim();
      router.replace({ name: "space", query });
      getPost();
    };

    const searchPosts = () => {
      const query = {};
      if (activeSectionId.value !== "all") query.sectionId = activeSectionId.value;
      if (searchKeyword.value.trim()) query.keyword = searchKeyword.value.trim();
      router.replace({ name: "space", query });
      getPost();
    };

    const openComposer = () => {
      if (requireLogin()) {
        window.scrollTo({ top: 0, behavior: "smooth" });
      }
    };

    const write = () => {
      if (!requireLogin() || publishing.value) return;
      const trimmedTitle = title.value.trim();
      const trimmedContent = content.value.trim();
      if (!trimmedTitle) {
        ElMessage.error("标题不能为空");
        return;
      }
      if (!trimmedContent) {
        ElMessage.error("内容不能为空");
        return;
      }
      if (!selectedSectionId.value) {
        ElMessage.error("请选择分区");
        return;
      }

      const now = new Date();
      const timeValue = `${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()} ${now.getHours()}:${now.getMinutes()}:${now.getSeconds()}`;
      const formData = new FormData();
      formData.append("title", trimmedTitle);
      formData.append("content", trimmedContent);
      formData.append("timer", timeValue);
      formData.append("id", store.state.user.id);
      formData.append("sectionId", selectedSectionId.value);
      formData.append("visibility", visibility.value);
      selectedImages.value.forEach((image) => {
        formData.append("images", image.file);
      });

      publishing.value = true;
      $.ajax({
        url: `${API_BASE}/user/post/write/`,
        type: "post",
        headers: authHeaders(),
        data: formData,
        processData: false,
        contentType: false,
        success(resp) {
          if (resp.error_message === "success") {
            title.value = "";
            content.value = "";
            visibility.value = "PUBLIC";
            clearSelectedImages();
            getPost();
            loadHotPosts();
            ElMessage.success("发布成功");
          } else {
            ElMessage.error(resp.error_message || "发布失败");
          }
        },
        error() {
          ElMessage.error("发布失败，请重新登录后再试");
        },
        complete() {
          publishing.value = false;
        },
      });
    };

    const openPostPage = (post) => {
      if (!post || !post.pid) return;
      openRouteInNewWindow(router, { name: "post-detail", params: { pid: post.pid } });
    };

    const viewPost = openPostPage;

    const openUserPage = (userId) => {
      if (!userId) return;
      openRouteInNewWindow(router, { name: "user-home", params: { userId } });
    };

    const addAgree = (post, index) => {
      $.ajax({
        url: `${API_BASE}/user/agree/add/`,
        type: "get",
        headers: authHeaders(),
        data: {
          pid: post.pid,
          id: store.state.user.id,
        },
        success(resp) {
          if (resp.error_message === "success") {
            posts[index].like = true;
            countAgree(post.pid);
            loadHotPosts();
          } else {
            ElMessage.error(resp.error_message || "点赞失败");
          }
        },
        error() {
          ElMessage.error("请先登录");
        },
      });
    };

    const deleteAgree = (post, index) => {
      $.ajax({
        url: `${API_BASE}/user/agree/delete/`,
        type: "get",
        headers: authHeaders(),
        data: {
          pid: post.pid,
          id: store.state.user.id,
        },
        success() {
          posts[index].like = false;
          countAgree(post.pid);
          loadHotPosts();
        },
        error() {
          ElMessage.error("取消点赞失败");
        },
      });
    };

    const toggleAgree = (post, index) => {
      if (!requireLogin()) return;
      if (post.like) {
        deleteAgree(post, index);
      } else {
        addAgree(post, index);
      }
    };

    const toggleCommentBox = (post) => {
      post.boxshow = !post.boxshow;
      if (post.boxshow) {
        getComment(post.pid);
      }
    };

    const replyKey = (post, comment) => `${post.pid}-${comment.cid}`;

    const toggleReply = (post, comment) => {
      const key = replyKey(post, comment);
      activeReplyKey.value = activeReplyKey.value === key ? "" : key;
    };

    const writeComment = (post, parent = null) => {
      if (!requireLogin()) return;
      const key = parent ? replyKey(post, parent) : post.pid;
      const source = parent ? replyInputs : newComments;
      const value = (source[key] || "").trim();
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
          pid: post.pid,
          id: store.state.user.id,
          parentId: parent ? parent.cid : "",
        },
        success(resp) {
          if (resp.error_message === "success") {
            source[key] = "";
            if (parent) {
              activeReplyKey.value = "";
            }
            getComment(post.pid);
            ElMessage.success("评论成功");
          } else {
            ElMessage.error(resp.error_message || "评论失败");
          }
        },
        error() {
          ElMessage.error("评论失败，请重新登录后再试");
        },
      });
    };

    const canDeleteComment = (post, comment) => {
      if (!store.state.user.is_login) return false;
      return isManager.value || String(post.id) === String(store.state.user.id) || String(comment.id) === String(store.state.user.id);
    };

    const deleteComment = (post, comment) => {
      if (!canDeleteComment(post, comment)) return;
      $.ajax({
        url: `${API_BASE}/user/post/comment/delete/`,
        type: "post",
        headers: authHeaders(),
        data: {
          cid: comment.cid,
        },
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("评论已删除");
            getComment(post.pid);
          } else {
            ElMessage.error(resp.error_message || "删除失败");
          }
        },
        error() {
          ElMessage.error("删除失败");
        },
      });
    };

    const canEditPost = (post) => store.state.user.is_login && String(post.id) === String(store.state.user.id);
    const canDeletePost = (post) => store.state.user.is_login && (canEditPost(post) || isManager.value);
    const canBlockUser = (post) => store.state.user.is_login && String(post.id) !== String(store.state.user.id);
    const canBlockSection = () => store.state.user.is_login;
    const canShowPostMenu = (post) => canEditPost(post) || canDeletePost(post) || canBlockUser(post) || canBlockSection(post);

    const handlePostMenu = (command, post) => {
      if (command === "edit") {
        openEditDialog(post);
      }
      if (command === "delete") {
        deletePost(post);
      }
      if (command === "blockUser") {
        addBlockRule("USER", post.id, `确认屏蔽用户 ${post.username}？`);
      }
      if (command === "blockSection") {
        addBlockRule("SECTION", post.sectionId, `确认屏蔽分区 ${post.sectionName}？`);
      }
    };

    const openEditDialog = (post) => {
      if (!canEditPost(post)) return;
      clearEditImages();
      editForm.pid = post.pid;
      editForm.title = post.title || "";
      editForm.content = post.content;
      editForm.sectionId = post.sectionId || selectedSectionId.value;
      editForm.visibility = post.visibility === "PRIVATE" ? "PRIVATE" : "PUBLIC";
      editForm.keepImages = [...(post.imageUrls || [])];
      editVisible.value = true;
    };

    const saveEdit = () => {
      const trimmedTitle = editForm.title.trim();
      const trimmedContent = editForm.content.trim();
      if (!trimmedTitle) {
        ElMessage.error("标题不能为空");
        return;
      }
      if (!trimmedContent) {
        ElMessage.error("内容不能为空");
        return;
      }
      if (!editForm.sectionId) {
        ElMessage.error("请选择分区");
        return;
      }
      if (editForm.keepImages.length + editImages.value.length > MAX_IMAGE_COUNT) {
        ElMessage.error("每条帖子最多保留 9 张图片");
        return;
      }

      const formData = new FormData();
      formData.append("pid", editForm.pid);
      formData.append("title", trimmedTitle);
      formData.append("content", trimmedContent);
      formData.append("sectionId", editForm.sectionId);
      formData.append("visibility", editForm.visibility);
      formData.append("keepImageUrls", JSON.stringify(editForm.keepImages));
      editImages.value.forEach((image) => {
        formData.append("images", image.file);
      });

      editing.value = true;
      $.ajax({
        url: `${API_BASE}/user/post/update/`,
        type: "post",
        headers: authHeaders(),
        data: formData,
        processData: false,
        contentType: false,
        success(resp) {
          if (resp.error_message === "success") {
            ElMessage.success("帖子已更新");
            editVisible.value = false;
            getPost();
            loadHotPosts();
          } else {
            ElMessage.error(resp.error_message || "保存失败");
          }
        },
        error() {
          ElMessage.error("保存失败，请重新登录后再试");
        },
        complete() {
          editing.value = false;
        },
      });
    };

    const deletePost = (post) => {
      if (!canDeletePost(post)) return;
      const useAdminEndpoint = isManager.value;
      ElMessageBox.confirm("删除帖子会同时删除评论、点赞和相关通知，确认继续？", "删除帖子", {
        confirmButtonText: "确认删除",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        $.ajax({
          url: `${API_BASE}${useAdminEndpoint ? "/admin/posts/delete/" : "/user/post/delete/"}`,
          type: "post",
          headers: authHeaders(),
          data: {
            pid: post.pid,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("帖子已删除");
              if (activePost.value && String(activePost.value.pid) === String(post.pid)) {
                detailVisible.value = false;
                activePost.value = null;
              }
              getPost();
              loadHotPosts();
            } else {
              ElMessage.error(resp.error_message || "删除失败");
            }
          },
          error() {
            ElMessage.error("删除失败，请重新登录后再试");
          },
        });
      }).catch(() => {});
    };

    const addBlockRule = (targetType, targetId, message) => {
      if (!requireLogin()) return;
      ElMessageBox.confirm(message, "屏蔽确认", {
        confirmButtonText: "确认屏蔽",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        $.ajax({
          url: `${API_BASE}/user/block/add/`,
          type: "post",
          headers: authHeaders(),
          data: {
            targetType,
            targetId,
          },
          success(resp) {
            if (resp.error_message === "success") {
              ElMessage.success("已屏蔽");
              getPost();
              loadHotPosts();
            } else {
              ElMessage.error(resp.error_message || "屏蔽失败");
            }
          },
          error() {
            ElMessage.error("屏蔽失败");
          },
        });
      }).catch(() => {});
    };

    onMounted(() => {
      loadSections();
      getPost();
      loadHotPosts();
    });

    onBeforeUnmount(() => {
      clearSelectedImages();
      clearEditImages();
    });

    return {
      ChatLineRound,
      CloseBold,
      Delete,
      Edit,
      MoreFilled,
      Picture,
      Promotion,
      Star,
      StarFilled,
      View,
      store,
      posts,
      hotPosts,
      sections,
      newComments,
      replyInputs,
      activeReplyKey,
      title,
      content,
      visibility,
      imageInput,
      selectedImages,
      selectedSectionId,
      activeSectionId,
      searchKeyword,
      publishing,
      detailVisible,
      activePost,
      editVisible,
      editImageInput,
      editImages,
      editForm,
      editing,
      chooseImages,
      handleImageChange,
      removeSelectedImage,
      chooseEditImages,
      handleEditImageChange,
      removeKeepImage,
      removeEditImage,
      fullImageUrl,
      fullImageUrls,
      roleTagType,
      canSeeHotScore,
      formatHot,
      contentPreview,
      selectSection,
      searchPosts,
      openComposer,
      write,
      viewPost,
      openPostPage,
      openUserPage,
      toggleAgree,
      toggleCommentBox,
      replyKey,
      toggleReply,
      writeComment,
      canDeleteComment,
      deleteComment,
      canEditPost,
      canDeletePost,
      canBlockUser,
      canBlockSection,
      canShowPostMenu,
      handlePostMenu,
      addBlockRule,
      openEditDialog,
      saveEdit,
      deletePost,
      clearEditImages,
    };
  },
};
</script>

<style lang="scss" scoped>
.forum-page {
  background: #f5f7fb;
  min-height: calc(100vh - 67px);
  padding: 28px 16px 48px;
}

.forum-shell {
  margin: 0 auto;
  max-width: 1180px;
}

.forum-header {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  margin-bottom: 18px;
}

.forum-header h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
}

.forum-header p {
  color: #606266;
  margin: 0;
}

.forum-layout {
  align-items: start;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) 320px;
}

.feed-column {
  min-width: 0;
}

.section-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.search-bar {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto;
  margin-bottom: 14px;
}

.composer-card,
.login-alert {
  margin-bottom: 18px;
}

.composer-top {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.title-input {
  min-width: 260px;
  flex: 1 1 320px;
}

.section-select {
  max-width: 220px;
  width: 100%;
}

.visibility-group {
  flex: 0 0 auto;
}

.file-input {
  display: none;
}

.selected-images {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(86px, 1fr));
  margin-top: 12px;
}

.edit-images {
  width: 100%;
}

.selected-image {
  aspect-ratio: 1;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.selected-image img {
  height: 100%;
  object-fit: cover;
  width: 100%;
}

.remove-image {
  align-items: center;
  background: rgba(0, 0, 0, 0.62);
  border: 0;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  display: flex;
  height: 24px;
  justify-content: center;
  padding: 0;
  position: absolute;
  right: 6px;
  top: 6px;
  width: 24px;
}

.composer-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 12px;
}

.post-card,
.hot-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.post-card {
  cursor: pointer;
  margin-bottom: 16px;
  padding: 18px;
}

.post-card.pinned {
  border-color: #f56c6c;
}

.post-header,
.comment-form,
.comment-item {
  display: flex;
  gap: 12px;
}

.post-header {
  align-items: center;
}

.post-meta {
  min-width: 0;
}

.post-menu {
  margin-left: auto;
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

.identity-line {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.identity-line.compact {
  gap: 6px;
}

.avatar {
  border-radius: 50%;
  height: 48px;
  object-fit: cover;
  width: 48px;
}

.username,
.name-button,
.comment-user {
  font-weight: 700;
}

.time {
  color: #909399;
  font-size: 13px;
}

.post-title {
  color: #303133;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
  margin: 16px 0 8px;
  word-break: break-word;
}

.detail-title {
  font-size: 22px;
}

.post-content {
  color: #303133;
  line-height: 1.7;
  margin: 0 0 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0 0 14px;
  max-width: 520px;
}

.post-image {
  aspect-ratio: 1;
  border-radius: 8px;
  cursor: zoom-in;
  overflow: hidden;
  width: 100%;
}

.post-stats {
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  gap: 14px;
  margin-bottom: 8px;
}

.post-actions {
  border-top: 1px solid #ebeef5;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 8px;
}

.comment-panel {
  background: #f8f9fb;
  border-radius: 8px;
  margin-top: 12px;
  padding: 14px;
}

.comment-form {
  align-items: flex-start;
  margin-bottom: 14px;
}

.comment-form .el-button {
  flex: 0 0 auto;
}

.comment-avatar {
  border-radius: 50%;
  height: 36px;
  object-fit: cover;
  width: 36px;
}

.comment-avatar.small {
  height: 30px;
  width: 30px;
}

.comment-main {
  flex: 1 1 auto;
  min-width: 0;
}

.comment-login {
  margin-bottom: 12px;
}

.comment-item {
  border-top: 1px solid #ebeef5;
  padding: 12px 0;
}

.comment-content {
  color: #303133;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.reply-form {
  align-items: center;
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.reply-item {
  background: #fff;
  border-radius: 8px;
  display: flex;
  gap: 10px;
  margin-top: 10px;
  padding: 10px;
}

.reply-to {
  color: #909399;
  font-size: 12px;
}

.hot-panel {
  padding: 14px;
  position: sticky;
  top: 82px;
}

.hot-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
}

.hot-item {
  align-items: flex-start;
  background: transparent;
  border: 0;
  border-top: 1px solid #ebeef5;
  cursor: pointer;
  display: flex;
  gap: 10px;
  padding: 12px 0;
  text-align: left;
  width: 100%;
}

.hot-item:first-of-type {
  border-top: 0;
}

.hot-rank {
  background: #f56c6c;
  border-radius: 50%;
  color: #fff;
  flex: 0 0 auto;
  font-size: 12px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  width: 22px;
}

.hot-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.hot-content {
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-meta {
  color: #909399;
  font-size: 12px;
}

.detail-post {
  padding-right: 4px;
}

@media (max-width: 900px) {
  .forum-layout {
    grid-template-columns: 1fr;
  }

  .hot-panel {
    position: static;
  }
}

@media (max-width: 640px) {
  .forum-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .composer-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .search-bar {
    grid-template-columns: 1fr;
  }

  .visibility-group {
    width: 100%;
  }

  .post-images {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .comment-form {
    flex-direction: column;
  }
}
</style>
