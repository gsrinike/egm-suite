# Local Deployment And Environment

The suite is built with Maven and run locally with Docker Compose. Backend
services use YAML configuration loaded by `com.utils`; frontend modules load
small JSON runtime configuration files before Vue mounts.

## Environment Resolution

Backend modules resolve the active environment in this order:

1. JVM system property `env`
2. environment variable `ENV`
3. default `local`

The module name is supplied through the `module` property and determines which
configuration files are loaded.

## Backend Configuration Layout

Modules load base files and environment overrides:

```text
src/main/resources/config/
  base/<module>-application.yml
  base/<module>-infra.yml
  base/<module>-cache-config.yml
  base/<module>-vault.yml
  local/<module>-application.yml
  local/<module>-infra.yml
  local/<module>-cache-config.yml
  local/<module>-vault.yml
```

Profile conversion defaults are kept under:

```text
src/main/resources/config/profile/<cgmes|nc|iidm>/*.yml
```

These defaults are loaded and cached for use by concurrent parsing and
transformation workers.

## Frontend Configuration Layout

Vue applications load:

```text
public/config/base/<module>-application.json
public/config/<env>/<module>-application.json
```

`VITE_APP_ENV` selects the frontend environment and defaults to `local`.
Runtime configuration contains API base URLs such as CNM, IIDM, CSA, LF/SA, and
RAO endpoints.

## Docker Compose

`docker/docker-compose.yml` defines local infrastructure and runnable services:

- Elasticsearch
- MinIO
- RabbitMQ
- Keycloak
- OpenTelemetry collector
- backend services
- GUI Nginx containers
- mock services when requested

`docker/egm-compose.sh` wraps Docker Compose and supports service inclusion and
exclusion so mock services can be enabled only when needed.

`build-and-deploy.sh` cleans frontend build outputs, runs Maven, prunes local
Docker build/cache artifacts, builds selected images, and starts the local stack.

## Common Commands

Build all Maven modules:

```bash
mvn -Dmaven.repo.local=work/m2 verify
```

Package selected services:

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl srv.cnm.services,srv.iidm.transformer -am package
```

Build selected Docker services:

```bash
docker/egm-compose.sh build --no-cache srv-cnm-services gui-rcc-manager gui-cnm-manager
```

Run the local stack:

```bash
docker/egm-compose.sh up
```

## Local Infrastructure Notes

Object-storage buckets are initialized during service startup or adapter
initialization, not during concurrent uploads. RabbitMQ exchanges used by a
publisher are declared during startup. Elasticsearch list/search queries exclude
large payload fields where services provide lightweight metadata APIs.
