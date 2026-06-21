package eu.egm.com.data.cgm;

import java.util.Arrays;
import java.util.List;

/**
 * Catalog of PowSyBl IIDM extension contracts used by the local DTO module.
 */
public final class PowsyblIidmExtensionDefinition {
    public static final List<IidmExtensionType> SUPPORTED_EXTENSIONS = Arrays.asList(IidmExtensionType.values());

    private PowsyblIidmExtensionDefinition() {
    }
}
