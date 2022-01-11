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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataValue;
import static org.hisp.dhis.DhisConvenienceTest.createExpression2;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.hisp.dhis.DhisConvenienceTest.createValidationRule;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class DataValidationTaskTest
{
    @Mock
    private ExpressionService expressionService;

    @Mock
    private DataValueService dataValueService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PeriodService periodService;

    @Mock
    private AnalyticsService analyticsService;

    private PeriodType MONTHLY = PeriodType.getPeriodTypeFromIsoString( "201901" );

    private DataValidationTask subject;

    private DataElement deA;

    private List<OrganisationUnit> organisationUnits;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private Period p1;

    private Period p2;

    private Period p3;

    private Map<String, Constant> constantMap;

    @BeforeEach
    public void setUp()
    {
        subject = new DataValidationTask( expressionService, dataValueService, categoryService, periodService );

        deA = createDataElement( 'A' );

        organisationUnits = new ArrayList<>();

        ouA = createOu( 'A' );
        ouB = createOu( 'B' );
        OrganisationUnit ouC = createOu( 'C' );
        OrganisationUnit ouD = createOu( 'D' );

        organisationUnits.add( ouA );
        organisationUnits.add( ouB );
        organisationUnits.add( ouC );
        organisationUnits.add( ouD );

        p1 = createPeriod( "201901" );
        p2 = createPeriod( "201902" );
        p3 = createPeriod( "201903" );

        constantMap = new HashMap<>();
        constantMap.put( "Gfd3ppDfq8E", new Constant( "a", 5.0 ) );
        constantMap.put( "bCqvfPR02Im", new Constant( "pi", 3.14 ) );
    }

    /**
     * Verify that a single rule passes against a Data Element
     */
    @Test
    void verifySimpleValidation_oneRule_noErrors()
    {
        Expression leftExpression = createExpression2( 'A', "#{FUrCpcvMAmC.OrDRjJL9bTS}" );
        Expression rightExpression = createExpression2( 'B', "-10" );

        ValidationRuleExtended vre = createValidationRuleExtended( leftExpression, rightExpression,
            Operator.not_equal_to );

        List<PeriodTypeExtended> periodTypes = new ArrayList<>();
        PeriodTypeExtended periodType = createPeriodTypeExtended( vre );
        periodType.addDataElement( deA );
        periodTypes.add( periodType );

        CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo( 'A', 'B' );

        ValidationRunContext ctx = ValidationRunContext.newBuilder()
            .withOrgUnits( organisationUnits )
            .withConstantMap( constantMap )
            .withDefaultAttributeCombo( categoryOptionCombo )
            .withPeriodTypeXs( periodTypes )
            .withMaxResults( 500 )
            .build();

        List<DeflatedDataValue> deflatedDataValues = new ArrayList<>();

        DataValue dv = createDataValue( deA, createPeriod( "201901" ), ouA, "12.4",
            createCategoryOptionCombo( 'B', 'C' ) );

        DeflatedDataValue ddv = new DeflatedDataValue( dv );
        deflatedDataValues.add( ddv );

        when( dataValueService.getDeflatedDataValues( any( DataExportParams.class ) ) )
            .thenReturn( deflatedDataValues );

        Map<DimensionalItemObject, Object> vals = new HashMap<>();
        vals.put( deA, 12.4 );

        mockExpressionService( leftExpression, vals, ctx, 8.4 );
        mockExpressionService( rightExpression, vals, ctx, -10.0 );

        when( expressionService.getExpressionValue( "8.4!=-10.0", SIMPLE_TEST ) ).thenReturn( true );

        subject.init( organisationUnits, ctx, analyticsService );
        subject.run();

        assertThat( ctx.getValidationResults().size(), is( 0 ) );
    }

    @Test
    void verifyValidationSkippedOnNoData()
    {
        Expression leftExpression = createExpression2( 'A', "#{FUrCpcvMAmC.OrDRjJL9bTS}" );
        Expression rightExpression = createExpression2( 'B', "-10" );

        ValidationRuleExtended vre = createValidationRuleExtended( leftExpression, rightExpression,
            Operator.not_equal_to );

        List<PeriodTypeExtended> periodTypes = new ArrayList<>();
        PeriodTypeExtended periodType = createPeriodTypeExtended( vre );
        periodType.addDataElement( deA );
        periodTypes.add( periodType );

        CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo( 'A', 'B' );

        ValidationRunContext ctx = ValidationRunContext.newBuilder()
            .withOrgUnits( organisationUnits )
            .withConstantMap( constantMap )
            .withDefaultAttributeCombo( categoryOptionCombo )
            .withPeriodTypeXs( periodTypes )
            .withMaxResults( 500 )
            .build();

        List<DeflatedDataValue> deflatedDataValues = new ArrayList<>();

        // Return no values!
        when( dataValueService.getDeflatedDataValues( any( DataExportParams.class ) ) )
            .thenReturn( deflatedDataValues );

        subject.init( organisationUnits, ctx, analyticsService );
        subject.run();

        assertThat( ctx.getValidationResults().size(), is( 0 ) );
    }

    private void mockExpressionService( Expression expression, Map<DimensionalItemObject, Object> vals,
        ValidationRunContext ctx, Double val )
    {
        when( expressionService.getExpressionValue( expression.getExpression(), VALIDATION_RULE_EXPRESSION, null, vals,
            ctx.getConstantMap(), null, null, p1.getDaysInPeriod(), expression.getMissingValueStrategy(), ouA ) )
                .thenReturn( val );

        when( expressionService.getExpressionValue( expression.getExpression(), VALIDATION_RULE_EXPRESSION, null, vals,
            ctx.getConstantMap(), null, null, p2.getDaysInPeriod(), expression.getMissingValueStrategy(), ouA ) )
                .thenReturn( val );

        when( expressionService.getExpressionValue( expression.getExpression(), VALIDATION_RULE_EXPRESSION, null, vals,
            ctx.getConstantMap(), null, null, p3.getDaysInPeriod(), expression.getMissingValueStrategy(), ouA ) )
                .thenReturn( val );
    }

    private ValidationRuleExtended createValidationRuleExtended( Expression left, Expression right, Operator op )
    {
        return new ValidationRuleExtended( createValidationRule( 'A', op, left, right, MONTHLY ) );
    }

    private PeriodTypeExtended createPeriodTypeExtended( ValidationRuleExtended... validationRuleExtended )
    {
        PeriodTypeExtended pt = new PeriodTypeExtended( MONTHLY );
        // add three months
        pt.addPeriod( p1 );
        pt.addPeriod( p2 );
        pt.addPeriod( p3 );
        // add the actual validation rule
        for ( ValidationRuleExtended ruleExtended : validationRuleExtended )
        {
            pt.getRuleXs().add( ruleExtended );

        }
        pt.setSlidingWindows( false );
        return pt;
    }

    private OrganisationUnit createOu( char uniqueCharacter )
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( uniqueCharacter );
        organisationUnit.setId( RandomUtils.nextLong() );
        return organisationUnit;
    }
}