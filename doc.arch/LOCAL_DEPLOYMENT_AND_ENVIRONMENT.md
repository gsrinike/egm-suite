# Local Deployment And Environment Resolution

## Local Deployment Principle

Local deployment is intentionally built from the same artifacts used by CI and Docker publication:

1. Maven builds Java modules and frontend bundles.
2. Docker Compose starts infrastructure and application containers.
3. Runtime configuration is resolved from environment-specific module files.

The goal is that a developer can reproduce the application stack without manual service setup.

## Local Build

Build the application without Docker image work:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true clean package
```

Run a focused backend slice:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl srv.cgm.importer -am test
```

Run the frontend build:

```bash
cd gui.cgm.explorer
npm run build
```

## Docker Compose

Start the local stack:

```bash
docker compose -f docker/docker-compose.yml up
```

The stack includes dependencies such as Elasticsearch, MinIO, RabbitMQ, OpenTelemetry, and Keycloak where applicable. The backend service and GUI images are built from local Maven/npm artifacts.

## Docker Maven Lifecycle

Docker behavior is configured from root Maven properties:

```xml
<docker.registry>docker.io</docker.registry>
<docker.namespace>gsrinike</docker.namespace>
<docker.image.tag>${project.version}</docker.image.tag>
<docker.image.latest-tag>latest</docker.image.latest-tag>
<docker.skip.build>false</docker.skip.build>
<docker.skip.push>false</docker.skip.push>
<maven.deploy.skip>true</maven.deploy.skip>
```

Docker-enabled modules configure only their module-specific image name. The full image name is assembled consistently from root properties.

## Environment Resolution Principle

Environment selection is centralized in `com.utils.env`:

1. JVM system property `env`
2. Operating system environment variable `ENV`
3. Default value `local`

The resolved value is normalized and used by `com.utils.config`.

## Configuration Loading Order

`com.utils.config` loads module configuration in this property-source order:

1. `<env>/<module>-application.xml`
2. `<env>/<module>-infra.xml`
3. `<env>/<module>-cache-config.yml`
4. `<env>/<module>-vault.xml`
5. `base/<module>-application.xml`
6. `base/<module>-infra.xml`
7. `base/<module>-cache-config.yml`
8. `base/<module>-vault.xml`

Earlier property sources have higher precedence. This makes base configuration stable while allowing `local`, `prod`, `sate`, or other environments to override only changed values.

## Cache Configuration

Cache configuration follows the same environment principle through `<module>-cache-config.yml`.

Current cache providers:

- `java`: in-memory Java cache implementation.
- `none`: disables cache behavior.

Cache, environment, and application configuration loading details are owned by `com.utils`.

## Vault Secret Resolution

Configuration values can use `${vault:SECRET_KEY}` when the application includes `com.vault`. If a module has Vault enabled in `<module>-vault.xml`, `com.vault` reads the secret from HashiCorp Vault. If Vault is not configured, resolution falls back to the matching environment variable and then to matching loaded configuration.

Before any value is returned, `com.vault` calls `com.auth.secret.SecretAuthorizationService`. This prevents an application from loading arbitrary configuration secrets merely by naming a key.

Example:

```xml
<entry key="vault.authorization.application-id">srv.cgm.importer</entry>
<entry key="vault.authorization.allowed-keys">MINIO_SECRET_KEY</entry>
<entry key="utility.object-storage.access-key">${vault:MINIO_SECRET_KEY}</entry>
```

## Module Requirements

Spring Boot applications should:

- Set `module` before `SpringApplication.run(...)`, or set `MODULE`.
- Include resources under `base`, `local`, and any supported runtime environment folders.
- Keep secrets outside committed files and provide them through environment variables or deployment configuration.

See:

- [com.utils README](../com.utils/README.md)
- [root README](../README.md)
