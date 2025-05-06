package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.data.programindicator.BoundarySqlBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.coalesce.ValueCoalescePolicy;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_EVENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.DB_SCHEDULED_DATE;

@Slf4j
public class PsDeCteFactory implements CteSqlFactory {

    private static final Pattern PATTERN = PlaceholderParser.psDePattern();
    private final DataElementService dataElementService;

    public PsDeCteFactory(DataElementService dataElementService) {
        this.dataElementService = dataElementService;
    }

    @Override
    public boolean supports(String rawSql) {
        return rawSql != null && rawSql.contains("__PSDE_CTE_PLACEHOLDER__");
    }

    @Override
    public String process(String rawSql,
                          ProgramIndicator programIndicator,
                          Date start,
                          Date end,
                          CteContext cteContext,
                          Map<String, String> aliasMap,
                          SqlBuilder sqlBuilder) {
        StringBuilder out = new StringBuilder();
        Matcher m = PATTERN.matcher(rawSql);

        while (m.find()) {
            Optional<PlaceholderParser.PsDeFields> opt = parse(m);
            if (opt.isEmpty()) {                     // malformed → leave untouched
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            PlaceholderParser.PsDeFields p = opt.get();
            int offset = p.offset();

            String key = buildCteKey(p, offset);
            ensureCte(key, p, offset, programIndicator, start, end, cteContext, sqlBuilder);

            CteDefinition def = cteContext.getDefinitionByKey(key);
            if (def == null) {                       // create failed → keep raw
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                continue;
            }

            String alias = def.getAlias();
            aliasMap.put(m.group(0), alias);

            String replacement = renderReplacement(alias, p.deUid());
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Determines the primary column to use for ordering events within the PS/DE CTE, mimicking logic
     * from DefaultStatementBuilder.
     *
     * @param programIndicator The program indicator.
     * @param sqlBuilder       The SQL builder for quoting.
     * @return The quoted column names to order by (e.g., `"occurreddate"` or `"scheduleddate"`).
     */
    private static String getOrderByColumn(ProgramIndicator programIndicator, SqlBuilder sqlBuilder) {
        AnalyticsPeriodBoundary boundary = programIndicator.getEndEventBoundary();
        String column = DB_EVENT_DATE; // Default
        if (boundary != null && boundary.isScheduledDateBoundary()) {
            column = DB_SCHEDULED_DATE;
        }
        if (!DB_SCHEDULED_DATE.equals(column)) {
            return sqlBuilder.quote(DB_EVENT_DATE);
        }
        return sqlBuilder.quote(DB_SCHEDULED_DATE);
    }

    private Optional<PlaceholderParser.PsDeFields> parse(Matcher m) {
        String raw = m.group(0);
        return PlaceholderParser.parsePsDe(raw);
    }

    private String buildCteKey(PlaceholderParser.PsDeFields p, int offset) {
        return new PsDeCteKey(p.psUid(), p.deUid(), offset, p.boundaryHash(), p.piUid()).toString();
    }

    private void ensureCte(String key, PlaceholderParser.PsDeFields p, int offset,
                           ProgramIndicator pi, Date start, Date end,
                           CteContext ctx, SqlBuilder qb) {

        if (ctx.containsCte(key)) return;                           // already present

        if (pi.getProgram() == null) {
            log.error("PI {} has no Program – cannot create CTE {}", pi.getUid(), key);
            return;
        }
        String table = "analytics_event_" + pi.getProgram().getUid();
        String boundaries = BoundarySqlBuilder.buildSql(
                pi.getAnalyticsPeriodBoundaries(),
                AnalyticsPeriodBoundary.DB_EVENT_DATE, pi, start, end, qb);

        String col = qb.quote(p.deUid());
        String orderCol = getOrderByColumn(pi, qb);
        String dir = offset <= 0 ? "desc" : "asc";
        int rank = offset <= 0 ? (-offset + 1) : offset;

        String bodySql = String.format(
                "select enrollment, %1$s as value, " +
                        "row_number() over (partition by enrollment order by %2$s %3$s) as rn " +
                        "from %4$s where %1$s is not null and ps = %5$s %6$s",
                col, orderCol, dir, table, qb.singleQuote(p.psUid()), boundaries);

        ctx.addProgramStageDataElementCte(
                key, CteDefinition.forProgramStageDataElement(key, bodySql, "enrollment", rank));
    }

    private String renderReplacement(String alias, String deUid) {
        DataElement de = dataElementService.getDataElement(deUid);
        ValueCoalescePolicy policy = de != null
                ? ValueCoalescePolicy.from(de.getValueType())
                : ValueCoalescePolicy.NUMBER;
        return policy.render(alias);
    }

    record PsDeCteKey(String psUid, String deUid, int offset, String boundaryHash, String piUid) {
        @Override
        public String toString() {
            // Format: psdecte_psUid_deUid_offset_boundaryHash_piUid
            return String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);
        }
    }
}
