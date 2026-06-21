package eu.egm.map.cgm;

import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.transformer.TransformerFactory;

/**
 * Abstract factory for the CGMES/IIDM transformer family.
 */
public interface CgmesIidmTransformerFactory extends TransformerFactory {
    MappingService createMappingService();

    CgmesIidmMappingConfiguration createMappingConfiguration();
}
