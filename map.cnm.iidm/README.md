# map.cnm.iidm

`map.cnm.iidm` converts CNM/CGMES inputs into PowSyBl IIDM networks. It is a
library module with no Spring or infrastructure ownership.

## Transformers

- `CgmesSourceToIidmTransformer`: preferred path. It stages raw CGMES files and
  delegates conversion to PowSyBl's native CGMES importer.
- `CnmToIidmTransformer`: compatibility path for parsed CNM profile DTOs and
  diagnostic scenarios.
- `CnmToIidmTransformerFactory`: factory for creating the compatibility
  transformer with mapping configuration.

The preferred path expects a complete source group, including boundary data when
required by the CGMES files.

## Developer Command

```bash
mvn -Dmaven.repo.local=work/m2 -pl map.cnm.iidm -am test
```
