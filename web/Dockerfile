# =============================================================
# SGTC - Frontend (Angular)
# Stage 1: build com Node 22
# Stage 2: nginx servindo /dist + proxy /api -> backend:8080
# =============================================================

FROM node:22-alpine AS build
WORKDIR /app

# Copia manifestos primeiro para aproveitar cache.
COPY package*.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build -- --configuration production

# ----- Runtime -----
FROM nginx:alpine

COPY --from=build /app/dist/sgtc-web/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
