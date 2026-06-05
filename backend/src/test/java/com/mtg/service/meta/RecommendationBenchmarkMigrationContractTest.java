package com.mtg.service.meta;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationBenchmarkMigrationContractTest {

    @Test
    void v13RemovesOnlyLinkedLegacyProjectionsAndV14V15CreateBenchmarkTables() throws Exception {
        String v13 = Files.readString(Path.of("src/main/resources/db/migration/V13__replace_manual_top_decks_with_canonical_meta.sql"));
        String v14 = Files.readString(Path.of("src/main/resources/db/migration/V14__create_recommendation_benchmark.sql"));
        String v15 = Files.readString(Path.of("src/main/resources/db/migration/V15__create_benchmark_ai_artifacts.sql"));

        assertTrue(v13.contains("SELECT public_deck_id"));
        assertFalse(v13.contains("owner_id = 'external-import'"));
        assertTrue(v14.contains("recommendation_benchmark_runs"));
        assertTrue(v14.contains("recommendation_benchmark_case_results"));
        assertTrue(v14.contains("recommendation_benchmark_reviews"));
        assertTrue(v14.contains("UNIQUE (run_id, case_id, reviewer_id)"));
        assertTrue(v15.contains("recommendation_benchmark_ai_jobs"));
        assertTrue(v15.contains("recommendation_benchmark_ai_artifacts"));
        assertTrue(v15.contains("recommendation_benchmark_ai_sets"));
        assertTrue(v15.contains("UNIQUE (job_id, case_id, artifact_type, input_hash)"));
    }
}
