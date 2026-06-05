package com.mtg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtg.dto.RecommendationBenchmarkReviewRequestDTO;
import com.mtg.dto.RecommendationBenchmarkSummaryDTO;
import com.mtg.repository.RecommendationBenchmarkCaseRepository;
import com.mtg.repository.RecommendationBenchmarkReviewRepository;
import com.mtg.repository.RecommendationBenchmarkRunRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RecommendationBenchmarkServiceTest {
    @Inject RecommendationBenchmarkService service;
    @Inject RecommendationBenchmarkRunRepository runRepository;
    @Inject RecommendationBenchmarkCaseRepository caseRepository;
    @Inject RecommendationBenchmarkReviewRepository reviewRepository;
    @Inject RecommendationBenchmarkAiService aiService;
    @Inject RecommendationBenchmarkScenarioService scenarioService;
    @Inject ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void clean() {
        reviewRepository.deleteAll();
        caseRepository.deleteAll();
        runRepository.deleteAll();
    }

    @Test
    void calculatesTwentyOfflineCasesAndPreservesVersionedResults() {
        RecommendationBenchmarkSummaryDTO result = service.run();

        assertEquals(20, result.getTotalCases());
        assertEquals(20, result.getEvaluatedCases());
        assertNotNull(result.getLastRunId());
        assertEquals(20, caseRepository.byRun(result.getLastRunId()).size());
        assertTrue(result.getMetrics().stream().anyMatch(metric -> "addPrecisionAt10".equals(metric.getName()) && metric.getSampleSize() > 0));
        assertTrue(result.getCoverage().size() >= 8);
    }

    @Test
    void blindReviewRequiresThreeVotesAndNeverExposesSystemOption() {
        RecommendationBenchmarkSummaryDTO result = service.run();
        var next = service.nextReview("reviewer-1");

        assertNotNull(next);
        service.review(next.caseId(), new RecommendationBenchmarkReviewRequestDTO(result.getLastRunId(), "A"), "reviewer-1");
        service.review(next.caseId(), new RecommendationBenchmarkReviewRequestDTO(result.getLastRunId(), "A"), "reviewer-2");
        service.review(next.caseId(), new RecommendationBenchmarkReviewRequestDTO(result.getLastRunId(), "tie"), "reviewer-3");

        RecommendationBenchmarkSummaryDTO summary = service.summary();
        assertEquals(1, summary.getReviewProgress().get("completedCases"));
        assertEquals(3, summary.getReviewProgress().get("votes"));
        assertTrue(summary.getMetrics().stream().anyMatch(metric -> "blindPreferenceWinRate".equals(metric.getName()) && metric.getSampleSize() == 1));
    }

    @Test
    void blocksAiGenerationPreviewUntilCorpusHasFiftyRealCompleteDecks() {
        var preview = aiService.preview();
        var summary = service.summary();

        assertEquals(20, preview.totalCases());
        assertEquals("corpus_not_ready", preview.status());
        assertEquals(25, summary.getCorpusStatus().get("validCases"), summary.getCorpusStatus().toString());
        assertEquals(25, summary.getCorpusStatus().get("archidektCases"));
    }

    @Test
    void reportsWhyLegacyFixtureDoesNotQualifyAsRealCorpus() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("recommendation-benchmark/cases-v1.json")) {
            var fixture = objectMapper.readTree(input).path("cases").get(0);
            var violations = scenarioService.validateFixture(fixture);

            assertTrue(violations.contains("unapproved_source"));
            assertTrue(violations.contains("missing_source_url"));
            assertTrue(violations.contains("missing_capture_date"));
            assertTrue(violations.contains("deck_must_include_commander_plus_99"));
        }
    }

    @Test
    void validatesFrozenArchidektSnapshots() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("recommendation-benchmark/archidekt-snapshots.json")) {
            var cases = objectMapper.readTree(input).path("cases");
            java.util.List<String> violations = new java.util.ArrayList<>();
            cases.forEach(fixture -> scenarioService.validateFixture(fixture)
                    .forEach(violation -> violations.add(fixture.path("id").asText() + ":" + violation)));

            assertEquals(25, cases.size());
            assertTrue(violations.isEmpty(), violations.toString());
        }
    }
}
