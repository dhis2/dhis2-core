/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.datavalue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

import java.util.Date;

import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class DataValidatorTest
{
    @Mock
    private CategoryService categoryService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DataSetService dataSetService;

    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private InputUtils inputUtils;

    @Mock
    private FileResourceService fileResourceService;

    @Mock
    private CalendarService calendarService;

    @Mock
    private AggregateAccessManager accessManager;

    @Mock
    private DataValidator dataValidator;

    @Rule
    public MockitoRule mockitoRule = rule();

    private Period periodJan;

    private Period periodFeb;

    private Period periodMar;

    private DataSet dataSetA;

    private DataElement dataElementA;

    private CategoryOption categoryOptionA;

    private CategoryOptionCombo optionComboA;

    private Date jan15;

    private Date feb15;

    @Before
    public void setUp()
    {
        dataValidator = new DataValidator( categoryService, organisationUnitService, dataSetService,
            idObjectManager, inputUtils, fileResourceService, calendarService, accessManager );

        periodJan = createPeriod( "202001" );
        periodFeb = createPeriod( "202002" );
        periodMar = createPeriod( "202003" );

        dataSetA = new DataSet( "dataSet", new MonthlyPeriodType() );
        dataElementA = new DataElement();
        categoryOptionA = new CategoryOption();
        optionComboA = new CategoryOptionCombo();

        dataSetA.addDataSetElement( dataElementA );
        dataElementA.getDataSetElements().addAll( dataSetA.getDataSetElements() );

        optionComboA.addCategoryOption( categoryOptionA );

        jan15 = getDate( 2020, 1, 15 );
        feb15 = getDate( 2020, 2, 15 );
    }

    /**
     * Creates a date.
     *
     * @param year the year.
     * @param month the month.
     * @param day the day of month.
     * @return a date.
     */
    public static Date getDate( int year, int month, int day )
    {
        LocalDateTime dateTime = new LocalDateTime( year, month, day, 0, 0 );
        return dateTime.toDate();
    }

    /**
     * @param isoPeriod the ISO period string.
     */
    public static Period createPeriod( String isoPeriod )
    {
        return PeriodType.getPeriodFromIsoString( isoPeriod );
    }

    @Test
    public void testValidateAttributeOptionComboWithValidData()
    {
        // Initially
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, dataElementA );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, null );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, null, dataElementA );

        dataValidator.validateAttributeOptionCombo( optionComboA, periodFeb, dataSetA, dataElementA );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodMar, dataSetA, dataElementA );

        // Given
        categoryOptionA.setStartDate( jan15 );

        // Then
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, dataElementA );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, null );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, null, dataElementA );

        dataValidator.validateAttributeOptionCombo( optionComboA, periodFeb, dataSetA, dataElementA );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodMar, dataSetA, dataElementA );

        // And given
        categoryOptionA.setEndDate( jan15 );

        // Then
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, dataElementA );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, null );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, null, dataElementA );

        // And given
        dataSetA.setOpenPeriodsAfterCoEndDate( 1 );

        // Then
        dataValidator.validateAttributeOptionCombo( optionComboA, periodFeb, dataSetA, dataElementA );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodFeb, dataSetA, null );
        dataValidator.validateAttributeOptionCombo( optionComboA, periodFeb, null, dataElementA );
    }

    @Test
    public void testGetMissingDataElement()
    {
        final String uid = CodeGenerator.generateUid();

        when( idObjectManager.get( DataElement.class, uid ) ).thenReturn( null );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> dataValidator.getAndValidateDataElement( uid ) );

        assertEquals( ErrorCode.E1100, ex.getErrorCode() );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateAttributeOptionComboWithEarlyData()
    {
        // Given
        categoryOptionA.setStartDate( feb15 );

        // Then
        dataValidator.validateAttributeOptionCombo( optionComboA, periodJan, dataSetA, dataElementA );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateAttributeOptionComboWithLateData()
    {
        // Given
        categoryOptionA.setEndDate( jan15 );

        // Then
        dataValidator.validateAttributeOptionCombo( optionComboA, periodFeb, null, dataElementA );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateAttributeOptionComboWithLateAdjustedData()
    {
        // Given
        categoryOptionA.setEndDate( jan15 );
        dataSetA.setOpenPeriodsAfterCoEndDate( 1 );

        // Then
        dataValidator.validateAttributeOptionCombo( optionComboA, periodMar, dataSetA, dataElementA );
    }

    @Test
    public void validateBooleanDataValueWhenValuesAreAcceptableTrue()
    {
        // Given
        final DataElement aBooleanTypeDataElement = new DataElement();
        final String normalizedBooleanValue = "true";
        aBooleanTypeDataElement.setValueType( BOOLEAN );

        // Then
        String aBooleanDataValue = "true";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "1";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "t";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "True";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "TRUE";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );
    }

    @Test
    public void validateBooleanDataValueWhenValuesAreAcceptableFalse()
    {
        // Given
        final DataElement aBooleanTypeDataElement = new DataElement();
        final String normalizedBooleanValue = "false";
        aBooleanTypeDataElement.setValueType( BOOLEAN );

        // Then
        String aBooleanDataValue = "false";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "0";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "f";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "False";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );

        aBooleanDataValue = "FALSE";
        aBooleanDataValue = dataValidator.validateAndNormalizeDataValue( aBooleanDataValue, aBooleanTypeDataElement );
        assertThat( aBooleanDataValue, is( normalizedBooleanValue ) );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateBooleanDataValueWhenValueIsNotValid()
    {
        // Given
        String anInvalidBooleanValue = "InvalidValue";
        final DataElement aBooleanTypeDataElement = new DataElement();
        aBooleanTypeDataElement.setValueType( BOOLEAN );

        // When
        dataValidator.validateAndNormalizeDataValue( anInvalidBooleanValue, aBooleanTypeDataElement );

        fail( "Should not reach here. It was expected IllegalQueryException." );
    }
}
