/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.analytics.util.optimizer.cte.CteGeneratorFactory;
import org.hisp.dhis.analytics.util.optimizer.cte.CteInput;
import org.hisp.dhis.analytics.util.optimizer.cte.ExpressionTransformer;
import org.hisp.dhis.analytics.util.optimizer.cte.data.DecomposedCtes;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

/**
 * The `CteDecomposer` class is responsible for identifying and extracting correlated subqueries
 * within the WHERE clause of a specific type of CTE (Common Table Expression) and preparing the
 * necessary data for generating new, optimized CTEs to replace them. This class operates on CTEs
 * whose names start with "pi_" (Program Indicator CTEs).
 *
 * <p>This class processes a list of `WithItem` objects (representing CTEs), extracts correlated
 * subqueries from their WHERE clauses, and generates `GeneratedCte` objects that contain the
 * information needed to create the replacement CTEs. The actual replacement of the subqueries with
 * joins to the new CTEs is handled by a separate `CteSqlRebuilder` class.
 */
@Slf4j
public class CteDecomposer implements SqlOptimizationStep {

  private final CteGeneratorFactory factory = new CteGeneratorFactory();

  /**
   * Processes a list of `WithItem` objects (CTEs), extracting and transforming correlated
   * subqueries found within their WHERE clauses and SELECT items. It generates `GeneratedCte`
   * objects for each extracted subquery, which will be used later to construct the optimized SQL.
   *
   * @param withItems A list of `WithItem` objects representing the CTEs to process. Only CTEs with
   *     names starting with "pi_" and containing correlated subqueries in their WHERE clause or
   *     SELECT items will be processed.
   * @return A {@link DecomposedCtes} object containing the list of {@link GeneratedCte} and the
   *     transformed WHERE clause expression. Returns an empty {@link DecomposedCtes} object if no
   *     subqueries are found or if the input is empty or invalid.
   */
  public DecomposedCtes processCTE(List<WithItem> withItems) {
    Map<String, GeneratedCte> generatedCtes = new LinkedHashMap<>();

    // TODO only one cte is processed for now
    if (withItems.isEmpty()) return DecomposedCtes.empty();
    WithItem withItem = withItems.get(0);

    SelectBody body = withItem.getSubSelect().getSelectBody();
    // Extract the SELECT body from the PI CTE
    if (!(body instanceof PlainSelect oldSelect)) return DecomposedCtes.empty();

    // Process WHERE clause
    Expression whereExpr = oldSelect.getWhere();
    if (whereExpr != null) {
      // Create and apply the transformer for WHERE clause
      ExpressionTransformer whereTransformer = new ExpressionTransformer();
      whereExpr.accept(whereTransformer);

      // Get the transformed expression and extracted subselects
      Expression transformedWhere = whereTransformer.getTransformedExpression();
      if (transformedWhere == null) {
        // If transformation failed, keep original
        transformedWhere = whereExpr;
      }

      // Process the subselects extracted from WHERE clause
      Map<SubSelect, FoundSubSelect> extractedSubSelects =
          whereTransformer.getExtractedSubSelects();
      processExtractedSubSelects(extractedSubSelects, generatedCtes);

      // Update the WHERE clause in the original select
      oldSelect.setWhere(transformedWhere);
    }

    // Process SELECT items
    List<SelectItem> selectItems = oldSelect.getSelectItems();
    if (selectItems != null && !selectItems.isEmpty()) {
      List<SelectItem> transformedSelectItems = processSelectItems(selectItems, generatedCtes);
      oldSelect.setSelectItems(transformedSelectItems);
    }

    return new DecomposedCtes(
        List.copyOf(generatedCtes.values()),
        whereExpr != null ? oldSelect.getWhere() : null,
        oldSelect);
  }

  /**
   * Processes SELECT items, looking for and transforming subqueries within them.
   *
   * @param selectItems The SELECT items to process
   * @param generatedCtes The map to store generated CTEs
   * @return A list of transformed SELECT items
   */
  private List<SelectItem> processSelectItems(
      List<SelectItem> selectItems, Map<String, GeneratedCte> generatedCtes) {
    List<SelectItem> transformedItems = new ArrayList<>();

    for (SelectItem item : selectItems) {
      if (item instanceof SelectExpressionItem sei) {
        Expression expr = sei.getExpression();

        // Create and apply the transformer for the expression
        ExpressionTransformer expressionTransformer = new ExpressionTransformer();
        expr.accept(expressionTransformer);

        // Get the transformed expression
        Expression transformedExpr = expressionTransformer.getTransformedExpression();
        if (transformedExpr == null) {
          transformedExpr = expr; // Keep original if transformation failed
        }

        // Process the subselects extracted from this expression
        Map<SubSelect, FoundSubSelect> extractedSubSelects =
            expressionTransformer.getExtractedSubSelects();
        processExtractedSubSelects(extractedSubSelects, generatedCtes);

        // Create a new SelectExpressionItem with the transformed expression
        SelectExpressionItem newSei = new SelectExpressionItem(transformedExpr);
        if (sei.getAlias() != null) {
          newSei.setAlias(sei.getAlias());
        }

        transformedItems.add(newSei);
      } else {
        // For non-expression items (like AllColumns), add as-is
        transformedItems.add(item);
      }
    }

    return transformedItems;
  }

  /**
   * Processes extracted subselects and generates CTEs for them.
   *
   * @param extractedSubSelects The extracted subselects
   * @param generatedCtes The map to store generated CTEs
   */
  private void processExtractedSubSelects(
      Map<SubSelect, FoundSubSelect> extractedSubSelects, Map<String, GeneratedCte> generatedCtes) {

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
