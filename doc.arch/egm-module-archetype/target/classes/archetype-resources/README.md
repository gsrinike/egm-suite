# ${artifactId}

## Purpose

`${artifactId}` is a `${moduleCategory}` module generated from `egm-module-archetype`.

Replace this section with the module's concrete purpose and ownership boundary.

## What It Contains

- `${className}`: initial generated class.

## Implementation Notes

Follow the classification rules in `doc.arch/MODULE_CLASSIFICATION.md`:

- `com.*`: reusable cross-cutting capability.
- `data.*`: stable DTO/domain vocabulary.
- `map.*`: source-to-target transformation.
- `srv.*`: backend service/application.
- `gui.*`: frontend application.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -Ddocker.skip.build=true -Ddocker.skip.push=true -pl ${artifactId} -am test
```
