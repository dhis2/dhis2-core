/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.resourcetable.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.hisp.dhis.test.TestBase;

class BaseResourceTableTest extends TestBase {

  public record ExpessionAlias(String alias, String expression) {}

  /**
   * Verifies that the select statement has the expected number of columns and that the column
   * aliases are in the expected order.
   *
   * @param selectItems the select items from the select statement using during the insert
   * @param expectedColumnAliases the expected column aliases and expressions
   */
  void verifyPopulateStatement(
      List<SelectItem> selectItems, List<ExpessionAlias> expectedColumnAliases) {

    assertEquals(
        expectedColumnAliases.size(),
        selectItems.size(),
        "Select statement should have " + expectedColumnAliases.size() + " columns");

    // 6. Validate column aliases in order
    for (int i = 0; i < expectedColumnAliases.size(); i++) {
      SelectExpressionItem item = (SelectExpressionItem) selectItems.get(i);
      String actualAlias = item.getAlias().getName();

      // Remove quotes if present
      actualAlias = actualAlias.replace("\"", "");

      assertEquals(
          expectedColumnAliases.get(i).alias(),
          actualAlias,
          "Column at position "
              + (i + 1)
              + " should have alias '"
              + expectedColumnAliases.get(i).alias()
              + "'");
      assertEquals(
          expectedColumnAliases.get(i).expression(),
          item.getExpression().toString().toLowerCase(),
          "Column at position "
              + (i + 1)
              + " should have expression  '"
              + expectedColumnAliases.get(i).expression()
              + "'");
    }
  }
}
