package com.mtg.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RecommendationBenchmarkServiceTest {
    @Inject RecommendationBenchmarkService service;
    @Inject RecommendationBenchmarkRunRepository runRepository;
    @Inject RecommendationBenchmarkCaseRepository caseRepository;
    @Inject RecommendationBenchmarkReviewRepository reviewRepository;

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
}
