/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.expressiondimensionitem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;

/**
 * Parsing the expression of ExpressionDimensionItem, provides collection of
 * BaseDimensionalItemObjects.
 */
public class ExpressionDimensionItemHelper {
  public static final Pattern pattern =
      Pattern.compile("[a-zA-Z0-9]{11}[.]?[a-zA-Z0-9]{0,11}[.]?[a-zA-Z0-9]{0,11}");

  private ExpressionDimensionItemHelper() {
    throw new UnsupportedOperationException("helper");
  }

  /**
   * Provides collection of selected item types inside the expression
   *
   * @param manager {@link IdentifiableObjectManager} service for item delivery
   * @param dataDimensionItem {@link IdentifiableObjectManager} expression dimension item
   * @return collection of selected item types
   */
  public static List<BaseDimensionalItemObject> getExpressionItems(
      IdentifiableObjectManager manager, DataDimensionItem dataDimensionItem) {
    if (dataDimensionItem.getExpressionDimensionItem() == null) {
      return new ArrayList<>();
    }

    String expression = dataDimensionItem.getExpressionDimensionItem().getExpression();

    List<String> expressionTokens = getExpressionTokens(pattern, expression);

    List<BaseDimensionalItemObject> baseDimensionalItemObjects = new ArrayList<>();

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
   * Expression parser for expression tokens of indicator ( data_element.category_option_combo or
   * data_element only )
   *
   * @param pattern compiled Patter object of regular expression
   * @param expression expression of indicator
   * @return collection of tokens
   */
  public static List<String> getExpressionTokens(Pattern pattern, String expression) {
    List<String> expressionTokens = new ArrayList<>();

    pattern.matcher(expression).results().map(mr -> mr.group(0)).forEach(expressionTokens::add);

    return expressionTokens;
  }
}
