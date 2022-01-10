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
package org.hisp.dhis.analytics.data.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.common.IdScheme.ID;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.common.IdScheme.UUID;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class SchemaIdResponseMapperTest
{

    private SchemaIdResponseMapper schemaIdResponseMapper;

    @BeforeEach
    public void setUp()
    {
        schemaIdResponseMapper = new SchemaIdResponseMapper();
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToName()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( NAME );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getName() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getName() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getName() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getName() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getName() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getName() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToCode()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( CODE );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getCode() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getCode() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToUuid()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( UUID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( periodIsoDate ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToUid()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( UUID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( periodIsoDate ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToNameForDataValueSet()
    {
        // Given
        final List<DataElement> dataElementsStub = stubDataElements();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubDataElementQueryParams( dataElementsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( NAME );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementsStub.get( 0 );
        final DataElement dataElementB = dataElementsStub.get( 1 );

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getUid() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getName() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getName() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToCodeForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( CODE );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getUid() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getCode() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToUuidForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( UUID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getUid() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToUidForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( UID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getUid() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToNameForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( NAME );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getName() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToCodeForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( CODE );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getCode() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToUuidForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( UUID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( periodIsoDate ), is( periodStub.getUid() ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToUidForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( UID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getUid() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( emptyOrNullString() ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeOverridesOutputOrgUnitIdSchemeForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputIdScheme( NAME );

        // Overriding output id schema and setting CODE for Org Unit
        theDataQueryParams.setOutputOrgUnitIdScheme( CODE );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getCode() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getName() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getName() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getName() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getName() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getName() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeOverridesOutputOrgUnitIdSchemeForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputIdScheme( NAME );

        // Overriding output id schema and setting CODE for Data
        // Element/Operands
        theDataQueryParams.setOutputDataElementIdScheme( CODE );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getName() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getName() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getCode() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementAndOrgUnitIdSchemeOverrideOutputOrgUnitIdSchemeForDataValueSet()
    {
        // Given
        final List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        final OrganisationUnit orUnitStub = stubOrgUnit();
        final Period periodStub = stubPeriod();
        final DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputIdScheme( NAME );

        // Overriding output id schema and setting CODE for Data
        // Element/Operands
        theDataQueryParams.setOutputDataElementIdScheme( CODE );

        // Overriding output id schema and setting ID for Org Unit
        theDataQueryParams.setOutputOrgUnitIdScheme( ID );

        // When
        final Map<String, String> responseMap = schemaIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        // Then
        final String orgUnitUid = orUnitStub.getUid();
        final String periodIsoDate = periodStub.getIsoDate();
        final DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        final DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        final CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( valueOf( orUnitStub.getId() ) ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getName() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getCode() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
    }

    private DataQueryParams stubQueryParams( final List<DataElementOperand> dataElementOperands,
        final OrganisationUnit organisationUnit, final Period period )
    {
        return stubQueryParams( dataElementOperands, organisationUnit, period, null );
    }

    private DataQueryParams stubQueryParams( final List<DataElementOperand> dataElementOperands,
        final OrganisationUnit organisationUnit, final Period period, final OutputFormat outputFormat )
    {
        return newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DATA_X, dataElementOperands ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, ORGANISATION_UNIT, newArrayList( organisationUnit ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD,
                newArrayList( period ) ) )
            .withOutputFormat( outputFormat )
            .build();
    }

    private DataQueryParams stubDataElementQueryParams( final List<DataElement> dataElements,
        final OrganisationUnit organisationUnit, final Period period, final OutputFormat outputFormat )
    {
        return newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DATA_X, dataElements ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, ORGANISATION_UNIT, newArrayList( organisationUnit ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, PERIOD,
                newArrayList( period ) ) )
            .withOutputFormat( outputFormat )
            .build();
    }

    private Period stubPeriod()
    {
        final Period period = getPeriodFromIsoString( "202010" );
        period.setUid( "pe" );
        period.setName( "October 2020" );

        return period;
    }

    private List<DataElementOperand> stubDataElementOperands()
    {
        final DataElement dataElementA = new DataElement( "NameA" );
        dataElementA.setUid( "uid1234567A" );
        dataElementA.setCode( "CodeA" );

        final DataElement dataElementB = new DataElement( "NameB" );
        dataElementB.setUid( "uid1234567B" );
        dataElementB.setCode( "CodeB" );

        final CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
        categoryOptionCombo.setName( "NameC" );
        categoryOptionCombo.setUid( "uid1234567C" );
        categoryOptionCombo.setCode( "CodeC" );

        final DataElementOperand dataElementOperandA = new DataElementOperand( dataElementA, categoryOptionCombo );
        final DataElementOperand dataElementOperandB = new DataElementOperand( dataElementB, categoryOptionCombo );

        return newArrayList( dataElementOperandA, dataElementOperandB );
    }

    private List<DataElement> stubDataElements()
    {
        final DataElement dataElementA = new DataElement( "NameA" );
        dataElementA.setUid( "uid1234567A" );
        dataElementA.setCode( "CodeA" );

        final DataElement dataElementB = new DataElement( "NameB" );
        dataElementB.setUid( "uid1234567B" );
        dataElementB.setCode( "CodeB" );

        return newArrayList( dataElementA, dataElementB );
    }

    private OrganisationUnit stubOrgUnit()
    {
        final OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setName( "OrgUnitA" );
        organisationUnit.setShortName( "ShortOrgUnitA" );
        organisationUnit.setUid( "org1234567A" );
        organisationUnit.setCode( "CodeA" );
        organisationUnit.setId( 1 );

        return organisationUnit;
    }
}
