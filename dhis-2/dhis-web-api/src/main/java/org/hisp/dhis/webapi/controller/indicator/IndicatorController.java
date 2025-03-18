/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.indicator;

import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.security.Authorities.F_INDICATOR_MERGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.resolver.ExpressionResolver;
import org.hisp.dhis.analytics.resolver.ExpressionResolverCollection;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/indicators")
public class IndicatorController extends AbstractCrudController<Indicator, GetObjectListParams> {
  private final ExpressionService expressionService;

  private final ExpressionResolverCollection resolvers;

  private final I18nManager i18nManager;

  private final MergeService indicatorMergeService;

  @PostMapping(value = "/expression/description", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage getExpressionDescription(@RequestBody String expression) {
    String resolvingExpression = expression;

    for (ExpressionResolver resolver : resolvers.getExpressionResolvers()) {
      resolvingExpression = resolver.resolve(resolvingExpression);
    }

    String resolvedExpression = resolvingExpression;
    ExpressionValidationOutcome result =
        expressionService.expressionIsValid(resolvedExpression, INDICATOR_EXPRESSION);

    return new DescriptiveWebMessage(result.isValid() ? Status.OK : Status.ERROR, HttpStatus.OK)
        .setDescription(
            result::isValid,
            () ->
                expressionService.getExpressionDescription(
                    resolvedExpression, INDICATOR_EXPRESSION))
        .setMessage(i18nManager.getI18n().getString(result.getKey()));
  }

  @ResponseStatus(HttpStatus.OK)
  @RequiresAuthority(anyOf = F_INDICATOR_MERGE)
  @PostMapping(value = "/merge", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage mergeIndicators(@RequestBody MergeParams params)
      throws ConflictException {
    log.info("Indicator merge received");

    MergeReport report = indicatorMergeService.processMerge(params);

    log.info("Indicator merge processed with report: {}", report);
    return WebMessageUtils.mergeReport(report);
  }
}
