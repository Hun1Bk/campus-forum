import $ from 'jquery'
import { API_BASE } from "@/config/api";

const roleTitle = (role) => {
    if (role === 'OWNER') return '站长';
    if (role === 'SUPER_ADMIN') return '高级管理员';
    if (role === 'ADMIN') return '管理员';
    return '普通用户';
};

const ModuleUser = {
    state: {
        id: "",
        username: "",
        account: "",
        email: "",
        photo: "",
        token: "",
        role: "",
        title: "",
        customTitle: "",
        accountUpdateTime: "",
        status: "",
        is_login: false,
    },
    getters: {
    },
    mutations: {
        updateUser(state, user) {
            state.id = user.id;
            state.username = user.username;
            state.account = user.account || '';
            state.email = user.email || '';
            state.photo = user.photo;
            state.role = user.role || 'USER';
            state.title = user.title || roleTitle(state.role);
            state.customTitle = user.customTitle || '';
            state.accountUpdateTime = user.accountUpdateTime || '';
            state.status = user.status || 'ACTIVE';
            state.is_login = user.is_login;
        },
        updateToken(state, token) {
            state.token = token;
        },
        logout(state) {
            state.id = '';
            state.username = '';
            state.account = '';
            state.email = '';
            state.photo = '';
            state.token = '';
            state.role = '';
            state.title = '';
            state.customTitle = '';
            state.accountUpdateTime = '';
            state.status = '';
            state.is_login = false;
        }
    },
    actions: {
        login(context, data) {
            const loginName = data.account || '';
            const isEmail = loginName.includes('@');
            $.ajax({
                url: `${API_BASE}/user/account/token/`,
                type: 'post',
                data: {
                    account: isEmail ? '' : loginName,
                    username: isEmail ? '' : loginName,
                    email: isEmail ? loginName : '',
                    password: data.password,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        localStorage.setItem("jwt_token", resp.token);
                        context.commit("updateToken", resp.token);
                    }
                    data.success(resp);
                },
                error() {
                    data.error();
                }
            })
        },
        emailLogin(context, data) {
            $.ajax({
                url: `${API_BASE}/user/account/email-login/`,
                type: 'post',
                data: {
                    email: data.email,
                    emailCode: data.emailCode,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        localStorage.setItem("jwt_token", resp.token);
                        context.commit("updateToken", resp.token);
                    }
                    data.success(resp);
                },
                error() {
                    data.error();
                }
            })
        },
        getInfo(context, data) {
            $.ajax({
                url: `${API_BASE}/user/account/info/`,
                type: "get",
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        context.commit("updateUser", {
                            ...resp,
                            is_login: true,
                        })
                        data.success();
                    }
                },
                error() {
                    if (data.error) data.error();
                }
            })
        },
        updateProfile(context, data) {
            $.ajax({
                url: `${API_BASE}/user/account/update/`,
                type: "post",
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                data: {
                    username: data.username,
                    photo: data.photo,
                    password: data.password,
                    confirmedPassword: data.confirmedPassword,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        context.commit("updateUser", {
                            id: resp.id,
                            username: resp.username,
                            account: resp.account,
                            email: resp.email,
                            photo: resp.photo,
                            role: resp.role,
                            title: resp.title,
                            customTitle: resp.customTitle,
                            accountUpdateTime: resp.accountUpdateTime,
                            status: resp.status,
                            is_login: true,
                        });
                    }
                    data.success(resp);
                },
                error() {
                    data.error();
                }
            })
        },
        logout(context) {
            localStorage.removeItem("jwt_token");
            context.commit("logout");
        }
    },
    modules: {
    }
}

export default ModuleUser;
