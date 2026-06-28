FROM node:18-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci --legacy-peer-deps
COPY . .
ARG VUE_APP_API_BASE_URL=/api
ENV VUE_APP_API_BASE_URL=$VUE_APP_API_BASE_URL
RUN npm run build

FROM nginx:1.25-alpine

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
