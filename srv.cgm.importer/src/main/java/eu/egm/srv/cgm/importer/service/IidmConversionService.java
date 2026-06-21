package eu.egm.srv.cgm.importer.service;

import eu.egm.data.cgm.mapping.CgmIidmNetworkMapper;
import eu.egm.data.cgm.dto.iidm.IidmNetwork;
import eu.egm.srv.cgm.importer.domain.EquipmentDocument;
import eu.egm.srv.cgm.importer.repository.EquipmentSearchRepository;
import org.springframework.stereotype.Service;

/**
 * Converts persisted CGMES explorer projections into IIDM-oriented DTOs.
 *
 * The transformer module owns CGMES/IIDM vocabulary mapping. This service only
 * coordinates repository reads and exposes the converted model to API callers.
 */
@Service
public class IidmConversionService {
    private final EquipmentSearchRepository repository;
    private final CgmIidmNetworkMapper mapper;

    public IidmConversionService(EquipmentSearchRepository repository) {
        this.repository = repository;
        this.mapper = new CgmIidmNetworkMapper();
    }

    public IidmNetwork convert(String networkId) {
        var equipment = repository.findByNetworkId(networkId).stream()
                .map(EquipmentDocument::toView)
                .toList();
        return mapper.mapNetwork(networkId, equipment);
    }
}
