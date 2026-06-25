package eu.egm.srv.cnm.services.api;

import eu.egm.data.cnm.common.CnmPage;
import eu.egm.data.cnm.common.ChunkUploadCompleteRequest;
import eu.egm.data.cnm.common.CnmProfileMetadata;
import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.ImportFailureRequest;
import eu.egm.data.cnm.common.ImportFileStatusUpdateRequest;
import eu.egm.data.cnm.common.ImportStatus;
import eu.egm.data.cnm.common.TimeFrame;
import eu.egm.srv.cnm.services.service.CnmImportRestService;
import eu.egm.srv.cnm.services.service.CnmChunkUploadService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cnm/imports")
public class CnmImportController {
    private final CnmImportRestService importService;
    private final CnmChunkUploadService chunkUploadService;

    public CnmImportController(CnmImportRestService importService, CnmChunkUploadService chunkUploadService) {
        this.importService = importService;
        this.chunkUploadService = chunkUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportStatus importModel(
            @RequestParam CnmServiceType serviceType,
            @RequestParam TimeFrame timeFrame,
            @RequestParam(required = false) String importId,
            @RequestParam(required = false) String message,
            @RequestPart(value = "file", required = false) List<MultipartFile> file,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        List<MultipartFile> uploads = new ArrayList<>();
        if (file != null) {
            uploads.addAll(file);
        }
        if (files != null) {
            uploads.addAll(files);
        }
        return importService.importModels(uploads, serviceType, timeFrame, importId, message);
    }

    @PostMapping(value = "/failures", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportStatus reportFailure(@RequestBody ImportFailureRequest request) {
        return importService.reportFailure(request);
    }

    @PostMapping(value = "/chunks", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadChunk(
            @RequestParam String importId,
            @RequestParam String fileId,
            @RequestParam String fileName,
            @RequestParam int chunkIndex,
            @RequestParam int totalChunks,
            @RequestParam long fileSize,
            @RequestBody byte[] chunk) throws IOException {
        chunkUploadService.storeChunk(importId, fileId, fileName, chunkIndex, totalChunks, fileSize, chunk);
    }

    @PostMapping(value = "/chunks/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportStatus completeChunkUpload(@RequestBody ChunkUploadCompleteRequest request) throws IOException {
        try {
            return importService.importModels(
                    chunkUploadService.complete(request.importId()),
                    request.serviceType(),
                    request.timeFrame(),
                    request.importId(),
                    request.message());
        } finally {
            chunkUploadService.discard(request.importId());
        }
    }

    @GetMapping("/profiles")
    public CnmPage<CnmProfileMetadata> profiles(
            @RequestParam(required = false) String profileType,
            @RequestParam(required = false) String tso,
            @RequestParam(required = false) String businessDay,
            @RequestParam(required = false) String businessTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return importService.searchProfiles(profileType, tso, businessDay, businessTime, page, size);
    }

    @GetMapping
    public CnmPage<ImportStatus> imports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return importService.listImports(page, size);
    }

    @GetMapping("/{importId}")
    public ImportStatus importById(@PathVariable String importId) {
        return importService.findImport(importId);
    }

    @PutMapping(value = "/{importId}/files/{fileId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportStatus updateFileStatus(
            @PathVariable String importId,
            @PathVariable String fileId,
            @RequestBody ImportFileStatusUpdateRequest request) {
        return importService.updateFileStatus(importId, fileId, request);
    }
}
