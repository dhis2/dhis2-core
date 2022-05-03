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
package org.hisp.dhis.validation;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Date;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * ValidationService tests that need to run against a Postgres db.
 *
 * @author Jim Grace
 */
class ValidationServiceIntegrationTest extends IntegrationTestBase
{

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private UserService injectUserService;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    private DataElement dataElementA;

    private Period periodA;

    private int dayInPeriodA;

    private OrganisationUnit orgUnitA;

    private PeriodType periodTypeMonthly;

    private CategoryOptionCombo defaultCombo;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        this.userService = injectUserService;

        periodTypeMonthly = new MonthlyPeriodType();
        dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );
        periodA = createPeriod( periodTypeMonthly, getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) );
        dayInPeriodA = periodService.getDayInPeriod( periodA, new Date() );
        orgUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( orgUnitA );
        defaultCombo = categoryService.getDefaultCategoryOptionCombo();

        User user = createAndAddUser( true, "SUPERUSER", Sets.newHashSet( orgUnitA ), null );
        injectSecurityContext( user );
    }

    // -------------------------------------------------------------------------
    // Business logic tests
    // -------------------------------------------------------------------------
    /**
     * See https://jira.dhis2.org/browse/DHIS2-10336.
     */
    @Test
    void testDataElementAndDEO()
    {
        dataValueService
            .addDataValue( createDataValue( dataElementA, periodA, orgUnitA, defaultCombo, defaultCombo, "10" ) );
        Expression expressionLeft = new Expression(
            "#{" + dataElementA.getUid() + "} + #{" + dataElementA.getUid() + "." + defaultCombo.getUid() + "}",
            "expressionLeft" );
        Expression expressionRight = new Expression( "10", "expressionRight" );
        ValidationRule validationRule = createValidationRule( "R", equal_to, expressionLeft, expressionRight,
            periodTypeMonthly );
        validationRuleService.saveValidationRule( validationRule );
        Collection<ValidationResult> results = validationService.validationAnalysis( validationService
            .newParamsBuilder( Lists.newArrayList( validationRule ), orgUnitA, Lists.newArrayList( periodA ) )
            .build(), NoopJobProgress.INSTANCE );
        ValidationResult referenceA = new ValidationResult( validationRule, periodA, orgUnitA, defaultCombo, 20.0, 10.0,
            dayInPeriodA );
        assertEquals( 1, results.size() );
        assertTrue( results.contains( referenceA ) );
    }
}
