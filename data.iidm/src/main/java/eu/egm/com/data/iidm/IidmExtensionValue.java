package eu.egm.com.data.iidm;

import java.util.Map;

/**
 * Generic DTO for PowSyBl IIDM extension data attached to an equipment item.
 */
public record IidmExtensionValue(
        IidmExtensionType type,
        String powsyblName,
        Map<String, Object> values
) {
    public IidmExtensionValue {
        values = values == null ? Map.of() : Map.copyOf(values);
        if (powsyblName == null && type != null) {
            powsyblName = type.powsyblName();
        }
    }
}
