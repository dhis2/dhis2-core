package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.Optional;

@UtilityClass
public class SubselectMatchers {

    private static final LastSchedMatcher LAST_SCHED_MATCHER = new LastSchedMatcher();
    private static final LastCreatedMatcher LAST_CREATED_MATCHER = new LastCreatedMatcher();
    private static final LastEventValueMatcher LAST_EVENT_VALUE_MATCHER = new LastEventValueMatcher();
    private static final RelationshipCountMatcher RELATIONSHIP_COUNT_MATCHER = new RelationshipCountMatcher();
    private static final DataElementCountMatcher DATA_ELEMENT_COUNT_MATCHER = new DataElementCountMatcher();

    // ***********************************************************************
    // Public Pattern Matching Methods
    // ***********************************************************************

    /**
     * Checks if the given subselect matches the expected pattern:
     *
     * <pre>
     *   SELECT scheduleddate
     *   FROM &lt;some_table&gt;
     *   WHERE &lt;some_table&gt;.enrollment = subax.enrollment
     *     AND scheduleddate IS NOT NULL
     *   ORDER BY occurreddate DESC
     *   LIMIT 1
     * </pre>
     *
     * @param subSelect the subselect to check.
     * @return Optional containing FoundSubSelect metadata if the pattern matches.
     */
    public static Optional<FoundSubSelect> matchesLastSchedPattern(SubSelect subSelect) {
        return LAST_SCHED_MATCHER.match(subSelect);
    }

    /**
     * Checks if the given subselect (from an EXISTS expression) matches the expected pattern for last_created.
     * <p>
     * Expected pattern:
     *
     * <pre>
     *   SELECT created
     *   FROM &lt;table&gt;
     *   WHERE &lt;table&gt;.enrollment = subax.enrollment
     *     AND created IS NOT NULL
     *   ORDER BY occurreddate DESC
     *   LIMIT 1
     * </pre>
     *
     * @param subSelect the subselect to check.
     * @return Optional containing FoundSubSelect metadata if the pattern matches.
     */
    public static Optional<FoundSubSelect> matchesLastCreatedExistsPattern(SubSelect subSelect) {
        return LAST_CREATED_MATCHER.match(subSelect);
    }

    /**
     * Checks if the given subselect matches the pattern for selecting the last value of a specific column:
     *
     * <pre>
     *   SELECT "columnName"
     *   FROM &lt;table&gt;
     *   WHERE &lt;table&gt;.enrollment = subax.enrollment
     *     AND "columnName" IS NOT NULL
     *     AND ps = 'programStageId'
     *   ORDER BY occurreddate DESC
     *   LIMIT 1
     * </pre>
     *
     * @param subSelect the subselect to check.
     * @return Optional containing FoundSubSelect metadata if the pattern matches.
     */
    public static Optional<FoundSubSelect> matchesLastEventValuePattern(SubSelect subSelect) {
        return LAST_EVENT_VALUE_MATCHER.match(subSelect);
    }

    /**
     * Checks if the given subselect matches one of the relationship count patterns.
     * <p>
     * Pattern 1 (without relationship ID):
     * <pre>
     *   SELECT sum(relationship_count)
     *   FROM analytics_rs_relationship arr
     *   WHERE arr.trackedentityid = ax.trackedentity
     * </pre>
     *
     * Pattern 2 (with relationship ID):
     * <pre>
     *   SELECT relationship_count
     *   FROM analytics_rs_relationship arr
     *   WHERE arr.trackedentityid = ax.trackedentity
     *     AND relationshiptypeuid = 'specific_uid'
     * </pre>
     *
     * @param subSelect the subselect to check.
     * @return Optional containing FoundSubSelect metadata if the pattern matches.
     */
    public static Optional<FoundSubSelect> matchesRelationshipCountPattern(SubSelect subSelect) {
        return RELATIONSHIP_COUNT_MATCHER.match(subSelect);
    }

    /**
     * Checks if the given subselect matches the data element count pattern.
     * <p>
     * Expected pattern:
     *
     * <pre>
     *   SELECT count("dataElementId")
     *   FROM analytics_event_*
     *   WHERE analytics_event_*.enrollment = subax.enrollment
     *     AND "dataElementId" IS NOT NULL
     *     AND "dataElementId" = 1
     *     AND ps = 'programStageId'
     * </pre>
     *
     * @param subSelect the subselect to check.
     * @return Optional containing FoundSubSelect metadata if the pattern matches.
     */
    public static Optional<FoundSubSelect> matchesDataElementCountPattern(SubSelect subSelect) {
        return DATA_ELEMENT_COUNT_MATCHER.match(subSelect);
    }
}
