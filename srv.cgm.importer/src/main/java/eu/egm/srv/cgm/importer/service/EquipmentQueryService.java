package eu.egm.srv.cgm.importer.service;

import eu.egm.com.data.cgm.EquipmentView;
import eu.egm.com.data.cgm.NetworkDiff;
import eu.egm.com.data.cgm.PageResponse;
import eu.egm.com.data.cgm.SearchRequest;
import eu.egm.srv.cgm.importer.domain.EquipmentDocument;
import eu.egm.srv.cgm.importer.repository.EquipmentSearchRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EquipmentQueryService {
    private final EquipmentSearchRepository repository;

    public EquipmentQueryService(EquipmentSearchRepository repository) {
        this.repository = repository;
    }

    public PageResponse<EquipmentView> search(String networkId, SearchRequest request) {
        var page = repository.search(networkId, request);
        List<EquipmentView> results = page.content().stream()
                .map(EquipmentDocument::toView)
                .toList();
        return new PageResponse<>(results, Math.toIntExact(Math.min(page.total(), Integer.MAX_VALUE)), page.page(), page.size());
    }

    public NetworkDiff compare(String leftNetworkId, String rightNetworkId) {
        Map<String, EquipmentView> left = repository.findByNetworkId(leftNetworkId).stream()
                .map(EquipmentDocument::toView)
                .collect(Collectors.toMap(EquipmentView::id, Function.identity(), (first, second) -> first));
        Map<String, EquipmentView> right = repository.findByNetworkId(rightNetworkId).stream()
                .map(EquipmentDocument::toView)
                .collect(Collectors.toMap(EquipmentView::id, Function.identity(), (first, second) -> first));
        List<EquipmentView> added = right.entrySet().stream()
                .filter(entry -> !left.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        List<EquipmentView> removed = left.entrySet().stream()
                .filter(entry -> !right.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        List<NetworkDiff.ChangedEquipment> changed = left.entrySet().stream()
                .filter(entry -> right.containsKey(entry.getKey()))
                .map(entry -> changed(entry.getValue(), right.get(entry.getKey())))
                .filter(Objects::nonNull)
                .toList();
        return new NetworkDiff(leftNetworkId, rightNetworkId, added, removed, changed);
    }

    private NetworkDiff.ChangedEquipment changed(EquipmentView left, EquipmentView right) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(left.name(), right.name())) {
            fields.add("name");
        }
        if (left.type() != right.type()) {
            fields.add("type");
        }
        if (!Objects.equals(left.containerId(), right.containerId())) {
            fields.add("containerId");
        }
        if (Double.compare(left.nominalVoltage(), right.nominalVoltage()) != 0) {
            fields.add("nominalVoltage");
        }
        if (!Objects.equals(left.attributes(), right.attributes())) {
            fields.add("attributes");
        }
        return fields.isEmpty() ? null : new NetworkDiff.ChangedEquipment(left, right, fields);
    }
}
