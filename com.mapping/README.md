# com.mapping

## Purpose

`com.mapping` provides generic, configuration-driven object mapping for A-to-B and B-to-A transformations. It is domain-neutral and can be reused by any service or mapping module.

## What It Contains

- `MappingService`: public mapping contract.
- `MappingDefinition`: source type, target type, and field mapping configuration.
- `FieldMapping`: one source path to one target path, with optional value mappings.
- `ReflectionMappingService`: default adapter that maps records and simple mutable objects.
- `MappingException`: runtime exception for invalid mapping definitions or unsupported conversions.

## Implementation Notes

The default implementation reads record accessors, JavaBean getters, or fields. Record targets are built through their canonical constructor. Simple mutable targets are created through a no-argument constructor and populated by fields.

Enum vocabulary differences are handled through `FieldMapping.valueMappings`, which lets a domain module map values such as `TRANSFORMER` to `TWO_WINDINGS_TRANSFORMER` without hardcoding that rule in the generic mapper.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.mapping test
```
