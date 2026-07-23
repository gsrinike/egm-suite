# com.mapping

`com.mapping` is the domain-neutral mapping module.

## Responsibilities

- `MappingService`: object mapping contract.
- `ReflectionMappingService`: default reflection-backed implementation.
- `MappingDefinition` and `FieldMapping`: declarative field mapping contracts.
- `MappingConfiguration`: parent configuration type for transformers.
- `eu.egm.mapping.transformer.Transformer`: generic transformer contract.
- `eu.egm.mapping.transformer.TransformerFactory`: typed transformer factory
  contract.
- `JsonMappingService`: JSON conversion contract used by services that persist
  DTO payloads as JSON.

Mapping code remains independent of Spring, infrastructure adapters, and domain
workflow logic.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl com.mapping test
```
