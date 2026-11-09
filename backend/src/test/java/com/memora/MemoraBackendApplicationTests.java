package com.memora;

import com.memora.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@IntegrationTest
class MemoraBackendApplicationTests {

    /**
     * Also asserts, implicitly but usefully, that every Flyway migration applies
     * cleanly and that {@code ddl-auto=validate} finds no drift between the JPA
     * entities and the migrated schema.
     */
    @Test
    @DisplayName("application context starts against a migrated database")
    void contextLoads() {}
}
