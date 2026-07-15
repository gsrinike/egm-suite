FROM node:22-alpine AS build
WORKDIR /workspace
COPY gui.common ./gui.common
COPY gui.rcc.manager ./gui.rcc.manager
WORKDIR /workspace/gui.rcc.manager
RUN npm install && npm run build

FROM nginx:1.27-alpine
COPY gui.rcc.manager/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/gui.rcc.manager/dist /usr/share/nginx/html
EXPOSE 80
