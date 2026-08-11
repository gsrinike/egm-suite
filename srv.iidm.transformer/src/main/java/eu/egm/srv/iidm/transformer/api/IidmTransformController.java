package eu.egm.srv.iidm.transformer.api;

import eu.egm.srv.iidm.transformer.domain.IidmNetworkDocument;
import eu.egm.srv.iidm.transformer.domain.IidmProfileTransformDocument;
import eu.egm.srv.iidm.transformer.service.IidmProfileTransformService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iidm")
public class IidmTransformController {
    private final IidmProfileTransformService transformService;

    public IidmTransformController(IidmProfileTransformService transformService) {
        this.transformService = transformService;
    }

    @GetMapping("/transforms")
    public IidmPage<IidmTransformSummaryResponse> transforms(
            @RequestParam(required = false) String importId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return transformService.transformSummaries(importId, search, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/transforms/{fileId}")
    public IidmProfileTransformDocument transform(@PathVariable String fileId) {
        return transformService.transformByFileId(fileId);
    }

    @GetMapping("/networks")
    public IidmPage<IidmNetworkSummaryResponse> networks(
            @RequestParam(required = false) String importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return transformService.networkSummaries(importId, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/networks/{networkId}")
    public IidmNetworkDocument network(@PathVariable String networkId) {
        return transformService.network(networkId);
    }

    @GetMapping("/imports/{importId}/files/{fileId}/tables")
    public IidmTableBundle networkTablesForFile(@PathVariable String importId, @PathVariable String fileId) {
        return transformService.networkTablesForFile(importId, fileId);
    }

    @GetMapping("/networks/{networkId}/tables")
    public IidmTableBundle networkTableMetadata(@PathVariable String networkId) {
        return transformService.networkTableMetadata(networkId);
    }

    @GetMapping("/networks/{networkId}/tables/{tableId}")
    public IidmTableBundle networkTable(
            @PathVariable String networkId,
            @PathVariable String tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String search) {
        return transformService.networkTable(networkId, tableId, Math.max(page, 0), Math.min(Math.max(size, 1), 500), search);
    }

    @GetMapping("/networks/{networkId}/grid-view/tables")
    public IidmTableBundle gridViewTableMetadata(@PathVariable String networkId) {
        return transformService.gridViewTableMetadata(networkId);
    }

    @GetMapping("/networks/{networkId}/grid-view/tables/{tableId}")
    public IidmTableBundle gridViewTable(
            @PathVariable String networkId,
            @PathVariable String tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String search) {
        return transformService.gridViewTable(networkId, tableId, Math.max(page, 0), Math.min(Math.max(size, 1), 500), search);
    }

    @PostMapping("/networks/{networkId}/grid-view/map")
    public IidmGridViewMapResponse generateGridViewMap(@PathVariable String networkId) {
        return transformService.gridViewMap(networkId, true);
    }

    @GetMapping("/networks/{networkId}/grid-view/map")
    public IidmGridViewMapResponse gridViewMap(@PathVariable String networkId) {
        return transformService.gridViewMap(networkId, false);
    }

    @GetMapping("/networks/{networkId}/grid-view/map-data")
    public IidmGridViewMapDataResponse gridViewMapData(@PathVariable String networkId) {
        return transformService.gridViewMapData(networkId);
    }
}
