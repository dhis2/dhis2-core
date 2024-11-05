package org.hisp.dhis.analytics.table;

import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.scheduling.JobProgress;

import java.util.List;

public interface AnalyticsTableStrategy {

    /**
     * Execute some sanity checks to ensure that the database is in a valid state for analytics table generation.
     * The checks are dependent on the table type.
     *
     * @param progress the job progress object
     * @param tableType the table type
     * @return true if the state is valid, false otherwise
     */
    default boolean validateState(JobProgress progress, AnalyticsTableType tableType) {
        return true;
    }

    default TableStrategyOpResult preCreateTables(AnalyticsTableUpdateParams params, JobProgress progress) {
        return TableStrategyOpResult.SKIPPED;
    }

    default TableStrategyOpResult dropStagingTables(List<AnalyticsTable> tables, JobProgress progress) {
        return TableStrategyOpResult.SKIPPED;
    }

    default TableStrategyOpResult createTables(List<AnalyticsTable> tables, JobProgress progress) {
        return TableStrategyOpResult.SKIPPED;
    }

    default TableStrategyOpResult populateTables(AnalyticsTableUpdateParams params, List<AnalyticsTablePartition> tables, JobProgress progress) {
        return TableStrategyOpResult.SKIPPED;
    }

    default int applyAggregationLevels(AnalyticsTableType tableType, List<? extends Table> tables, JobProgress progress) {
        return 0;
    }

    default TableStrategyOpResult createIndexes(List<Index> tables, JobProgress progress, AnalyticsTableType tableType) {
        return TableStrategyOpResult.SKIPPED;
    }

    default TableStrategyOpResult optimizeTables(List<AnalyticsTable> tables, JobProgress progress) {
        return TableStrategyOpResult.SKIPPED;
    }

    default TableStrategyOpResult analyzeTables(List<AnalyticsTable> tables, JobProgress progress) {
        return TableStrategyOpResult.SKIPPED;
    }

    default TableStrategyOpResult swapTables(AnalyticsTableUpdateParams params, List<AnalyticsTable> tables, JobProgress progress, AnalyticsTableType tableType) {
        return TableStrategyOpResult.SKIPPED;
    }
}
