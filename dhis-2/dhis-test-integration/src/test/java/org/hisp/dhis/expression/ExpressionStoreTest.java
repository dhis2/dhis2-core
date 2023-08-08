/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Lars Helge Overland
 */
class ExpressionStoreTest extends SingleSetupIntegrationTestBase {

  @Autowired
  @Qualifier("org.hisp.dhis.expression.ExpressionStore")
  private GenericStore<Expression> expressionStore;

  @Autowired private DataElementService dataElementService;

  private long dataElementIdA;

  private long dataElementIdB;

  private long dataElementIdC;

  private long dataElementIdD;

  private String expressionA;

  private String expressionB;

  private String descriptionA;

  private String descriptionB;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------
  @Override
  public void setUpTest() throws Exception {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    DataElement dataElementD = createDataElement('D');
    dataElementIdA = dataElementService.addDataElement(dataElementA);
    dataElementIdB = dataElementService.addDataElement(dataElementB);
    dataElementIdC = dataElementService.addDataElement(dataElementC);
    dataElementIdD = dataElementService.addDataElement(dataElementD);
    expressionA = "[" + dataElementIdA + "] + [" + dataElementIdB + "]";
    expressionB = "[" + dataElementIdC + "] - [" + dataElementIdD + "]";
    descriptionA = "Expression A";
    descriptionB = "Expression B";
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testAddGetExpression() {
    Expression expr = new Expression(expressionA, descriptionA);
    expressionStore.save(expr);
    long id = expr.getId();
    expr = expressionStore.get(id);
    assertEquals(expr.getExpression(), expressionA);
    assertEquals(expr.getDescription(), descriptionA);
  }

  @Test
  void testUpdateExpression() {
    Expression expr = new Expression(expressionA, descriptionA);
    expressionStore.save(expr);
    long id = expr.getId();
    expr = expressionStore.get(id);
    assertEquals(expr.getExpression(), expressionA);
    assertEquals(expr.getDescription(), descriptionA);
    expr.setExpression(expressionB);
    expr.setDescription(descriptionB);
    expressionStore.update(expr);
    expr = expressionStore.get(id);
    assertEquals(expr.getExpression(), expressionB);
    assertEquals(expr.getDescription(), descriptionB);
  }

  @Test
  void testDeleteExpression() {
    Expression exprA = new Expression(expressionA, descriptionA);
    Expression exprB = new Expression(expressionB, descriptionB);
    expressionStore.save(exprA);
    long idA = exprA.getId();
    expressionStore.save(exprB);
    long idB = exprB.getId();
    assertNotNull(expressionStore.get(idA));
    assertNotNull(expressionStore.get(idB));
    expressionStore.delete(exprA);
    assertNull(expressionStore.get(idA));
    assertNotNull(expressionStore.get(idB));
    expressionStore.delete(exprB);
    assertNull(expressionStore.get(idA));
    assertNull(expressionStore.get(idB));
  }

  @Test
  void testGetAllExpressions() {
    Expression exprA = new Expression(expressionA, descriptionA);
    Expression exprB = new Expression(expressionB, descriptionB);
    expressionStore.save(exprA);
    expressionStore.save(exprB);
    List<Expression> expressions = expressionStore.getAll();
    assertTrue(expressions.size() == 2);
    assertTrue(expressions.contains(exprA));
    assertTrue(expressions.contains(exprB));
  }
}
