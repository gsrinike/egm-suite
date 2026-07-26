# Repository Rules

## Active Modules

The active Maven reactor contains:

- `egm-dependencies`
- `com.utils`
- `com.mapping`
- `com.infra`
- `com.auth`
- `com.vault`
- `data.cnm`
- `data.iidm`
- `data.common`
- `map.cnm.iidm`
- `srv.cnm.services`
- `srv.iidm.transformer`
- `srv.common.lfsa`
- `mock.srv.common.lfsa`
- `mock.srv.cnm.services`
- `gui.common`
- `gui.cnm.manager`
- `gui.rcc.manager`

Keep `pom.xml` and `modules.yml` synchronized.

## Dependency Rules

- Shared modules expose stable contracts and hide implementation details.
- `com.vault` depends on `com.utils`; `com.auth` does not depend on `com.vault`.
- `com.infra` owns Elasticsearch, MinIO, RabbitMQ, and BPM adapter dependencies.
- `data.cnm` and `data.common` stay storage-neutral and framework-neutral.
- `data.common` groups LF/SA contracts by capability package:
  `lfsa.common` and `lfsa.sensitivity`.
- `data.iidm` is the PowSyBl DTO boundary and may depend on PowSyBl.
- `map.cnm.iidm` depends on data/mapping modules, not Spring or infrastructure.
- `srv.cnm.services` may publish `data.iidm` events but does not import
  `srv.iidm.transformer`.
- `srv.iidm.transformer` owns IIDM transform and network documents.
- Feature GUIs consume `gui.common`.

## Configuration Rules

Backend configuration is YAML and module-scoped. Environment selection uses
`env`, then `ENV`, then `local`. Do not add XML application configuration.

Vault references use `${vault:KEY}` and must be authorized before lookup.

## Infrastructure Rules

Use `InfrastructureUtils` for document repositories, object storage, event
publishing, and BPM services. Do not put application-specific index names,
queue names, process IDs, or parsing rules in `com.infra`.

Initialize object-storage buckets and RabbitMQ exchanges during startup or
adapter initialization, not inside concurrent hot paths.

## Documentation Rules

Documentation should describe the current as-is architecture. Avoid writing new
docs as a chronological list of incremental changes. Keep:

- root overview in `README.md`
- architecture overview in `doc.arch`
- module-specific ownership in module `README.md`
- reusable component details in `gui.common/COMPONENTS.md`

## Verification Rules

For code changes, run targeted tests or package commands with:

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true ...
```

For documentation-only changes, run scans for stale references and Markdown
formatting issues. Never stage or commit `work/m2`.
