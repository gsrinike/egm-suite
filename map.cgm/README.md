# map.cgm

## Purpose

`map.cgm` contains the CGMES-to-IIDM and IIDM-to-CGMES transformation layer. It follows the same separation visible in GridSuite-style architectures: shared data contracts, reusable services, and dedicated mapping/adaptation modules are kept separate from application services.

## What It Contains

- `CGMES2IIDMTransformer`: converts CGMES explorer DTOs to IIDM-oriented DTOs.
- `IIDM2CGMESTransformer`: converts IIDM-oriented DTOs back to CGMES explorer DTOs with caller-provided network context.
- `CgmesIidmMappingConfiguration`: field and enum mapping definitions consumed by `MappingService`.

## Implementation Notes

The transformer delegates mechanical field copying to `com.mapping.MappingService`. Domain-specific vocabulary differences, such as `TRANSFORMER` to `TWO_WINDINGS_TRANSFORMER`, are expressed in mapping configuration.

This module currently uses lightweight DTO projections. A production PowSyBl integration can add adapter classes here to bridge these DTOs to PowSyBl IIDM objects while keeping service modules insulated from mapper details.

## Developer Commands

```bash
mvn -Dmaven.repo.local=work/m2 -pl map.cgm -am test
```
