FROM node:20.19-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci
COPY . .

ARG VITE_API_BASE_URL
ARG VITE_WS_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_URL=${VITE_WS_URL}

RUN npm run build

FROM nginx:alpine
ENV RENDER=false PROXY_SHARED_SECRET=""
ENV NGINX_ENVSUBST_FILTER="^(RENDER|PROXY_SHARED_SECRET)$"
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY docker-entrypoint-proxy.sh /docker-entrypoint.d/19-proxy-secret.sh
RUN chmod +x /docker-entrypoint.d/19-proxy-secret.sh
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 10000
