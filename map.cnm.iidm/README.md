# map.cnm.iidm

`map.cnm.iidm` transforms CGMES sources into real PowSyBl IIDM `Network`
objects wrapped by `data.iidm`.

The preferred transformer, `CgmesSourceToIidmTransformer`, delegates raw CGMES
RDF/XML source files to PowSyBl's native CIM-CGMES importer. PowSyBl loads the
complete source set into its RDF4J triplestore and performs CGMES-to-IIDM
conversion directly.

The compatibility transformer, `CnmToIidmTransformer`, remains available for
parsed CNM DTOs and diagnostic flows. It keeps a two-pass structure:

1. instantiate substations, voltage levels, and buses from topology objects.
2. resolve terminals/equipment associations into PowSyBl connectables.
