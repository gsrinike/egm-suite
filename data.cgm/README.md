# data.cgm

## Purpose

`data.cgm` is the shared domain contract and CGM reading module for the CGMES explorer suite. It contains the Java records, enums, constants, parsing/classification helpers, PowSyBl-backed import reader, and fallback graph projection used by service modules and API payloads.

This module should stay free of storage, messaging, web, and UI concerns. It is the stable business-data vocabulary that other modules build on.

## What It Contains

- `eu.egm.data.cgm.dto.cgmes`: CGMES-facing DTOs and enums, including `CgmesConstants`, `CgmesRegion`, `CgmesProcess`, `CgmesProfileType`, `EquipmentType`, `EquipmentView`, `ImportMetadata`, `ImportStatus`, `NetworkDiff`, `PageResponse`, and `SearchRequest`.
- `eu.egm.data.cgm.dto.iidm`: IIDM-facing DTOs and enums, including `IidmNetwork`, `IidmEquipment`, `IidmEquipmentType`, `IidmExtensionType`, and `IidmExtensionValue`.
- `eu.egm.data.cgm.mapping`: reading, graph loading, projection, classification, and topology strategy classes.
- `PowsyblCgmesEquipmentReader`: reads complete CGMES XML/ZIP payloads through PowSyBl and projects the resulting IIDM network into `EquipmentView`.
- `EquipmentProjectionReader`: lightweight RDF/XML equipment projection fallback for incomplete CGMES profile snippets.
- `CgmProfileGraphLoader`: loads CGMES XML/RDF or ZIP payloads into an interim profile graph before mapping.
- `NodeBreakerTopologyMappingStrategy` and `BusBranchTopologyMappingStrategy`: strategy-based projections selected from loaded profile metadata.
- `AbstractTwoPassTopologyMappingStrategy`: performs the instantiate-then-associate projection used by fallback readers.
- `CgmIidmNetworkMapper`: converts indexed CGMES explorer projections into IIDM DTOs for API callers.
- `CgmesFileNameParser`: parses CGMES filenames such as `20231016T0030Z_1D_TSCNET-EU-MAVIR_SSH_000.zip`.

## Implementation Notes

The filename parser extracts:

- business day
- timestamp
- time frame
- TSO name
- CGMES profile type
- version number
- extension

`ImportMetadata` also validates that timestamps are aligned to 15-minute intervals. This keeps validation close to the shared data contract instead of scattering it through service code.

CGMES reading is owned here rather than in service modules. Complete CGMES profile sets are delegated to PowSyBl conversion, then `data.cgm` projects the IIDM network into stable internal DTOs.

The fallback `EquipmentProjectionReader` does not map raw XML directly into IIDM DTOs. It first loads the upload into `CgmProfileGraph`, then selects a topology strategy and runs a two-pass projection:

1. Instantiate supported equipment/state nodes by mRID.
2. Resolve topology and container associations through lookup maps.

This keeps parsing concerns separate from business mapping and allows node-breaker and bus-branch variants to evolve independently.

## Developer Commands

From the repository root:

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.cgm test
```

To build it as part of all backend modules:

```bash
mvn -Dmaven.repo.local=work/m2 -pl srv.cgm.importer -am test
```

## Dependency Rules

Keep this module free of Elasticsearch, MinIO, RabbitMQ, Spring MVC, and frontend implementation details. PowSyBl CGMES conversion/runtime dependencies are isolated here so service modules do not depend on PowSyBl directly. If a field is needed by multiple modules, add it here first in either `dto.cgmes` or `dto.iidm`, then propagate it outward.
