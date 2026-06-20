package eu.egm.com.data.iidm;

import java.util.List;

/**
 * Minimal IIDM network projection used as an application-level transfer object.
 */
public record IidmNetwork(
        String id,
        String name,
        List<IidmEquipment> equipment
) {
    public IidmNetwork {
        equipment = equipment == null ? List.of() : List.copyOf(equipment);
    }
}
