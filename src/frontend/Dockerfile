# syntax=docker/dockerfile:1.7

# ---- Stage 1: build the SPA ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Stage 2: serve via Nginx ----
FROM nginx:1.27-alpine

# Templated config: nginx image runs envsubst on /etc/nginx/templates/*.template
# at container start, writing the result to /etc/nginx/conf.d/.
# NGINX_ENVSUBST_FILTER_VARS limits substitution to our vars so nginx's own
# $host / $remote_addr / etc. are left alone.
COPY default.conf.template /etc/nginx/templates/default.conf.template
ENV BACKEND_HOST=backend \
    BACKEND_PORT=8080 \
    NGINX_ENVSUBST_FILTER_VARS="BACKEND_HOST BACKEND_PORT"

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
