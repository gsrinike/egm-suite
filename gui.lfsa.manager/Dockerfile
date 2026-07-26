FROM node:22-alpine AS build
WORKDIR /workspace
COPY gui.common ./gui.common
COPY gui.lfsa.manager ./gui.lfsa.manager
WORKDIR /workspace/gui.lfsa.manager
RUN npm install && npm run build

FROM nginx:1.27-alpine
COPY gui.lfsa.manager/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/gui.lfsa.manager/dist /usr/share/nginx/html
EXPOSE 80
