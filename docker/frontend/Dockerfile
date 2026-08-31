# nginx serving the built TicketMesh SPA (frontend/dist) and proxying API
# calls to the backend container.
FROM nginx:1.27-alpine
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/dist /usr/share/nginx/html
EXPOSE 80
