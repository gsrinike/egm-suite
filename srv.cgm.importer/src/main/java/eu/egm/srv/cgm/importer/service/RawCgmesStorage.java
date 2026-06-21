package eu.egm.srv.cgm.importer.service;

import com.infra.storage.ObjectStorageService;
import eu.egm.com.data.cgm.CgmesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RawCgmesStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawCgmesStorage.class);
    private final ObjectStorageService objectStorageService;

    public RawCgmesStorage(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    public void store(String objectName, byte[] bytes, String contentType) {
        objectStorageService.store(CgmesConstants.RAW_BUCKET, objectName, bytes, contentType);
        LOGGER.info("Stored raw CGMES object {}/{}", CgmesConstants.RAW_BUCKET, objectName);
    }
}
