# Production nginx image: builds the React app, serves static files,
# and reverse-proxies API/WebSocket traffic to the Spring Boot container.

FROM node:22-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json ./
RUN npm install
COPY frontend/ .
# Empty base URL → browser uses same origin (nginx handles /api and /ws).
ENV VITE_API_BASE_URL=
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx/snippets/ /etc/nginx/snippets/
COPY --from=frontend-build /app/dist /usr/share/nginx/html
EXPOSE 80
