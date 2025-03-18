/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.validation;

import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping("/api/validationRules")
public class ValidationRuleController
    extends AbstractCrudController<
        ValidationRule, ValidationRuleController.GetValidationRuleObjectListParams> {

  @Autowired private DataSetService dataSetService;
  @Autowired private ValidationRuleService validationRuleService;
  @Autowired private ExpressionService expressionService;
  @Autowired private I18nManager i18nManager;

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class GetValidationRuleObjectListParams extends GetObjectListParams {
    @OpenApi.Description(
        """
      Limits results to validation rules that are connected to the given dataset.
      Can be combined with further `filter`s.
      """)
    @OpenApi.Property({UID.class, DataSet.class})
    String dataSet;
  }

  @Override
  protected List<UID> getPreQueryMatches(GetValidationRuleObjectListParams params) {
    String dsId = params.getDataSet();
    if (dsId == null) return null;
    DataSet ds = dataSetService.getDataSet(dsId);
    List<ValidationRule> res =
        ds == null
            ? List.of()
            : new ArrayList<>(validationRuleService.getValidationRulesForDataSet(ds));
    return res.stream().map(UID::of).toList();
  }

  @PostMapping(value = "/expression/description", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage getExpressionDescription(@RequestBody String expression) {
    ExpressionValidationOutcome result =
        expressionService.expressionIsValid(expression, VALIDATION_RULE_EXPRESSION);

    return new DescriptiveWebMessage(result.isValid() ? Status.OK : Status.ERROR, HttpStatus.OK)
        .setDescription(
            result::isValid,
            () ->
                expressionService.getExpressionDescription(expression, VALIDATION_RULE_EXPRESSION))
        .setMessage(i18nManager.getI18n().getString(result.getKey()));
  }
}
