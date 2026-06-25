FROM node:22-alpine AS build
WORKDIR /workspace
COPY gui.common ./gui.common
COPY gui.cnm.manager ./gui.cnm.manager
WORKDIR /workspace/gui.cnm.manager
RUN npm install && npm run build

FROM nginx:1.27-alpine
COPY gui.cnm.manager/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/gui.cnm.manager/dist /usr/share/nginx/html
EXPOSE 80
