# data.iidm

## Purpose

`data.iidm` contains IIDM-facing data definitions used by mapping and service modules. Its extension definitions are aligned with PowSyBl `com.powsybl:powsybl-iidm-extensions`.

## What It Contains

- `IidmNetwork`: network-level transfer object with a stable id, display name, and equipment list.
- `IidmEquipment`: searchable equipment projection.
- `IidmEquipmentType`: normalized IIDM-oriented equipment categories.
- `IidmExtensionType`: supported PowSyBl IIDM extension contracts.
- `IidmExtensionValue`: generic DTO for extension payload values attached to equipment.
- `PowsyblIidmExtensionDefinition`: catalog of supported PowSyBl IIDM extensions.

## Implementation Notes

Records defensively copy list and map values to keep DTOs immutable after construction. Domain-specific conversion logic belongs in mapping modules such as `map.cgmes.iidm`, not in this data module. This module may reference PowSyBl IIDM extension interfaces, but it should not create runtime networks or own service adapter logic.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.iidm test
```
