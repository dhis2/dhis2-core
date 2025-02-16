package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.analytics.util.optimizer.cte.CteGeneratorFactory;
import org.hisp.dhis.analytics.util.optimizer.cte.CteInput;
import org.hisp.dhis.analytics.util.optimizer.cte.ExpressionTransformer;
import org.hisp.dhis.analytics.util.optimizer.cte.data.DecomposedCtes;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The `CteDecomposer` class is responsible for identifying and extracting correlated
 * subqueries within the WHERE clause of a specific type of CTE (Common Table Expression)
 * and preparing the necessary data for generating new, optimized CTEs to replace them.
 * This class operates on CTEs whose names start with "pi_" (Program Indicator CTEs).
 * <p>
 * This class processes a list of `WithItem` objects (representing CTEs), extracts
 * correlated subqueries from their WHERE clauses, and generates `GeneratedCte` objects
 * that contain the information needed to create the replacement CTEs. The actual
 * replacement of the subqueries with joins to the new CTEs is handled by a separate
 * `CteSqlRebuilder` class.
 */
@Slf4j
public class CteDecomposer implements SqlOptimizationStep {

    private final CteGeneratorFactory factory = new CteGeneratorFactory();

    /**
     * Processes a list of `WithItem` objects (CTEs), extracting and transforming
     * correlated subqueries found within their WHERE clauses.  It generates
     * `GeneratedCte` objects for each extracted subquery, which will be used
     * later to construct the optimized SQL.
     * <p>
     *
     * @param withItems A list of `WithItem` objects representing the CTEs to process.
     *                  Only CTEs with names starting with "pi_" and containing correlated
     *                  subqueries in their WHERE clause will be processed.
     * @return A {@link DecomposedCtes} object containing the list of {@link GeneratedCte} and the transformed
     *         WHERE clause expression. Returns an empty {@link DecomposedCtes} object if no
     *         subqueries are found or if the input is empty or invalid.
     */
    public DecomposedCtes processCTE(List<WithItem> withItems) {
        Map<String, GeneratedCte> generatedCtes = new LinkedHashMap<>();

        // TODO only one cte is processed for now
        if (withItems.isEmpty()) return DecomposedCtes.empty();
        WithItem withItem = withItems.get(0);

        SelectBody body = withItem.getSubSelect().getSelectBody();
        // Extract the SELECT body from the PI CTE
        if (!(body instanceof PlainSelect oldSelect)) return DecomposedCtes.empty();

        // Get the WHERE expression
        Expression whereExpr = oldSelect.getWhere();
        if (whereExpr == null) return DecomposedCtes.empty();

        // Create and apply the transformer
        ExpressionTransformer transformer = new ExpressionTransformer();
        whereExpr.accept(transformer);

        // Get the transformed expression and extracted subselects
        Expression transformedWhere = transformer.getTransformedExpression();
        if (transformedWhere == null) {
            // If transformation failed, keep original
            transformedWhere = whereExpr;
        }

        Map<SubSelect, FoundSubSelect> extractedSubSelects = transformer.getExtractedSubSelects();
        if (!extractedSubSelects.isEmpty()) {
            for (Map.Entry<SubSelect, FoundSubSelect> entry : extractedSubSelects.entrySet()) {
                FoundSubSelect found = entry.getValue();
                String cteName = found.name();

                if (!generatedCtes.containsKey(cteName)) {
                    String eventTable = extractTableName(entry.getKey());
                    if (eventTable != null) {
                        CteInput input = new CteInput(entry.getKey(), found, eventTable);
                        Function<CteInput, GeneratedCte> generator = factory.getGenerator(cteName);
                        GeneratedCte cte = generator.apply(input);
                        log.debug("Adding Generated CTE: {}", cte.name());
                        generatedCtes.put(cteName, cte);
                    }
                }
            }
        }
        oldSelect.setWhere(transformedWhere);
        return new DecomposedCtes(List.copyOf(generatedCtes.values()), transformedWhere, oldSelect);
    }

    /**
     * Extracts the table name from the subselect's FROM clause.
     *
     * @param subSelect the subselect.
     * @return the table name as a String.
     */
    private String extractTableName(SubSelect subSelect) {
        if (subSelect.getSelectBody() instanceof PlainSelect plain) {
            FromItem fromItem = plain.getFromItem();
            if (fromItem instanceof Table table) {
                return table.getName();
            }
        }
        return null;
    }
}
