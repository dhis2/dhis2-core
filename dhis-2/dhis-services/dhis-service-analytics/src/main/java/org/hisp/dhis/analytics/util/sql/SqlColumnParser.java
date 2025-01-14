package org.hisp.dhis.analytics.util.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;

public class SqlColumnParser {

    /**
     * Removes table alias from a SQL column reference using JSqlParser.
     * Handles quoted column names and complex SQL expressions.
     *
     * @param columnReference The SQL column reference (e.g., "ax.uidlevel2", "test1.`alfa`")
     * @return The column name without the table alias (e.g., "uidlevel2", "alfa")
     */
    public static String removeTableAlias(String columnReference) {
        if (columnReference == null || columnReference.isEmpty()) {
            return columnReference;
        }

        try {
            // Parse the column reference using JSqlParser
            Expression expression = CCJSqlParserUtil.parseExpression(columnReference);

            // Ensure the parsed expression is a Column
            if (!(expression instanceof Column column)) {
                throw new IllegalArgumentException("Input is not a valid SQL column reference: " + columnReference);
            }

            // Extract the column name
            return unquote(column.getColumnName());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing SQL: " + e.getMessage(), e);
        }
    }

    // FIXME - this method is duplicated in SqlWhereClauseExtractor
    private static String unquote(String quoted) {
        // Handle null or empty
        if (quoted == null || quoted.isEmpty()) {
            return "";
        }

        // Check minimum length (needs at least 2 chars for quotes)
        if (quoted.length() < 2) {
            return quoted;
        }

        char firstChar = quoted.charAt(0);
        char lastChar = quoted.charAt(quoted.length() - 1);

        // Check if quotes match
        if ((firstChar == '"' && lastChar == '"') ||
                (firstChar == '`' && lastChar == '`')) {
            return quoted.substring(1, quoted.length() - 1);
        }

        return quoted;
    }
}
