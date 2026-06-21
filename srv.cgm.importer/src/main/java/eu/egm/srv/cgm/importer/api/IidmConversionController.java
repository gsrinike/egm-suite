package eu.egm.srv.cgm.importer.api;

import eu.egm.data.cgm.dto.iidm.IidmNetwork;
import eu.egm.srv.cgm.importer.service.IidmConversionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cgm/networks")
public class IidmConversionController {
    private final IidmConversionService conversionService;

    public IidmConversionController(IidmConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/{networkId}/iidm")
    @Operation(summary = "Convert an imported CGMES network projection to an IIDM-oriented model")
    public IidmNetwork convert(@PathVariable("networkId") String networkId) {
        return conversionService.convert(networkId);
    }
}
