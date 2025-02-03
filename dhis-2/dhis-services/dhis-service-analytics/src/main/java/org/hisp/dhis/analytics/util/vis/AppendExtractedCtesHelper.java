package org.hisp.dhis.analytics.util.vis;

import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class AppendExtractedCtesHelper {

    /**
     * Appends the list of CTE definitions (given as GeneratedCte records) to the main query's WITH clause,
     * placing the new CTEs before the existing ones.
     *
     * @param select the main query (as a Select AST node)
     * @param generatedCtes the collection of new CTE definitions.
     */
    public static void appendExtractedCtes(Select select, Map<String, GeneratedCte> generatedCtes) {
        List<WithItem> existingWithItems = select.getWithItemsList();
        if (existingWithItems == null) {
            existingWithItems = new ArrayList<>();
            select.setWithItemsList(existingWithItems);
        }
        List<WithItem> newCtes = new ArrayList<>();
        for (GeneratedCte genCte : generatedCtes.values()) {
            try {
                // select 1 is a dummy query to wrap the CTE definition
                String wrappedCte = "with " + genCte.name() + " as (" + genCte.cteString() + ") select 1";
                Statement stmt = CCJSqlParserUtil.parse(wrappedCte);
                if (stmt instanceof Select tempSelect) {
                    List<WithItem> tempWithItems = tempSelect.getWithItemsList();
                    if (tempWithItems != null && !tempWithItems.isEmpty()) {
                        WithItem newCte = tempWithItems.get(0);
                        newCtes.add(newCte);
                    }
                }
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E7149, e.getMessage(), e);
            }
        }
        // Prepend the new CTEs.
        existingWithItems.addAll(0, newCtes);
    }
}