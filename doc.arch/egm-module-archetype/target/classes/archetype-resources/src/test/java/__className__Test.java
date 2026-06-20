package ${package};

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ${className}Test {
    @Test
    void exposesGeneratedModuleName() {
        assertThat(new ${className}().moduleName()).isEqualTo("${artifactId}");
    }
}
