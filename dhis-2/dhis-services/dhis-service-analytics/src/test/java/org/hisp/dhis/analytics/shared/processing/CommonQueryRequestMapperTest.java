/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.shared.processing;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.analytics.EventOutputType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.EventDataQueryRequest.ExtendedEventDataQueryRequestBuilder.DIMENSION_OR_SEPARATOR;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset;
import org.hisp.dhis.analytics.common.dimension.StringUid;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.common.AnalyticsPagingCriteria;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CommonQueryRequestMapper}
 */
@ExtendWith( MockitoExtension.class )
class CommonQueryRequestMapperTest
{

    @Mock
    private I18nManager i18nManager;

    @Mock
    private DataQueryService dataQueryService;

    @Mock
    private EventDataQueryService eventDataQueryService;

    @Mock
    private ProgramService programService;

    @Mock
    private DimensionIdentifierConverter dimensionIdentifierConverter;

    @Test
    void mapWithSuccessOnlyDimension()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        final I18nFormat anyI18nFormat = new I18nFormat();
        final String queryItem = "EQ:john";
        final String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";

        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );
        final List<Program> programs = List.of( program1, program2 );

        final DimensionIdentifier<Program, ProgramStage, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ),
            StringUid.of( "yLIPuJHRgey" ) );

        final BaseDimensionalObject dimensionalObject = new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(), DATA_X, null, DISPLAY_NAME_DATA_X,
            emptyList(), new DimensionItemKeywords() );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( "PEZNsGbZaVJ" )
            .withProgram( Set.of( "ur1Edk5Oe2n", "lxAQ7Zs9VYR" ) )
            .withDimension( Set.of( dimension + ":" + queryItem ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( programs );
        when( i18nManager.getI18nFormat() ).thenReturn( anyI18nFormat );
        when( dimensionIdentifierConverter.fromString( programs, dimension ) ).thenReturn( deDimensionIdentifier );
        when( (dataQueryService.getDimension( deDimensionIdentifier.getDimension().getUid(),
            List.of( queryItem ), aCommonQueryRequest.getRelativePeriodDate(), organisationUnits,
            i18nManager.getI18nFormat(), true, UID )) ).thenReturn( dimensionalObject );

        // When
        final CommonParams params = new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
            programService, dimensionIdentifierConverter ).map( aCommonQueryRequest );

        // Then
        assertEquals( 2, params.getPrograms().size(), "Should contain 2 programs." );
        assertEquals( "lxAQ7Zs9VYR", params.getPrograms().get( 0 ).getUid(), "First program should be lxAQ7Zs9VYR." );
        assertEquals( "ur1Edk5Oe2n", params.getPrograms().get( 1 ).getUid(), "Second program should be ur1Edk5Oe2n." );
        assertEquals( 1, params.getDimensionIdentifiers().size(), "Should contain 1 identifier." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgram(), "Should contain 1 program." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgramStage(),
            "Should contain 1 program stage." );
        assertEquals( "yLIPuJHRgey",
            params.getDimensionIdentifiers().get( 0 ).get( 0 ).getDimension().getDimensionalObject().getDimension(),
            "Dimension identifier should be yLIPuJHRgey." );
        assertEquals( "1", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgram().getOffset(),
            "Program offset should be 1." );
        assertEquals( "2", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgramStage().getOffset(),
            "ProgramStage offset should be 2." );
        assertFalse( params.getPagingAndSortingParams().isEmpty(), "Paging and sorting should not be empty." );
    }

    @Test
    void mapWithSuccessOnlyFilter()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        final String queryItem = "ur1Edk5Oe2n.OU";
        final String orgUnitUid = "PEZNsGbZaVJ";
        final I18nFormat anyI18nFormat = new I18nFormat();

        final AnalyticsPagingCriteria theAnalyticsPagingCriteria = new AnalyticsPagingCriteria();
        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );
        final List<Program> programs = List.of( program1, program2 );

        final DimensionIdentifier<Program, StringUid, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ), null, StringUid.of( null ) );

        final DimensionIdentifier<Program, ProgramStage, StringUid> ouDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ), StringUid.of( queryItem ) );

        final BaseDimensionalObject dimensionalObject = new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(), DATA_X, null, DISPLAY_NAME_DATA_X,
            emptyList(), new DimensionItemKeywords() );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( orgUnitUid )
            .withProgram( Set.of( "lxAQ7Zs9VYR", "ur1Edk5Oe2n" ) )
            .withFilter( Set.of( "ur1Edk5Oe2n.OU:PEZNsGbZaVJ" ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( programs );
        when( i18nManager.getI18nFormat() ).thenReturn( anyI18nFormat );
        when( dimensionIdentifierConverter.fromString( programs, queryItem ) ).thenReturn( ouDimensionIdentifier );
        when( (dataQueryService.getDimension( ouDimensionIdentifier.getDimension().getUid(),
            List.of( orgUnitUid ), aCommonQueryRequest.getRelativePeriodDate(),
            organisationUnits, i18nManager.getI18nFormat(), true, UID )) ).thenReturn( dimensionalObject );

        // When
        final CommonParams params = new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
            programService, dimensionIdentifierConverter ).map( aCommonQueryRequest );

        // Then
        assertEquals( 2, params.getPrograms().size(), "Should contain 2 programs." );
        assertEquals( "lxAQ7Zs9VYR", params.getPrograms().get( 0 ).getUid(), "First program should be lxAQ7Zs9VYR." );
        assertEquals( "ur1Edk5Oe2n", params.getPrograms().get( 1 ).getUid(), "Second program should be ur1Edk5Oe2n." );
        assertEquals( 1, params.getDimensionIdentifiers().size(), "Should contain 1 identifier." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgramStage(),
            "Should contain 1 program stage." );
        assertNull(
            params.getDimensionIdentifiers().get( 0 ).get( 0 ).getDimension().getDimensionalObject().getDimension(),
            "Dimension identifier should be yLIPuJHRgey." );
        assertEquals( "1", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgram().getOffset(),
            "Program offset should be 1." );
        assertEquals( "2", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgramStage().getOffset(),
            "ProgramStage offset should be 2." );
        assertFalse( params.getPagingAndSortingParams().isEmpty(), "Paging and sorting should not be empty." );
    }

    @Test
    void mapWithSuccessDimensionAndFilter()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        final String orgUnitDimension = "ur1Edk5Oe2n.OU";
        final String queryItemFilter = "PEZNsGbZaVJ";
        final String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";
        final String queryItemDimension = "EQ:john";
        final I18nFormat anyI18nFormat = new I18nFormat();
        final AnalyticsPagingCriteria theAnalyticsPagingCriteria = new AnalyticsPagingCriteria();
        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );
        final List<Program> programs = List.of( program1, program2 );

        final DimensionIdentifier<Program, ProgramStage, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ), StringUid.of( queryItemDimension ) );

        final DimensionIdentifier<Program, ProgramStage, StringUid> ouDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ), StringUid.of( orgUnitDimension ) );

        final BaseDimensionalObject dimensionalObject = new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(), DATA_X, null, DISPLAY_NAME_DATA_X,
            emptyList(), new DimensionItemKeywords() );

        final BaseDimensionalObject orgUnitObject = new BaseDimensionalObject(
            ouDimensionIdentifier.getDimension().getUid(), ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
            organisationUnits, new DimensionItemKeywords() );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( queryItemFilter )
            .withProgram( Set.of( "lxAQ7Zs9VYR", "ur1Edk5Oe2n" ) )
            .withDimension( Set.of( dimension + ":" + queryItemDimension ) )
            .withFilter( Set.of( "ur1Edk5Oe2n.OU:PEZNsGbZaVJ" ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( programs );
        when( i18nManager.getI18nFormat() ).thenReturn( anyI18nFormat );

        when( dimensionIdentifierConverter.fromString( programs, dimension ) ).thenReturn( deDimensionIdentifier );
        when( (dataQueryService.getDimension( deDimensionIdentifier.getDimension().getUid(),
            List.of( queryItemDimension ), aCommonQueryRequest.getRelativePeriodDate(), organisationUnits,
            i18nManager.getI18nFormat(), true, UID )) ).thenReturn( dimensionalObject );

        when( dimensionIdentifierConverter.fromString( programs, orgUnitDimension ) )
            .thenReturn( ouDimensionIdentifier );
        when( (dataQueryService.getDimension( ouDimensionIdentifier.getDimension().getUid(),
            List.of( queryItemFilter ), aCommonQueryRequest.getRelativePeriodDate(),
            organisationUnits, i18nManager.getI18nFormat(), true, UID )) ).thenReturn( orgUnitObject );

        // When
        final CommonParams params = new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
            programService, dimensionIdentifierConverter ).map( aCommonQueryRequest );

        // Then
        assertEquals( 2, params.getPrograms().size(), "Should contain 2 programs." );
        assertEquals( "lxAQ7Zs9VYR", params.getPrograms().get( 0 ).getUid(), "First program should be lxAQ7Zs9VYR." );
        assertEquals( "ur1Edk5Oe2n", params.getPrograms().get( 1 ).getUid(), "Second program should be ur1Edk5Oe2n." );
        assertEquals( 2, params.getDimensionIdentifiers().size(), "Should contain 2 identifiers." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgram(), "Should contain 1 program." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgramStage(),
            "Should contain 1 program stage." );
        assertEquals( "EQ:john",
            params.getDimensionIdentifiers().get( 0 ).get( 0 ).getDimension().getDimensionalObject().getDimension(),
            "Dimension identifier should be yLIPuJHRgey." );
        assertEquals( "1", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgram().getOffset(),
            "Program offset should be 1." );
        assertEquals( "2", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgramStage().getOffset(),
            "ProgramStage offset should be 2." );
        assertFalse( params.getPagingAndSortingParams().isEmpty(), "Paging and sorting should not be empty." );
    }

    @Test
    void mapWhenProgramsCannotBeFound()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );

        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        final String queryItem = "EQ:john";
        final String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";

        final AnalyticsPagingCriteria theAnalyticsPagingCriteria = new AnalyticsPagingCriteria();
        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );

        final DimensionIdentifier<Program, ProgramStage, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ),
            StringUid.of( "yLIPuJHRgey" ) );

        // List has only one Program, but the CommonQueryRequest, below, has
        // two.
        final List<Program> programs = List.of( program1 );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( "PEZNsGbZaVJ" )
            .withDimension( Set.of( dimension + ":" + queryItem ) )
            // Two programs, where "ur1Edk5Oe2n" does not exist.
            .withProgram( Set.of( "lxAQ7Zs9VYR", "ur1Edk5Oe2n" ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( programs );

        // When
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
                programService, dimensionIdentifierConverter ).map( aCommonQueryRequest ) );

        // Then
        assertEquals( "The following programs couldn't be found: [ur1Edk5Oe2n]", thrown.getMessage(),
            "Exception message does not match." );
    }

    @Test
    void mapWhenDimensionIsNotObject()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        final I18nFormat anyI18nFormat = new I18nFormat();
        final String queryItem = "EQ:john";
        final String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";
        final QueryItem anyQueryItem = new QueryItem( new DataElement() );

        final AnalyticsPagingCriteria theAnalyticsPagingCriteria = new AnalyticsPagingCriteria();
        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );
        final List<Program> programs = List.of( program1, program2 );

        final DimensionIdentifier<Program, ProgramStage, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ),
            StringUid.of( "yLIPuJHRgey" ) );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( "PEZNsGbZaVJ" )
            .withProgram( Set.of( "lxAQ7Zs9VYR", "ur1Edk5Oe2n" ) )
            .withDimension( Set.of( dimension + ":" + queryItem ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( programs );
        when( i18nManager.getI18nFormat() ).thenReturn( anyI18nFormat );
        when( dimensionIdentifierConverter.fromString( programs, dimension ) ).thenReturn( deDimensionIdentifier );
        when( (dataQueryService.getDimension( deDimensionIdentifier.getDimension().getUid(),
            List.of( queryItem ), aCommonQueryRequest.getRelativePeriodDate(), organisationUnits,
            i18nManager.getI18nFormat(), true, UID )) ).thenReturn( null );
        when( eventDataQueryService.getQueryItem( deDimensionIdentifier.getDimension().getUid(),
            (Program) deDimensionIdentifier.getProgram().getElement(), TRACKED_ENTITY_INSTANCE ) )
                .thenReturn( anyQueryItem );

        // When
        final CommonParams params = new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
            programService, dimensionIdentifierConverter ).map( aCommonQueryRequest );

        // Then
        assertEquals( 2, params.getPrograms().size(), "Should contain 2 programs." );
        assertEquals( "lxAQ7Zs9VYR", params.getPrograms().get( 0 ).getUid(), "First program should be lxAQ7Zs9VYR." );
        assertEquals( "ur1Edk5Oe2n", params.getPrograms().get( 1 ).getUid(), "Second program should be ur1Edk5Oe2n." );
        assertEquals( 1, params.getDimensionIdentifiers().size(), "Should contain 1 identifier." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgram(), "Should contain 1 program." );
        assertTrue( params.getDimensionIdentifiers().get( 0 ).get( 0 ).hasProgramStage(),
            "Should contain 1 program stage." );
        assertNull( params.getDimensionIdentifiers().get( 0 ).get( 0 ).getDimension().getDimensionalObject(),
            "Dimensional object should be null." );
        assertEquals( "1", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgram().getOffset(),
            "Program offset should be 1." );
        assertEquals( "2", params.getDimensionIdentifiers().get( 0 ).get( 0 ).getProgramStage().getOffset(),
            "ProgramStage offset should be 2." );
        assertFalse( params.getPagingAndSortingParams().isEmpty(), "Paging and sorting should not be empty." );
    }

    @Test
    void mapWhenDimensionIsNotObjectAndProgramAndStageAreNotSet()
    {
        // Given
        final I18nFormat anyI18nFormat = new I18nFormat();
        final String queryItem = "EQ:john";
        final String nonFullyQualifiedDimension = "yLIPuJHRgey";

        final AnalyticsPagingCriteria theAnalyticsPagingCriteria = new AnalyticsPagingCriteria();
        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );
        final List<Program> noPrograms = emptyList();

        final DimensionIdentifier<Program, ProgramStage, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            null, // The null program
            null, // The null stage
            StringUid.of( nonFullyQualifiedDimension ) );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( "PEZNsGbZaVJ" )
            .withDimension( Set.of( nonFullyQualifiedDimension + ":" + queryItem ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( noPrograms );
        when( i18nManager.getI18nFormat() ).thenReturn( anyI18nFormat );
        when( dimensionIdentifierConverter.fromString( noPrograms, nonFullyQualifiedDimension ) )
            .thenReturn( deDimensionIdentifier );
        when( (dataQueryService.getDimension( deDimensionIdentifier.getDimension().getUid(),
            List.of( queryItem ), aCommonQueryRequest.getRelativePeriodDate(), organisationUnits,
            i18nManager.getI18nFormat(), true, UID )) ).thenReturn( null );

        // When
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
                programService, dimensionIdentifierConverter ).map( aCommonQueryRequest ) );

        // Then
        assertEquals( "yLIPuJHRgey is not a fully qualified dimension", thrown.getMessage(),
            "Exception message does not match." );
    }

    @Test
    void mapWhenOrIsUsed()
    {
        // Given
        final Program program1 = new Program( "prg-1" );
        program1.setUid( "lxAQ7Zs9VYR" );
        final Program program2 = new Program( "prg-2" );
        program2.setUid( "ur1Edk5Oe2n" );

        final ProgramStage programStage1 = new ProgramStage( "ps-1", program1 );
        final I18nFormat anyI18nFormat = new I18nFormat();
        final String queryItem_1 = "EQ:john";
        final String queryItem_2 = "EQ:joe";
        final String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";

        final AnalyticsPagingCriteria theAnalyticsPagingCriteria = new AnalyticsPagingCriteria();
        final List<OrganisationUnit> organisationUnits = List.of( new OrganisationUnit( "org-1" ),
            new OrganisationUnit( "org-2" ) );
        final List<Program> programs = List.of( program1, program2 );

        final DimensionIdentifier<Program, ProgramStage, StringUid> deDimensionIdentifier = DimensionIdentifier.of(
            ElementWithOffset.of( program1, "1" ),
            ElementWithOffset.of( programStage1, "2" ),
            StringUid.of( "yLIPuJHRgey" ) );

        final BaseDimensionalObject dimensionalObject = new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(), DATA_X, null, DISPLAY_NAME_DATA_X,
            emptyList(), new DimensionItemKeywords() );

        final CommonQueryRequest aCommonQueryRequest = new CommonQueryRequest()
            .withUserOrgUnit( "PEZNsGbZaVJ" )
            .withProgram( Set.of( "ur1Edk5Oe2n", "lxAQ7Zs9VYR" ) )
            .withDimension(
                Set.of( dimension + ":" + queryItem_1 + DIMENSION_OR_SEPARATOR + dimension + ":" + queryItem_2 ) );

        when( dataQueryService.getUserOrgUnits( null, aCommonQueryRequest.getUserOrgUnit() ) )
            .thenReturn( organisationUnits );
        when( programService.getPrograms( aCommonQueryRequest.getProgram() ) ).thenReturn( programs );
        when( i18nManager.getI18nFormat() ).thenReturn( anyI18nFormat );
        when( dimensionIdentifierConverter.fromString( programs, dimension ) ).thenReturn( deDimensionIdentifier );

        Stream.of( queryItem_1, queryItem_2 )
            .forEach( s -> {
                when( (dataQueryService.getDimension( deDimensionIdentifier.getDimension().getUid(),
                    List.of( s ), aCommonQueryRequest.getRelativePeriodDate(), organisationUnits,
                    i18nManager.getI18nFormat(), true, UID )) ).thenReturn( dimensionalObject );
            } );

        // When
        final CommonParams params = new CommonQueryRequestMapper( i18nManager, dataQueryService, eventDataQueryService,
            programService, dimensionIdentifierConverter ).map( aCommonQueryRequest );

        // Then
        assertThat( params.getDimensionIdentifiers(), hasSize( 1 ) );
        assertThat( params.getDimensionIdentifiers().get( 0 ), hasSize( 2 ) );
    }
}
