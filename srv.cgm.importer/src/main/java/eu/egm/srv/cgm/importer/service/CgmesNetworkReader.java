package eu.egm.srv.cgm.importer.service;

import eu.egm.data.cgm.dto.cgmes.EquipmentView;
import eu.egm.data.cgm.dto.cgmes.ImportMetadata;

import java.io.InputStream;
import java.util.List;

public interface CgmesNetworkReader {
    List<EquipmentView> read(String networkId, ImportMetadata metadata, InputStream inputStream);
}
