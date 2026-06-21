package eu.egm.srv.cgm.importer.service;

import eu.egm.data.cgm.dto.cgmes.EquipmentView;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;
import eu.egm.data.cgm.mapping.PowsyblCgmesEquipmentReader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class PowsyblCgmesNetworkReader implements CgmesNetworkReader {
    private final PowsyblCgmesEquipmentReader equipmentReader;

    public PowsyblCgmesNetworkReader() {
        this(new PowsyblCgmesEquipmentReader());
    }

    PowsyblCgmesNetworkReader(PowsyblCgmesEquipmentReader equipmentReader) {
        this.equipmentReader = equipmentReader;
    }

    @Override
    public List<EquipmentView> read(String networkId, ImportMetadata metadata, InputStream inputStream) {
        return equipmentReader.read(networkId, metadata, inputStream);
    }
}
