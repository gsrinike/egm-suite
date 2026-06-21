package eu.egm.srv.cgm.importer.service;

import eu.egm.com.data.cgm.EquipmentView;
import eu.egm.com.data.cgm.ImportMetadata;

import java.io.InputStream;
import java.util.List;

public interface CgmesNetworkReader {
    List<EquipmentView> read(String networkId, ImportMetadata metadata, InputStream inputStream);
}
