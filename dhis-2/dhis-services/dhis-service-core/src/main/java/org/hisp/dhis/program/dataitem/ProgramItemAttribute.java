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
package org.hisp.dhis.program.dataitem;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * Program indicator expression data item ProgramAttribute
 *
 * @author Jim Grace
 */
public class ProgramItemAttribute extends ProgramExpressionItem {
  @Override
  public Object getDescription(ExprContext ctx, CommonExpressionVisitor visitor) {
    String attributeId = getProgramAttributeId(ctx);

    TrackedEntityAttribute attribute =
        visitor.getIdObjectManager().get(TrackedEntityAttribute.class, attributeId);

    if (attribute == null) {
      throw new ParserExceptionWithoutContext(
          "Tracked entity attribute " + attributeId + " not found.");
    }

    visitor.getItemDescriptions().put(ctx.getText(), attribute.getDisplayName());

    return getNullReplacementValue(attribute.getValueType());
  }

  @Override
  public Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    String attributeId = getProgramAttributeId(ctx);

    String column = visitor.getStatementBuilder().columnQuote(attributeId);

    if (visitor.getState().isReplaceNulls()) {
      TrackedEntityAttribute attribute =
          visitor.getIdObjectManager().get(TrackedEntityAttribute.class, attributeId);

      if (attribute == null) {
        throw new ParserExceptionWithoutContext(
            "Tracked entity attribute " + attributeId + " not found during SQL generation.");
      }

      column = replaceNullSqlValues(column, visitor, attribute.getValueType());
    }

    return column;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Makes sure that the parsed A{...} has a syntax that could be used be used in an program
   * expression for A{attributeUid}
   *
   * @param ctx the item context
   * @return the attribute UID.
   */
  private String getProgramAttributeId(ExprContext ctx) {
    if (ctx.uid1 != null) {
      throw new org.hisp.dhis.antlr.ParserExceptionWithoutContext(
          "Program attribute must have one UID: " + ctx.getText());
    }

    return ctx.uid0.getText();
  }
}
