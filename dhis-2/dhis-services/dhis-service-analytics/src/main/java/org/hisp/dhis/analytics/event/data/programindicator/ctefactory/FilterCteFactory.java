package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.event.data.programindicator.BoundarySqlBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser.FilterFields;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.ProgramIndicator;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterCteFactory implements CteSqlFactory {

    private static final Pattern PATTERN = PlaceholderParser.filterPattern();
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

    private static final Pattern LONE_OP      = Pattern.compile("^\\s*(AND|OR)\\s*$");
    private static final Pattern LEADING_OP   = Pattern.compile("^\\s*(AND|OR)\\s+");
    private static final Pattern TRAILING_OP  = Pattern.compile("\\s+(AND|OR)\\s*$");

    @Override
    public boolean supports(String rawSql) {
        return rawSql != null && rawSql.contains("V{");
    }

    @Override
    public String process(String rawSql,
                          ProgramIndicator pi,
                          Date start,
                          Date end,
                          CteContext ctx,
                          Map<String,String> aliasMap,
                          SqlBuilder qb) {

        if (StringUtils.isBlank(rawSql)) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        boolean simpleFound = false;
        Matcher m = PATTERN.matcher(rawSql);

        while (m.find()) {
            simpleFound = true;
            Optional<FilterFields> opt = parse(m);
            if (opt.isEmpty()) {                     // malformed → leave untouched
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                continue;
            }

            FilterFields p = opt.get();

            /* resolve column + SQL operator */
            String column = mapVariable(p.variableName());
            String sqlOp  = mapOperator(p.operator());

            if (column == null || sqlOp == null) {
                // unsupported -> leave untouched
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                continue;
            }

            /* build / register CTE */
            String cteKey = String.format(
                    "filtercte_%s_%s_%s_%s",
                    column,
                    safeOp(p.operator()),
                    safeLiteral(p.literal()),
                    pi.getUid());

            if (!ctx.containsCte(cteKey)) {
                ctx.addFilterCte(cteKey,
                        buildFilterCteSql(pi, column, p.literal(), sqlOp, start, end, qb));
            }

            String alias = ctx.getDefinitionByKey(cteKey).getAlias();
            aliasMap.put(m.group(0), alias);      // placeholder-to-alias

            /* remove the simple clause from the filter string */
            m.appendReplacement(out, "");
        }
        m.appendTail(out);

        /* clean dangling AND/OR */
        String cleaned = clean(out.toString());

        if (!simpleFound) {
            // nothing recognised – return intact
            return rawSql;
        }
        return cleaned;
    }

    private Optional<FilterFields> parse(Matcher m) {
        String raw = m.group(0);
        return PlaceholderParser.parseFilter(raw);
    }

    private static String buildFilterCteSql(ProgramIndicator pi,
                                            String column,
                                            String literal,
                                            String sqlOp,
                                            Date start,
                                            Date end,
                                            SqlBuilder qb) {

        String table = "analytics_event_" + pi.getProgram().getUid();
        String quotedCol = qb.quote(column);

        String boundaries = BoundarySqlBuilder.buildSql(
                pi.getAnalyticsPeriodBoundaries(),
                "occurreddate",
                pi, start, end, qb);

        return String.format("""
                select enrollment
                from (
                    select enrollment, %1$s,
                           row_number() over (partition by enrollment order by occurreddate desc) as rn
                    from %2$s
                    where %1$s is not null %3$s
                ) latest
                where rn = 1 and %1$s %4$s %5$s""",
                quotedCol, table, boundaries, sqlOp, qb.singleQuote(literal));
    }

    /* maps V{variableName} → event analytics column */
    private static String mapVariable(String name) {
        return switch (name) {
            case "event_status"    -> "eventstatus";
            case "scheduled_date",
                 "due_date"        -> "scheduleddate";
            case "creation_date"   -> "created";
            case "event_date"      -> "occurreddate";
            default -> null;
        };
    }

    private static String mapOperator(String sym) {
        return switch (sym) {
            case "==" -> "=";
            case "!=" -> "!=";
            case ">"  -> ">";
            case "<"  -> "<";
            case ">=" -> ">=";
            case "<=" -> "<=";
            default   -> null;
        };
    }

    private static String safeOp(String op) {
        return op.replace("=", "eq")
                .replace("!", "ne")
                .replace("<", "lt")
                .replace(">", "gt");
    }

    private static String safeLiteral(String lit) {
        String s = INVALID_CHARS.matcher(lit).replaceAll("_").toLowerCase();
        return s.substring(0, Math.min(20, s.length()));
    }

    /**
     * Cleans a string by trimming whitespace and removing leading or trailing "AND" or "OR" logical
     * operators (case-sensitive). Also removes the operator if it is the only content of the string
     * after trimming.
     *
     * @param str The filter string segment to clean. Must not be null.
     * @return The cleaned string, potentially empty if only operators/whitespace were present.
     * @throws NullPointerException if {@code remaining} is null.
     */
    private static String clean(String str) {
        String cleaned = str.trim();
        cleaned = LONE_OP.matcher(cleaned).replaceAll("");
        cleaned = LEADING_OP.matcher(cleaned).replaceAll("");
        cleaned = TRAILING_OP.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }
}
