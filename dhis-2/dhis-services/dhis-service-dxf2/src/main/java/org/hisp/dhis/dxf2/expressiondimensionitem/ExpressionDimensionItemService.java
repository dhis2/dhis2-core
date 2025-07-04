/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.expressiondimensionitem;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.expression.ExpressionValidationOutcome.VALID;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.springframework.stereotype.Service;

/**
 * Parsing the expression of ExpressionDimensionItem, provides collection of
 * BaseDimensionalItemObjects.
 */
@Service
@RequiredArgsConstructor
public class ExpressionDimensionItemService {

  /** Valid patterns: fbfJHSPpUQD, fbfJHSPpUQD.pq2XI5kz2BY, fbfJHSPpUQD.pq2XI5kz2BY.pq2XI5kz2BZ */
  public static final Pattern EXPRESSION_PATTERN =
      compile("[a-zA-Z0-9]{11}[.]?[a-zA-Z0-9]{0,11}[.]?[a-zA-Z0-9]{0,11}");

  private final IdentifiableObjectManager manager;

  private final ExpressionService expressionService;

  /**
   * Returns a list of {@link BaseDimensionalItemObject} based on expressions defined in the given
   * {@link DataDimensionItem}.
   *
   * @param dataDimensionItem {@link IdentifiableObjectManager} expression dimension item
   * @return list of {@link BaseDimensionalItemObject}
   */
  public List<DimensionalItemObject> getExpressionItems(DataDimensionItem dataDimensionItem) {
    if (dataDimensionItem.getExpressionDimensionItem() == null) {
      return new ArrayList<>();
    }

    String expression = dataDimensionItem.getExpressionDimensionItem().getExpression();

    List<String> expressionTokens = getExpressionTokens(EXPRESSION_PATTERN, expression);

    List<DimensionalItemObject> baseDimensionalItemObjects = new ArrayList<>();

    expressionTokens.forEach(
        et -> {
          String[] uids = et.split(Pattern.quote("."));
          if (uids.length > 1) {
            DataElementOperand deo =
                new DataElementOperand(
                    manager.get(DataElement.class, uids[0]),
                    manager.get(CategoryOptionCombo.class, uids[1]));
            baseDimensionalItemObjects.add(deo);
          } else if (uids.length > 0) {
            baseDimensionalItemObjects.add(manager.get(DataElement.class, uids[0]));
          }
        });

    return baseDimensionalItemObjects;
  }

  /**
   * Provides expression validation.
   *
   * @param expression or indicator of expression dimension item
   * @return true when the expression is valid
   */
  public boolean isValidExpressionItems(String expression) {
    if (trimToNull(expression) == null || trimToEmpty(expression).equalsIgnoreCase("null")) {
      return false;
    }

    ExpressionValidationOutcome result =
        expressionService.expressionIsValid(expression, INDICATOR_EXPRESSION);

    return result == VALID;
  }

  /**
   * Expression parser for expression tokens of indicator (data_element.category_option_combo or
   * data_element only).
   *
   * @param pattern compiled {@link Pattern} object of regular expression
   * @param expression expression of indicator
   * @return collection of tokens
   */
  public List<String> getExpressionTokens(Pattern pattern, String expression) {
    List<String> expressionTokens = new ArrayList<>();

    pattern
        .matcher(expression)
        .results()
        .map(mr -> mr.group(0))
        .filter(mr -> !isNumeric(mr))
        .forEach(expressionTokens::add);

    return expressionTokens;
  }
}
