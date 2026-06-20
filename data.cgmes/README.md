# data.cgmes

## Purpose

`data.cgmes` is the shared domain contract module for the CGMES explorer suite. It contains the Java records, enums, constants, and small parsing/classification helpers that are used by service modules and API payloads. Its CGMES profile and CIM naming definitions are aligned with PowSyBl `com.powsybl:powsybl-cgmes-model`.

This module should stay free of storage, messaging, web, and UI concerns. It is the stable business-data vocabulary that other modules build on.

## What It Contains

- `CgmesConstants`: shared index, bucket, and messaging names used across modules.
- `CgmesRegion`: supported capacity calculation regions: `CORE`, `HANSA`, `IBWT`, `SWE`.
- `CgmesProcess`: supported study processes: `CGM`, `CSA`, `CC`, `OPC`, `STA`.
- `CgmesProfileType`: file profile codes mapped to PowSyBl `CgmesSubset` values.
- `PowsyblCgmesModelDefinition`: supported PowSyBl CGMES model boundary types and searchable CIM names.
- `EquipmentType`: normalized equipment categories used by search and comparison.
- `EquipmentView`: API-facing representation of an indexed network element.
- `ImportMetadata`: business context attached to imported data.
- `ImportStatus`: import result/status returned by the backend and shown in the GUI.
- `NetworkDiff`: comparison result between two imported network states.
- `PageResponse`: generic paged response wrapper.
- `SearchRequest`: filter request used for equipment search.
- `EquipmentClassifier`: maps CIM profile classes to normalized `EquipmentType` values.
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

## Developer Commands

From the repository root:

```bash
mvn -Dmaven.repo.local=work/m2 -pl data.cgmes test
```

To build it as part of all backend modules:

```bash
mvn -Dmaven.repo.local=work/m2 -pl srv.cgm.importer -am test
```

## Dependency Rules

Keep this module lightweight. It may depend on PowSyBl CGMES model contracts, but it should not depend on Elasticsearch, MinIO, RabbitMQ, Spring MVC, or frontend implementation details. If a field is needed by multiple modules, add it here first and then propagate it outward.
