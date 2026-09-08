# syntax=docker/dockerfile:1

# ===========================================================================
# One image, two services:
#   - the Ktor app  (dev.agner.chameidor.application.BootKt)  -> ${API_PORT}, default 8080
#   - nginx         (the built SPA + /api proxy)              -> ${WEB_PORT}, default 8081
# supervised together by supervisord.
#
# The database is NOT part of this image. Point MYSQL_* at an external MySQL
# (docker-compose.yml has one for local/full-stack runs).
# ===========================================================================

# ---------------------------------------------------------------------------
# Stage 1 — build the frontend SPA to static files
# ---------------------------------------------------------------------------
FROM node:22-slim AS frontend
WORKDIR /fe

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
# Served at the nginx root; API is reached through nginx's /api proxy
# (see deploy/nginx.conf.template), so the client's base stays "/api".
ENV VITE_BASE=/ \
    VITE_API_BASE=/api
RUN npm run build          # -> /fe/dist

# ---------------------------------------------------------------------------
# Stage 2 — build the backend distribution
# ---------------------------------------------------------------------------
FROM gradle:8.14-jdk21 AS backend
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle backend/ ./
RUN gradle -Dorg.gradle.daemon=false :application:installDist --stacktrace
# -> /home/gradle/project/application/build/install/application/{bin,lib}

# ---------------------------------------------------------------------------
# Stage 3 — runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble
WORKDIR /opt/app

RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx supervisor gettext-base curl \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default \
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /opt/app app

ENV API_PORT=8080 \
    WEB_PORT=8081

COPY --from=backend --chown=app:app /home/gradle/project/application/build/install/application/ /opt/app/
COPY --from=frontend /fe/dist /var/www/chameidor
COPY deploy/nginx.conf.template /etc/nginx/templates/chameidor.conf.template
COPY deploy/supervisord.conf /etc/supervisor/conf.d/chameidor.conf
COPY deploy/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 8080 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -fsS "http://localhost:${WEB_PORT}/" >/dev/null \
        && curl -fsS "http://localhost:${API_PORT}/health/internal" >/dev/null || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
