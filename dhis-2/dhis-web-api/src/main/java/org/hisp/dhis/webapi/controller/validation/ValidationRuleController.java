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
package org.hisp.dhis.webapi.controller.validation;

import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.ValidationRuleSchemaDescriptor;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = ValidationRuleSchemaDescriptor.API_ENDPOINT )
public class ValidationRuleController
    extends AbstractCrudController<ValidationRule>
{
    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private I18nManager i18nManager;

    @Override
    protected List<ValidationRule> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders )
        throws QueryParserException
    {
        if ( options.contains( "dataSet" ) )
        {
            DataSet ds = dataSetService.getDataSet( options.get( "dataSet" ) );

            if ( ds == null )
            {
                return null;
            }

            return Lists.newArrayList( validationRuleService.getValidationRulesForDataSet( ds ) );
        }

        return super.getEntityList( metadata, options, filters, orders );
    }

    @PostMapping( value = "/expression/description", produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage getExpressionDescription( @RequestBody String expression )
    {
        ExpressionValidationOutcome result = expressionService.expressionIsValid( expression,
            VALIDATION_RULE_EXPRESSION );

        return new DescriptiveWebMessage( result.isValid() ? Status.OK : Status.ERROR, HttpStatus.OK )
            .setDescription( result::isValid,
                () -> expressionService.getExpressionDescription( expression, VALIDATION_RULE_EXPRESSION ) )
            .setMessage( i18nManager.getI18n().getString( result.getKey() ) );
    }
}
