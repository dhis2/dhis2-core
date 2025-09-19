package org.hisp.dhis.analytics.util.sql;

import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

import java.util.Optional;

@UtilityClass
public class ColumnAliasUtils {

    /**
     * Returns true if the given column reference is qualified with a table alias/name,
     * e.g. "t.col", "t.\"column_name\"", "\"T\".\"COLUMN_NAME\"".
     * Notes:
     * - Works with quoted identifiers.
     * - Treats any qualifier (alias or actual table name) as an "alias".
     * - If the input isn't a plain column reference (e.g., it's an expression), returns false.
     */
    public static boolean isQualifiedWithAlias(String columnRef) {
        Column col = parseColumnOrNull(columnRef);
        if (col == null) return false;

        Table table = col.getTable();
        // getFullyQualifiedName() includes schema if present; presence means it’s qualified.
        return table != null && notBlank(table.getFullyQualifiedName());
    }

    /**
     * Extracts the alias/qualifier (the part before the dot) or returns null if unqualified.
     * For "schema.table.col" the qualifier will be "schema.table".
     */
    public static String extractQualifier(String columnRef) {
        Column col = parseColumnOrNull(columnRef);
        if (col == null) return null;

        Table table = col.getTable();
        String q = table != null ? table.getFullyQualifiedName() : null;
        return notBlank(q) ? q : null;
    }

    /**
     * Returns just the column name without any qualifier.
     * If the input isn't a simple column reference, returns the original string.
     */
    public static String unqualify(String columnRef) {
        if (columnRef == null) return null;

        // If it's clearly not a plain column reference, return as-is.
        // - preserve leading/trailing whitespace exactly
        // - preserve wildcards like "*", "t.*"
        // - preserve trailing dot like "t."
        if (hasOuterWhitespace(columnRef) || columnRef.contains("*") || columnRef.trim().endsWith(".")) {
            return columnRef;
        }

        Column col = parseColumnOrNull(columnRef);
        // If it's a plain column, return the column part exactly as parsed (quotes preserved).
        return (col != null) ? col.getColumnName() : columnRef;
    }

    public static Optional<QualifiedRef> splitQualified(String columnRef) {
        Column col = parseColumnOrNull(columnRef);
        if (col == null) return Optional.empty();

        Table table = col.getTable();
        String q = (table != null) ? table.getFullyQualifiedName() : null;
        if (q == null || q.isBlank()) return Optional.empty();

        // Preserve quotes in both qualifier and column name
        return Optional.of(new QualifiedRef(q, col.getColumnName()));
    }

    private static boolean hasOuterWhitespace(String s) {
        // true if there is at least one leading or trailing whitespace char
        return s.length() != s.trim().length();
    }

    private static Column parseColumnOrNull(String text) {
        if (!notBlank(text)) return null;
        try {
            // Parse as an expression and check it's a Column
            Expression expr = CCJSqlParserUtil.parseExpression(text);
            if (expr instanceof Column) {
                return (Column) expr;
            }
            // Not a plain column reference (could be a function, arithmetic, etc.)
            return null;
        } catch (JSQLParserException e) {
            // Not parseable as an expression — treat as not-a-column
            return null;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public record QualifiedRef(String qualifier, String columnName) { }
}
