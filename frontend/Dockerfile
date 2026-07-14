# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
# `npm install` (not `npm ci`): it honors the committed lockfile but reconciles the
# known chokidar 3/4 split in Angular 18's dev-dep tree that makes `npm ci` fail.
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# --- Runtime stage ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ticketing-frontend/browser /usr/share/nginx/html
EXPOSE 80
