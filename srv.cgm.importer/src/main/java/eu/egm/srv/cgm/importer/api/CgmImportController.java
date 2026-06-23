package eu.egm.srv.cgm.importer.api;

import eu.egm.data.cgm.dto.cgmes.ImportStatus;
import eu.egm.data.cgm.dto.cgmes.CgmesProcess;
import eu.egm.data.cgm.dto.cgmes.CgmesRegion;
import eu.egm.srv.cgm.importer.service.CgmImportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cgm/imports")
public class CgmImportController {
    private final CgmImportService importService;

    public CgmImportController(CgmImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    @Operation(summary = "List imported CGMES network IDs and business context")
    public List<ImportStatus> listImports() {
        return importService.listImports();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import one or more CGMES RDF/XML or ZIP files and index searchable network data")
    public ImportStatus importCgmes(@RequestPart("file") @NotNull List<MultipartFile> files,
                                    @RequestParam("region") CgmesRegion region,
                                    @RequestParam("process") CgmesProcess process) {
        return importService.importCgmes(files, region, process);
    }

    @PostMapping("/processes/start")
    @Operation(summary = "Start the asynchronous CGM import BPM process for a stored import")
    public ImportStatus startCgmImport(@org.springframework.web.bind.annotation.RequestBody ImportStatus status) {
        return importService.startCgmImport(status);
    }

    @PostMapping("/{networkId}/transforms/cgmes")
    @Operation(summary = "Transform one stored raw CGMES object into searchable CGMES documents")
    public ImportStatus transformCgmes(@org.springframework.web.bind.annotation.PathVariable String networkId,
                                       @org.springframework.web.bind.annotation.RequestBody CgmesTransformRequest request) {
        return importService.transformObject(networkId, request.objectId());
    }

    @PostMapping("/{networkId}/statuses/files")
    @Operation(summary = "Update file-level import and IIDM transform status")
    public ImportStatus updateImportStatus(@org.springframework.web.bind.annotation.PathVariable String networkId,
                                           @org.springframework.web.bind.annotation.RequestBody ImportFileStatusUpdate request) {
        return importService.updateFileStatus(networkId, request.objectId(), request.status(),
                request.iidmTransformStatus(), request.documentIds(), request.message());
    }

    @GetMapping("/{networkId}/process-history")
    @Operation(summary = "Return process instance and per-file import status history for a network")
    public ImportStatus processHistory(@org.springframework.web.bind.annotation.PathVariable String networkId) {
        return importService.requireStatus(networkId);
    }

    public record CgmesTransformRequest(String objectId) {
    }

    public record ImportFileStatusUpdate(
            String objectId,
            String status,
            String iidmTransformStatus,
            List<String> documentIds,
            String message
    ) {
    }
}
