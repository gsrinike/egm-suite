# ${artifactId}

`${artifactId}` is a `${moduleCategory}` module generated from
`egm-module-archetype`.

## Responsibility

Replace this section with the module's current purpose, owner boundary, and
allowed dependencies.

## Contents

- `${className}`: generated starter class.

## Rules

Follow `doc.arch/MODULE_CLASSIFICATION.md`:

- `com.*`: shared capability
- `data.*`: DTO contracts
- `map.*`: transformation library
- `srv.*`: runnable service
- `mock.srv.*`: mock runnable service
- `bpm.*`: process module
- `gui.*`: frontend module

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 \
  -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true \
  -pl ${artifactId} -am test
```
