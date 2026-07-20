# map.cnm.iidm

`map.cnm.iidm` transforms parsed CNM profile DTOs into real PowSyBl IIDM
`Network` objects wrapped by `data.iidm`.

The first increment creates profile-level PowSyBl networks from the extracted
common topology model. Full network aggregation across EQ, SSH, SV, and TP files
can be layered on top by grouping generated networks by `importId` and applying
profile updates in PowSyBl.

The mapper keeps a two-pass structure:

1. instantiate substations, voltage levels, and buses from topology objects.
2. resolve terminals/equipment associations into PowSyBl connectables.
