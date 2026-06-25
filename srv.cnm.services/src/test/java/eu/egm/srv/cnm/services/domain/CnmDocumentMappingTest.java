package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ProfileFamily;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import static org.assertj.core.api.Assertions.assertThat;

class CnmDocumentMappingTest {

    private final MappingElasticsearchConverter converter =
            new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());

    CnmDocumentMappingTest() {
        converter.afterPropertiesSet();
    }

    @Test
    void readsImportAndNestedFileTimestampsWithoutTemporalConverters() {
        long timestamp = Instant.parse("2026-06-24T12:53:54.152Z").toEpochMilli();
        Document source = Document.from(Map.of(
                "id", "import-1",
                "serviceType", "CGM",
                "timeFrame", "DAY_AHEAD",
                "state", "STORED",
                "files", List.of(Map.ofEntries(
                        Map.entry("fileId", "file-1"),
                        Map.entry("fileName", "20241202T2330Z_1D_TSCNET-EU_SV_002.xml"),
                        Map.entry("objectId", "import-1/model.xml"),
                        Map.entry("state", "PARSED"),
                        Map.entry("profileFamily", "SV"),
                        Map.entry("businessDay", "2024-12-02"),
                        Map.entry("businessTime", "23:30"),
                        Map.entry("modelTimeFrame", "1D"),
                        Map.entry("tsoName", "TSCNET-EU"),
                        Map.entry("profileType", "SV"),
                        Map.entry("modelVersion", "002"),
                        Map.entry("profiles", List.of()),
                        Map.entry("message", "Parsed"),
                        Map.entry("uploadedAt", timestamp))),
                "createdAt", timestamp,
                "message", "Imported"));

        CnmImportDocument restored = converter.read(CnmImportDocument.class, source);

        assertThat(restored.createdAt()).isEqualTo(timestamp);
        assertThat(restored.files().get(0).uploadedAt()).isEqualTo(timestamp);
        assertThat(restored.files().get(0).state()).isEqualTo(ImportFileState.PARSED);
    }

    @Test
    void readsProfileCodeFamilyStateAndTimestampIndependently() {
        long timestamp = Instant.parse("2026-06-24T12:53:54.152Z").toEpochMilli();
        Document source = Document.from(Map.ofEntries(
                Map.entry("id", "file-1"),
                Map.entry("importId", "import-1"),
                Map.entry("fileName", "20241202T2330Z_1D_TSCNET-EU_SV_002.xml"),
                Map.entry("objectId", "import-1/model.xml"),
                Map.entry("state", "PARSED"),
                Map.entry("profileFamily", "SV"),
                Map.entry("profileType", "SV"),
                Map.entry("tsoName", "TSCNET-EU"),
                Map.entry("businessDay", "2024-12-02"),
                Map.entry("businessTime", "23:30"),
                Map.entry("timeFrame", "1D"),
                Map.entry("version", "002"),
                Map.entry("importedAt", timestamp)));

        CnmProfileDocument restored = converter.read(CnmProfileDocument.class, source);

        assertThat(restored.state()).isEqualTo(ImportFileState.PARSED);
        assertThat(restored.profileFamily()).isEqualTo(ProfileFamily.SV);
        assertThat(restored.profileType()).isEqualTo("SV");
        assertThat(restored.importedAt()).isEqualTo(timestamp);
    }
}
