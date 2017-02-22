package org.hisp.dhis.validation;
/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.jdbc.batchhandler.ValidationResultBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stian Sandvold
 */
public class DefaultValidationResultService
    implements ValidationResultService
{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Autowired
    private DataElementCategoryService categoryService;

    @Override
    @Transactional
    public void saveValidationResults( Collection<ValidationResult> validationResults )
    {
        BatchHandler<ValidationResult> validationResultBatchHandler = batchHandlerFactory
            .createBatchHandler( ValidationResultBatchHandler.class ).init();

        validationResults.forEach( validationResult ->
        {
            if ( !validationResultBatchHandler.objectExists( validationResult ) )
            {
                validationResultBatchHandler.addObject( validationResult );
            }
        } );

        validationResultBatchHandler.flush();
    }

    @Override
    public Map<String, ValidationResult> validationResultExists( ValidationRule validationRule,
        Period period,
        OrganisationUnit organisationUnit )
    {
        Map<String, ValidationResult> result = new HashMap<>();

        String sql = "" +
            "SELECT attributeoptioncomboid, leftsidevalue, rightsidevalue, dayinperiod " +
            "FROM validationresult R " +
            "WHERE R.validationresultid IN ( " +
            "SELECT validationresultid " +
            "FROM validationresult R2 " +
            "WHERE R2.validationruleid = " + validationRule.getId() +
            "AND R2.periodid = " + period.getId() +
            "AND R2.organisationunitid = " + organisationUnit.getId() +
            "AND R2.attributeoptioncomboid = R.attributeoptioncomboid " +
            "ORDER BY R2.dayinperiod DESC " +
            "LIMIT 1" +
            ")";

        jdbcTemplate.queryForList( sql ).forEach( row ->
        {
            ValidationResult validationResult = new ValidationResult();
            validationResult.setLeftsideValue( (double) row.get( "leftsidevalue" ) );
            validationResult.setRightsideValue( (double) row.get( "rightsidevalue" ) );
            validationResult.setDayInPeriod( (int) row.get( "dayinperiod" ) );

            result.put(
                categoryService.getDataElementCategoryOptionCombo( (int) row.get( "attributeoptioncomboid" ) ).getUid(),
                validationResult
            );
        } );

        return result;
    }
}
