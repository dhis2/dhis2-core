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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.hisp.dhis.dxf2.expressiondimensionitem.ExpressionDimensionItemService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExpressionDimensionItemObjectBundleHook
    extends AbstractObjectBundleHook<ExpressionDimensionItem> {
  private final ExpressionDimensionItemService expressionDimensionItemService;

  private final AclService aclService;

  @Override
  public void validate(
      ExpressionDimensionItem expressionDimensionItem,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {
    String expression = expressionDimensionItem.getExpression();

    if (!expressionDimensionItemService.isValidExpressionItems(expression)) {
      addReports.accept(
          new ErrorReport(
              ExpressionDimensionItem.class,
              ErrorCode.E7137,
              expression,
              "Not a valid expression"));
    }

    validateSecurity(expressionDimensionItem, bundle, addReports);
  }

  private void validateSecurity(
      ExpressionDimensionItem expressionDimensionItem,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {
    PreheatIdentifier identifier = bundle.getPreheatIdentifier();

    if (!aclService.canRead(bundle.getUser(), expressionDimensionItem)) {
      addReports.accept(
          new ErrorReport(
              ExpressionDimensionItem.class,
              ErrorCode.E3012,
              identifier.getIdentifiersWithName(bundle.getUser()),
              identifier.getIdentifiersWithName(expressionDimensionItem)));
    }
  }
}
