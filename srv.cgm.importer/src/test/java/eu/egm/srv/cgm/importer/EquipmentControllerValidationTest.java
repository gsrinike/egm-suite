package eu.egm.srv.cgm.importer;

import eu.egm.com.data.cgm.PageResponse;
import eu.egm.com.data.cgm.SearchRequest;
import eu.egm.srv.cgm.importer.api.EquipmentController;
import eu.egm.srv.cgm.importer.service.EquipmentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EquipmentController.class, properties = "module=srv.cgm.importer")
class EquipmentControllerValidationTest {
    static {
        System.setProperty("module", "srv.cgm.importer");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipmentQueryService queryService;

    @Test
    void acceptsSearchRequestWhenBeanValidationProviderIsAvailable() throws Exception {
        when(queryService.search(eq("network-a"), any(SearchRequest.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/cgm/networks/network-a/equipment")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidSearchRequestWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/cgm/networks/network-a/equipment")
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isBadRequest());
    }
}
