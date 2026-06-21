package eu.egm.srv.cgm.importer.api;

import eu.egm.com.data.cgm.EquipmentView;
import eu.egm.com.data.cgm.NetworkDiff;
import eu.egm.com.data.cgm.PageResponse;
import eu.egm.com.data.cgm.SearchRequest;
import eu.egm.srv.cgm.importer.service.EquipmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cgm/networks")
public class EquipmentController {
    private final EquipmentQueryService queryService;

    public EquipmentController(EquipmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{networkId}/equipment")
    @Operation(summary = "Search and filter indexed network equipment")
    public PageResponse<EquipmentView> search(@PathVariable("networkId") String networkId, @Valid @ModelAttribute SearchRequest request) {
        return queryService.search(networkId, request);
    }

    @GetMapping("/{leftNetworkId}/compare/{rightNetworkId}")
    @Operation(summary = "Compare two imported network states")
    public NetworkDiff compare(@PathVariable("leftNetworkId") String leftNetworkId, @PathVariable("rightNetworkId") String rightNetworkId) {
        return queryService.compare(leftNetworkId, rightNetworkId);
    }
}
