# ============================================================================
# SareeKart Frontend — Dockerfile
# Node 20 / Vite / Nginx Alpine · Multi-stage build
# Step 3.2: npm ci, non-root nginx user, unprivileged port 8080, HEALTHCHECK
# ============================================================================

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Copy lockfile first so the npm cache layer is only invalidated when
# package.json or package-lock.json changes (not on every source edit).
COPY package*.json ./

# npm ci is deterministic: it reads package-lock.json exactly, fails if the
# lockfile is inconsistent, and does not modify it.  Faster than npm install.
RUN npm ci

COPY . .

# Produce the production bundle.
RUN npm run build

# Create a minimal static health-check file so the nginx HEALTHCHECK below
# has a target that does not depend on backend availability.
RUN echo "OK" > /app/dist/health.html

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM nginx:alpine AS run

# Why port 8080 instead of 80?
# nginx:alpine's built-in 'nginx' user (UID 101) cannot bind privileged
# ports (< 1024) without CAP_NET_BIND_SERVICE.  Port 8080 is unprivileged
# and allows the entire process — including the master — to run as UID 101.
# The host port mapping (docker-compose: "80:8080" or "8080:8080") is
# unchanged from the user's perspective.
#
# The custom nginx.conf also redirects all writable temp dirs to /tmp/*
# and sets pid /tmp/nginx.pid so no root-owned directories are required.

# Copy the full nginx.conf (not just the server block) — required because
# non-root operation needs http-level temp-path and pid directives.
COPY nginx.conf /etc/nginx/nginx.conf

# Copy the Vite production build (including health.html) from the build stage.
COPY --from=build /app/dist /usr/share/nginx/html

# Fix ownership so the nginx user can read all static files.
RUN chown -R nginx:nginx /usr/share/nginx/html

# Switch to the non-root nginx user (UID 101, GID 101) before starting.
USER nginx

# Nginx listens on 8080 (matches nginx.conf listen directive).
EXPOSE 8080

# Native HEALTHCHECK — polls the static health.html; does NOT require
# the backend to be available, so the frontend container is self-contained.
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --spider -q http://127.0.0.1:8080/health.html || exit 1

CMD ["nginx", "-g", "daemon off;"]
