package com.security.forecsic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherStatsResponse {

    private boolean success;
    private StatsData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsData {
        private long journalsPublished;
        private long issuesPublished;
        private long papersPublished;
        private long registeredUsers;
        private long totalDownloads;
        private List<PieChartItem> pieChart;
        private PerformanceMetrics metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieChartItem {
        private String name;
        private long value;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private int acceptanceRate;
        private int rejectionRate;
        private int avgReviewDays;
        private int avgPublishDays;
    }
}
