package eu.egm.map.cgmes.iidm;

import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.transformer.TransformerFactory;

/**
 * Abstract factory for the CGMES/IIDM transformer family.
 */
public interface CgmesIidmTransformerFactory extends TransformerFactory {
    MappingService createMappingService();

    CgmesIidmMappingConfiguration createMappingConfiguration();
}
