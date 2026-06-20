# Architecture Documentation

`doc.arch` contains architecture-facing material for Energy Grid Management Suite developers.

## Contents

- `DESIGN_PRINCIPLES.md`: design principles adopted by the project and how they are applied in the current modules.
- `LOCAL_DEPLOYMENT_AND_ENVIRONMENT.md`: local deployment workflow, Docker/Maven behavior, and environment resolution rules.
- `MODULE_CLASSIFICATION.md`: classification rules for `com`, `data`, `map`, `srv`, and `gui` modules with links to module README files.
- `egm-module-archetype`: standalone Maven archetype for generating a new EGM Java module skeleton.

## Archetype Quick Start

Install the archetype locally:

```bash
mvn -f doc.arch/egm-module-archetype/pom.xml clean install
```

Generate a module skeleton:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=eu.egm \
  -DarchetypeArtifactId=egm-module-archetype \
  -DarchetypeVersion=0.1.0-SNAPSHOT \
  -DgroupId=eu.egm \
  -DartifactId=com.example.capability \
  -Dpackage=eu.egm.com.example.capability \
  -DclassName=ExampleCapability \
  -DmoduleCategory=com \
  -DinteractiveMode=false
```

After generation, add the new module to both root `pom.xml` and `modules.xml`, then add only the dependencies the module directly uses.
