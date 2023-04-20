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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
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
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class SchemeIdResponseMapperTest
{
    private SchemeIdResponseMapper schemeIdResponseMapper;

    @BeforeEach
    public void setUp()
    {
        schemeIdResponseMapper = new SchemeIdResponseMapper();
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToName()
    {
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( NAME );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( CODE );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( UUID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub );
        theDataQueryParams.setOutputIdScheme( UUID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElement> dataElementsStub = stubDataElements();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubDataElementQueryParams( dataElementsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( NAME );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementsStub.get( 0 );
        DataElement dataElementB = dataElementsStub.get( 1 );

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( orUnitStub.getUid() ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getUid() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getName() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getName() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToCodeForDataValueSet()
    {
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( CODE );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( UUID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputDataElementIdScheme( UID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( NAME );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( CODE );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( UUID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputOrgUnitIdScheme( UID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputIdScheme( NAME );

        // Overriding output id schema and setting CODE for Org Unit
        theDataQueryParams.setOutputOrgUnitIdScheme( CODE );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputIdScheme( NAME );

        // Overriding output id schema and setting CODE for Data
        // Element/Operands
        theDataQueryParams.setOutputDataElementIdScheme( CODE );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

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
        List<DataElementOperand> dataElementOperandsStub = stubDataElementOperands();
        OrganisationUnit orUnitStub = stubOrgUnit();
        Period periodStub = stubPeriod();
        DataQueryParams theDataQueryParams = stubQueryParams( dataElementOperandsStub, orUnitStub, periodStub,
            DATA_VALUE_SET );
        theDataQueryParams.setOutputIdScheme( NAME );

        // Overriding output id schema and setting CODE for Data
        // Element/Operands
        theDataQueryParams.setOutputDataElementIdScheme( CODE );

        // Overriding output id schema and setting ID for Org Unit
        theDataQueryParams.setOutputOrgUnitIdScheme( ID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( theDataQueryParams );

        String orgUnitUid = orUnitStub.getUid();
        String periodIsoDate = periodStub.getIsoDate();
        DataElement dataElementA = dataElementOperandsStub.get( 0 ).getDataElement();
        DataElement dataElementB = dataElementOperandsStub.get( 1 ).getDataElement();
        CategoryOptionCombo categoryOptionComboC = dataElementOperandsStub.get( 0 ).getCategoryOptionCombo();

        assertThat( responseMap.get( orgUnitUid ), is( equalTo( valueOf( orUnitStub.getId() ) ) ) );
        assertThat( responseMap.get( periodIsoDate ), is( equalTo( periodStub.getName() ) ) );
        assertThat( responseMap.get( dataElementA.getUid() ), is( equalTo( dataElementA.getCode() ) ) );
        assertThat( responseMap.get( dataElementB.getUid() ), is( equalTo( dataElementB.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
        assertThat( responseMap.get( categoryOptionComboC.getUid() ), is( equalTo( categoryOptionComboC.getCode() ) ) );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsNameInCommonParams()
    {
        Program program = createProgram( 'A' );
        program.setName( "Name" );
        program.setCode( "Code" );

        CommonParams commonParams = stubCommonParams( program, NAME );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( commonParams );

        assertEquals( responseMap.get( program.getUid() ), program.getName() );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsCodeInCommonParams()
    {
        Program program = createProgram( 'A' );
        program.setName( "Name" );
        program.setCode( "Code" );

        CommonParams commonParams = stubCommonParams( program, CODE );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( commonParams );

        assertEquals( responseMap.get( program.getUid() ), program.getCode() );
    }

    @Test
    void testGetSchemeIdResponseMapWhenOutputIdSchemeIsUidInCommonParams()
    {
        Program program = createProgram( 'A' );
        program.setName( "Name" );
        program.setCode( "Code" );

        CommonParams commonParams = stubCommonParams( program, UID );

        Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap( commonParams );

        // Uid is the default, so conversion is not needed. The map will not contain any conversion on this case.
        assertNull( responseMap.get( program.getUid() ) );
    }

    @Test
    void testApplyOptionAndLegendSetMappingWhenHasOptionSetAndSchemeNAME()
    {
        // Given
        Option option = new Option( "name", "code" );
        option.setUid( "uid" );

        OptionSet optionSet = new OptionSet();
        optionSet.addOption( option );

        GridHeader gridHeader = new GridHeader( "header", "column", TEXT, false, false, optionSet, null );

        Grid grid = new ListGrid();
        grid.addHeader( gridHeader );
        grid.addMetaData( "code", "value" );
        grid.addRow();
        grid.addValue( "code" );
        grid.addValue( 11 );

        // When
        schemeIdResponseMapper.applyOptionAndLegendSetMapping( grid, NAME );

        // Then
        assertEquals( "name", grid.getRow( 0 ).get( 0 ) );
    }

    @Test
    void testApplyOptionAndLegendSetMappingWhenHasOptionSetAndSchemeCODE()
    {
        // Given
        Option option = new Option( "name", "code" );
        option.setUid( "uid" );

        OptionSet optionSet = new OptionSet();
        optionSet.addOption( option );

        GridHeader gridHeader = new GridHeader( "header", "column", TEXT, false, false, optionSet, null );

        Grid grid = new ListGrid();
        grid.addHeader( gridHeader );
        grid.addMetaData( "code", "value" );
        grid.addRow();
        grid.addValue( "code" );
        grid.addValue( 11 );

        // When
        schemeIdResponseMapper.applyOptionAndLegendSetMapping( grid, CODE );

        // Then
        assertEquals( "code", grid.getRow( 0 ).get( 0 ) );
    }

    @Test
    void testApplyOptionAndLegendSetMappingWhenHasOptionSetAndSchemeUID()
    {
        // Given
        Option option = new Option( "name", "code" );
        option.setUid( "uid" );

        OptionSet optionSet = new OptionSet();
        optionSet.addOption( option );

        GridHeader gridHeader = new GridHeader( "header", "column", TEXT, false, false, optionSet, null );

        Grid grid = new ListGrid();
        grid.addHeader( gridHeader );
        grid.addMetaData( "code", "value" );
        grid.addRow();
        grid.addValue( "code" );
        grid.addValue( 11 );

        // When
        schemeIdResponseMapper.applyOptionAndLegendSetMapping( grid, UID );

        // Then
        assertEquals( "uid", grid.getRow( 0 ).get( 0 ) );
    }

    @Test
    void testApplyOptionAndLegendSetMappingWhenHasLegendSetAndSchemeNAME()
    {
        // Given
        Legend legend = new Legend();
        legend.setUid( "uid" );
        legend.setCode( "code" );
        legend.setName( "name" );

        LegendSet legendSet = new LegendSet();
        legendSet.getLegends().add( legend );

        GridHeader gridHeader = new GridHeader( "header", "column", TEXT, false, false, null, legendSet );

        Grid grid = new ListGrid();
        grid.addHeader( gridHeader );
        grid.addMetaData( "uid", "value" );
        grid.addRow();
        grid.addValue( "uid" );
        grid.addValue( 11 );

        // When
        schemeIdResponseMapper.applyOptionAndLegendSetMapping( grid, NAME );

        // Then
        assertEquals( "name", grid.getRow( 0 ).get( 0 ) );
    }

    @Test
    void testApplyOptionAndLegendSetMappingWhenHasLegendSetAndSchemeCODE()
    {
        // Given
        Legend legend = new Legend();
        legend.setUid( "uid" );
        legend.setCode( "code" );
        legend.setName( "name" );

        LegendSet legendSet = new LegendSet();
        legendSet.getLegends().add( legend );

        GridHeader gridHeader = new GridHeader( "header", "column", TEXT, false, false, null, legendSet );

        Grid grid = new ListGrid();
        grid.addHeader( gridHeader );
        grid.addMetaData( "uid", "value" );
        grid.addRow();
        grid.addValue( "uid" );
        grid.addValue( 11 );

        // When
        schemeIdResponseMapper.applyOptionAndLegendSetMapping( grid, CODE );

        // Then
        assertEquals( "code", grid.getRow( 0 ).get( 0 ) );
    }

    @Test
    void testApplyOptionAndLegendSetMappingWhenHasLegendSetAndSchemeUID()
    {
        // Given
        Legend legend = new Legend();
        legend.setUid( "uid" );
        legend.setCode( "code" );
        legend.setName( "name" );

        LegendSet legendSet = new LegendSet();
        legendSet.getLegends().add( legend );

        GridHeader gridHeader = new GridHeader( "header", "column", TEXT, false, false, null, legendSet );

        Grid grid = new ListGrid();
        grid.addHeader( gridHeader );
        grid.addMetaData( "uid", "value" );
        grid.addRow();
        grid.addValue( "uid" );
        grid.addValue( 11 );

        // When
        schemeIdResponseMapper.applyOptionAndLegendSetMapping( grid, UID );

        // Then
        assertEquals( "uid", grid.getRow( 0 ).get( 0 ) );
    }

    private CommonParams stubCommonParams( Program program, IdScheme idScheme )
    {
        List<DimensionIdentifier<DimensionParam>> dimIdentifiers = getDimensionIdentifiers();

        return CommonParams.builder().programs( List.of( program ) )
            .dimensionIdentifiers( dimIdentifiers )
            .outputIdScheme( idScheme )
            .build();
    }

    private List<DimensionIdentifier<DimensionParam>> getDimensionIdentifiers()
    {
        List<String> ous = List.of( "ou1-uid", "ou2-uid" );

        DimensionIdentifier<DimensionParam> dimensionIdentifierA = stubDimensionIdentifier(
            ous, "Z8z5uu61HAb", "tO8L1aBitDm", "teaA-uid" );

        DimensionIdentifier<DimensionParam> dimensionIdentifierB = stubDimensionIdentifier(
            ous, "Z8z5uu61HAb", "tO8L1aBitDm", "teaB-uid" );

        List<DimensionIdentifier<DimensionParam>> dimIdentifiers = new ArrayList<>();
        dimIdentifiers.add( dimensionIdentifierA );
        dimIdentifiers.add( dimensionIdentifierB );

        return dimIdentifiers;
    }

    private DimensionIdentifier<DimensionParam> stubDimensionIdentifier( List<String> ous,
        String programUid, String programStageUid, String dimensionUid )
    {
        BaseDimensionalObject tea = new BaseDimensionalObject( dimensionUid, DATA_X,
            ous.stream()
                .map( item -> new BaseDimensionalItemObject( item ) )
                .collect( Collectors.toList() ),
            TEXT );

        DimensionParam dimensionParam = DimensionParam.ofObject( tea, DIMENSIONS, ous );

        ElementWithOffset program = emptyElementWithOffset();
        ElementWithOffset programStage = emptyElementWithOffset();

        if ( isNotBlank( programUid ) )
        {
            Program p = new Program();
            p.setUid( programUid );
            program = ElementWithOffset.of( p, null );
        }

        if ( isNotBlank( programStageUid ) )
        {
            ProgramStage ps = new ProgramStage();
            ps.setUid( programStageUid );
            programStage = ElementWithOffset.of( ps, null );
        }

        return DimensionIdentifier.of( program, programStage, dimensionParam );
    }

    private DataQueryParams stubQueryParams( List<DataElementOperand> dataElementOperands,
        OrganisationUnit organisationUnit, Period period )
    {
        return stubQueryParams( dataElementOperands, organisationUnit, period, null );
    }

    private DataQueryParams stubQueryParams( List<DataElementOperand> dataElementOperands,
        OrganisationUnit organisationUnit, Period period, OutputFormat outputFormat )
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

    private DataQueryParams stubDataElementQueryParams( List<DataElement> dataElements,
        OrganisationUnit organisationUnit, Period period, OutputFormat outputFormat )
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
        Period period = getPeriodFromIsoString( "202010" );
        period.setUid( "pe" );
        period.setName( "October 2020" );

        return period;
    }

    private List<DataElementOperand> stubDataElementOperands()
    {
        DataElement dataElementA = new DataElement( "NameA" );
        dataElementA.setUid( "uid1234567A" );
        dataElementA.setCode( "CodeA" );

        DataElement dataElementB = new DataElement( "NameB" );
        dataElementB.setUid( "uid1234567B" );
        dataElementB.setCode( "CodeB" );

        CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
        categoryOptionCombo.setName( "NameC" );
        categoryOptionCombo.setUid( "uid1234567C" );
        categoryOptionCombo.setCode( "CodeC" );

        DataElementOperand dataElementOperandA = new DataElementOperand( dataElementA, categoryOptionCombo );
        DataElementOperand dataElementOperandB = new DataElementOperand( dataElementB, categoryOptionCombo );

        return newArrayList( dataElementOperandA, dataElementOperandB );
    }

    private List<DataElement> stubDataElements()
    {
        DataElement dataElementA = new DataElement( "NameA" );
        dataElementA.setUid( "uid1234567A" );
        dataElementA.setCode( "CodeA" );

        DataElement dataElementB = new DataElement( "NameB" );
        dataElementB.setUid( "uid1234567B" );
        dataElementB.setCode( "CodeB" );

        return newArrayList( dataElementA, dataElementB );
    }

    private OrganisationUnit stubOrgUnit()
    {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setName( "OrgUnitA" );
        organisationUnit.setShortName( "ShortOrgUnitA" );
        organisationUnit.setUid( "org1234567A" );
        organisationUnit.setCode( "CodeA" );
        organisationUnit.setId( 1 );

        return organisationUnit;
    }
}
