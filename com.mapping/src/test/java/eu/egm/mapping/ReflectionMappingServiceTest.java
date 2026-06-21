package eu.egm.mapping;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionMappingServiceTest {
    private final MappingService mappingService = new ReflectionMappingService();

    @Test
    void mapsRecordsUsingConfiguredFieldsAndEnumValues() {
        MappingDefinition definition = new MappingDefinition(
                "left-to-right",
                SourceRecord.class,
                TargetRecord.class,
                List.of(
                        FieldMapping.of("identifier", "id"),
                        FieldMapping.of("kind", "type", Map.of("OLD", "NEW")),
                        FieldMapping.of("nested.name", "name")
                )
        );

        TargetRecord target = mappingService.map(new SourceRecord("ID-1", SourceType.OLD, new NestedRecord("mapped")), TargetRecord.class, definition);

        assertThat(target.id()).isEqualTo("ID-1");
        assertThat(target.type()).isEqualTo(TargetType.NEW);
        assertThat(target.name()).isEqualTo("mapped");
    }

    record SourceRecord(String identifier, SourceType kind, NestedRecord nested) {
    }

    record NestedRecord(String name) {
    }

    record TargetRecord(String id, TargetType type, String name) {
    }

    enum SourceType {
        OLD
    }

    enum TargetType {
        NEW
    }
}
