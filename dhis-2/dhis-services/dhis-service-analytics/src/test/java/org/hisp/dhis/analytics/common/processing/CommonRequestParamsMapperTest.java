/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Arrays.asList;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.params.dimension.StringUid;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link CommonRequestParamsParser}. */
@ExtendWith(MockitoExtension.class)
class CommonRequestParamsMapperTest {
  @Mock SystemSettingsProvider settingsProvider;

  @Mock private DataQueryService dataQueryService;

  @Mock private EventDataQueryService eventDataQueryService;

  @Mock private ProgramService programService;

  @Mock private DimensionIdentifierConverter dimensionIdentifierConverter;

  @BeforeEach
  void setUp() {
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
  }

  @Test
  void mapWithSuccessOnlyDimension() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    String queryItem = "EQ:john";
    String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";

    List<OrganisationUnit> organisationUnits =
        List.of(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));
    List<Program> programs = asList(program1, program2);

    DimensionIdentifier<StringUid> deDimensionIdentifier =
        DimensionIdentifier.of(
            ElementWithOffset.of(program1, 1),
            ElementWithOffset.of(programStage1, 2),
            StringUid.of("yLIPuJHRgey"));

    DimensionalObject dimensionalObject =
        new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(),
            DATA_X,
            null,
            DISPLAY_NAME_DATA_X,
            emptyList(),
            new DimensionItemKeywords());

    CommonRequestParams aCommonRequestParams =
        new CommonRequestParams()
            .withUserOrgUnit("PEZNsGbZaVJ")
            .withProgram(new HashSet<>(asList("ur1Edk5Oe2n", "lxAQ7Zs9VYR")))
            .withDimension(new HashSet<>(asList(dimension + ":" + queryItem)));

    when(dataQueryService.getUserOrgUnits(null, aCommonRequestParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(aCommonRequestParams.getProgram())).thenReturn(programs);
    when(dimensionIdentifierConverter.fromString(programs, dimension))
        .thenReturn(deDimensionIdentifier);
    when((dataQueryService.getDimension(
            deDimensionIdentifier.getDimension().getUid(),
            asList(queryItem),
            aCommonRequestParams.getRelativePeriodDate(),
            organisationUnits,
            true,
            aCommonRequestParams.getDisplayProperty(),
            UID)))
        .thenReturn(dimensionalObject);

    // When
    CommonParsedParams params =
        new CommonRequestParamsParser(
                settingsProvider,
                dataQueryService,
                eventDataQueryService,
                programService,
                dimensionIdentifierConverter)
            .parse(aCommonRequestParams);

    // Then
    assertEquals(2, params.getPrograms().size(), "Should contain 2 programs.");
    assertEquals(
        "lxAQ7Zs9VYR",
        params.getPrograms().get(0).getUid(),
        "First program should be lxAQ7Zs9VYR.");
    assertEquals(
        "ur1Edk5Oe2n",
        params.getPrograms().get(1).getUid(),
        "Second program should be ur1Edk5Oe2n.");
    assertEquals(1, params.getDimensionIdentifiers().size(), "Should contain 1 identifier.");
    assertTrue(params.getDimensionIdentifiers().get(0).hasProgram(), "Should contain 1 program.");
    assertTrue(
        params.getDimensionIdentifiers().get(0).hasProgramStage(),
        "Should contain 1 program stage.");
    assertEquals(
        "yLIPuJHRgey",
        params
            .getDimensionIdentifiers()
            .get(0)
            .getDimension()
            .getDimensionalObject()
            .getDimension(),
        "Dimension identifier should be yLIPuJHRgey.");
    assertEquals(
        1,
        params.getDimensionIdentifiers().get(0).getProgram().getOffset(),
        "Program offset should be 1.");
    assertEquals(
        2,
        params.getDimensionIdentifiers().get(0).getProgramStage().getOffset(),
        "ProgramStage offset should be 2.");
    assertFalse(params.getPagingParams().isEmpty(), "Paging and sorting should not be empty.");
  }

  @Test
  void mapWithSuccessOnlyFilter() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    String queryItem = "ur1Edk5Oe2n.OU";
    String orgUnitUid = "PEZNsGbZaVJ";

    List<OrganisationUnit> organisationUnits =
        List.of(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));
    List<Program> programs = List.of(program1, program2);

    DimensionIdentifier<StringUid> deDimensionIdentifier =
        DimensionIdentifier.of(ElementWithOffset.of(program1, 1), null, StringUid.of(null));

    DimensionIdentifier<StringUid> ouDimensionIdentifier =
        DimensionIdentifier.of(
            ElementWithOffset.of(program1, 1),
            ElementWithOffset.of(programStage1, 2),
            StringUid.of(queryItem));

    DimensionalObject dimensionalObject =
        new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(),
            DATA_X,
            null,
            DISPLAY_NAME_DATA_X,
            emptyList(),
            new DimensionItemKeywords());

    CommonRequestParams aCommonRequestParams =
        new CommonRequestParams()
            .withUserOrgUnit(orgUnitUid)
            .withProgram(Set.of("lxAQ7Zs9VYR", "ur1Edk5Oe2n"))
            .withFilter(Set.of("ur1Edk5Oe2n.OU:PEZNsGbZaVJ"));

    when(dataQueryService.getUserOrgUnits(null, aCommonRequestParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(aCommonRequestParams.getProgram())).thenReturn(programs);
    when(dimensionIdentifierConverter.fromString(programs, queryItem))
        .thenReturn(ouDimensionIdentifier);
    when((dataQueryService.getDimension(
            ouDimensionIdentifier.getDimension().getUid(),
            List.of(orgUnitUid),
            aCommonRequestParams.getRelativePeriodDate(),
            organisationUnits,
            true,
            aCommonRequestParams.getDisplayProperty(),
            UID)))
        .thenReturn(dimensionalObject);

    // When
    CommonParsedParams params =
        new CommonRequestParamsParser(
                settingsProvider,
                dataQueryService,
                eventDataQueryService,
                programService,
                dimensionIdentifierConverter)
            .parse(aCommonRequestParams);

    // Then
    assertEquals(2, params.getPrograms().size(), "Should contain 2 programs.");
    assertEquals(
        "lxAQ7Zs9VYR",
        params.getPrograms().get(0).getUid(),
        "First program should be lxAQ7Zs9VYR.");
    assertEquals(
        "ur1Edk5Oe2n",
        params.getPrograms().get(1).getUid(),
        "Second program should be ur1Edk5Oe2n.");
    assertEquals(1, params.getDimensionIdentifiers().size(), "Should contain 1 identifier.");
    assertTrue(
        params.getDimensionIdentifiers().get(0).hasProgramStage(),
        "Should contain 1 program stage.");
    assertNull(
        params
            .getDimensionIdentifiers()
            .get(0)
            .getDimension()
            .getDimensionalObject()
            .getDimension(),
        "Dimension identifier should be yLIPuJHRgey.");
    assertEquals(
        1,
        params.getDimensionIdentifiers().get(0).getProgram().getOffset(),
        "Program offset should be 1.");
    assertEquals(
        2,
        params.getDimensionIdentifiers().get(0).getProgramStage().getOffset(),
        "ProgramStage offset should be 2.");
    assertFalse(params.getPagingParams().isEmpty(), "Paging and sorting should not be empty.");
  }

  @Test
  void mapWithSuccessDimensionAndFilter() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    String orgUnitDimension = "ur1Edk5Oe2n.OU";
    String queryItemFilter = "PEZNsGbZaVJ";
    String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";
    String queryItemDimension = "EQ:john";
    List<OrganisationUnit> organisationUnits =
        asList(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));
    List<Program> programs = asList(program1, program2);

    DimensionIdentifier<StringUid> deDimensionIdentifier =
        DimensionIdentifier.of(
            ElementWithOffset.of(program1, 1),
            ElementWithOffset.of(programStage1, 2),
            StringUid.of(queryItemDimension));

    DimensionIdentifier<StringUid> ouDimensionIdentifier =
        DimensionIdentifier.of(
            ElementWithOffset.of(program1, 1),
            ElementWithOffset.of(programStage1, 2),
            StringUid.of(orgUnitDimension));

    DimensionalObject dimensionalObject =
        new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(),
            DATA_X,
            null,
            DISPLAY_NAME_DATA_X,
            emptyList(),
            new DimensionItemKeywords());

    DimensionalObject orgUnitObject =
        new BaseDimensionalObject(
            ouDimensionIdentifier.getDimension().getUid(),
            ORGANISATION_UNIT,
            null,
            DISPLAY_NAME_ORGUNIT,
            organisationUnits,
            new DimensionItemKeywords());

    CommonRequestParams aCommonRequestParams =
        new CommonRequestParams()
            .withUserOrgUnit(queryItemFilter)
            .withProgram(new HashSet<>(asList("lxAQ7Zs9VYR", "ur1Edk5Oe2n")))
            .withDimension(new HashSet<>(asList(dimension + ":" + queryItemDimension)))
            .withFilter(new HashSet<>(asList("ur1Edk5Oe2n.OU:PEZNsGbZaVJ")));

    when(dataQueryService.getUserOrgUnits(null, aCommonRequestParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(aCommonRequestParams.getProgram())).thenReturn(programs);
    when(dimensionIdentifierConverter.fromString(programs, dimension))
        .thenReturn(deDimensionIdentifier);
    when((dataQueryService.getDimension(
            deDimensionIdentifier.getDimension().getUid(),
            asList(queryItemDimension),
            aCommonRequestParams.getRelativePeriodDate(),
            organisationUnits,
            true,
            aCommonRequestParams.getDisplayProperty(),
            UID)))
        .thenReturn(dimensionalObject);

    when(dimensionIdentifierConverter.fromString(programs, orgUnitDimension))
        .thenReturn(ouDimensionIdentifier);
    when((dataQueryService.getDimension(
            ouDimensionIdentifier.getDimension().getUid(),
            asList(queryItemFilter),
            aCommonRequestParams.getRelativePeriodDate(),
            organisationUnits,
            true,
            aCommonRequestParams.getDisplayProperty(),
            UID)))
        .thenReturn(orgUnitObject);

    // When
    CommonParsedParams params =
        new CommonRequestParamsParser(
                settingsProvider,
                dataQueryService,
                eventDataQueryService,
                programService,
                dimensionIdentifierConverter)
            .parse(aCommonRequestParams);

    // Then
    assertEquals(2, params.getPrograms().size(), "Should contain 2 programs.");
    assertEquals(
        "lxAQ7Zs9VYR",
        params.getPrograms().get(0).getUid(),
        "First program should be lxAQ7Zs9VYR.");
    assertEquals(
        "ur1Edk5Oe2n",
        params.getPrograms().get(1).getUid(),
        "Second program should be ur1Edk5Oe2n.");
    assertEquals(2, params.getDimensionIdentifiers().size(), "Should contain 2 identifiers.");
    assertTrue(params.getDimensionIdentifiers().get(0).hasProgram(), "Should contain 1 program.");
    assertTrue(
        params.getDimensionIdentifiers().get(0).hasProgramStage(),
        "Should contain 1 program stage.");
    assertEquals(
        "EQ:john",
        params
            .getDimensionIdentifiers()
            .get(0)
            .getDimension()
            .getDimensionalObject()
            .getDimension(),
        "Dimension identifier should be yLIPuJHRgey.");
    assertEquals(
        1,
        params.getDimensionIdentifiers().get(0).getProgram().getOffset(),
        "Program offset should be 1.");
    assertEquals(
        2,
        params.getDimensionIdentifiers().get(0).getProgramStage().getOffset(),
        "ProgramStage offset should be 2.");
    assertFalse(params.getPagingParams().isEmpty(), "Paging and sorting should not be empty.");
  }

  @Test
  void mapWhenProgramsCannotBeFound() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");

    String queryItem = "EQ:john";
    String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";

    List<OrganisationUnit> organisationUnits =
        List.of(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));

    CommonRequestParamsParser commonRequestParamsParser =
        new CommonRequestParamsParser(
            settingsProvider,
            dataQueryService,
            eventDataQueryService,
            programService,
            dimensionIdentifierConverter);

    // List has only one Program, but the CommonQueryRequest, has two
    List<Program> programs = List.of(program1);

    CommonRequestParams commonRequestParams =
        new CommonRequestParams()
            .withUserOrgUnit("PEZNsGbZaVJ")
            .withDimension(Set.of(dimension + ":" + queryItem))
            // Two programs, where "ur1Edk5Oe2n" does not exist.
            .withProgram(Set.of("lxAQ7Zs9VYR", "ur1Edk5Oe2n"));

    when(dataQueryService.getUserOrgUnits(null, commonRequestParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(commonRequestParams.getProgram())).thenReturn(programs);

    // When
    IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class,
            () -> commonRequestParamsParser.parse(commonRequestParams));

    // Then
    assertEquals(
        "Program is specified but does not exist: `[ur1Edk5Oe2n]`",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void mapWhenDimensionIsNotObject() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    String queryItem = "EQ:john";
    String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";
    QueryItem anyQueryItem = new QueryItem(new DataElement());

    List<OrganisationUnit> organisationUnits =
        asList(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));
    List<Program> programs = asList(program1, program2);

    DimensionIdentifier<StringUid> deDimensionIdentifier =
        DimensionIdentifier.of(
            ElementWithOffset.of(program1, 1),
            ElementWithOffset.of(programStage1, 2),
            StringUid.of("yLIPuJHRgey"));

    CommonRequestParams aCommonRequestParams =
        new CommonRequestParams()
            .withUserOrgUnit("PEZNsGbZaVJ")
            .withProgram(new HashSet<>(asList("lxAQ7Zs9VYR", "ur1Edk5Oe2n")))
            .withDimension(new HashSet<>(asList(dimension + ":" + queryItem)));

    when(dataQueryService.getUserOrgUnits(null, aCommonRequestParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(aCommonRequestParams.getProgram())).thenReturn(programs);
    when(dimensionIdentifierConverter.fromString(programs, dimension))
        .thenReturn(deDimensionIdentifier);
    when((dataQueryService.getDimension(
            deDimensionIdentifier.getDimension().getUid(),
            asList(queryItem),
            aCommonRequestParams.getRelativePeriodDate(),
            organisationUnits,
            true,
            aCommonRequestParams.getDisplayProperty(),
            UID)))
        .thenReturn(null);
    when(eventDataQueryService.getQueryItem(
            deDimensionIdentifier.getDimension().getUid(),
            deDimensionIdentifier.getProgram().getElement(),
            TRACKED_ENTITY_INSTANCE))
        .thenReturn(anyQueryItem);

    // When
    CommonParsedParams params =
        new CommonRequestParamsParser(
                settingsProvider,
                dataQueryService,
                eventDataQueryService,
                programService,
                dimensionIdentifierConverter)
            .parse(aCommonRequestParams);

    // Then
    assertEquals(2, params.getPrograms().size(), "Should contain 2 programs.");
    assertEquals(
        "lxAQ7Zs9VYR",
        params.getPrograms().get(0).getUid(),
        "First program should be lxAQ7Zs9VYR.");
    assertEquals(
        "ur1Edk5Oe2n",
        params.getPrograms().get(1).getUid(),
        "Second program should be ur1Edk5Oe2n.");
    assertEquals(1, params.getDimensionIdentifiers().size(), "Should contain 1 identifier.");
    assertTrue(params.getDimensionIdentifiers().get(0).hasProgram(), "Should contain 1 program.");
    assertTrue(
        params.getDimensionIdentifiers().get(0).hasProgramStage(),
        "Should contain 1 program stage.");
    assertNull(
        params.getDimensionIdentifiers().get(0).getDimension().getDimensionalObject(),
        "Dimensional object should be null.");
    assertEquals(
        1,
        params.getDimensionIdentifiers().get(0).getProgram().getOffset(),
        "Program offset should be 1.");
    assertEquals(
        2,
        params.getDimensionIdentifiers().get(0).getProgramStage().getOffset(),
        "ProgramStage offset should be 2.");
    assertFalse(params.getPagingParams().isEmpty(), "Paging and sorting should not be empty.");
  }

  @Test
  void mapWhenDimensionIsNotObjectAndProgramAndStageAreNotSet() {
    // Given
    String queryItem = "EQ:john";
    String nonFullyQualifiedDimension = "yLIPuJHRgey";

    List<OrganisationUnit> organisationUnits =
        List.of(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));
    List<Program> noPrograms = emptyList();

    DimensionIdentifier<StringUid> deDimensionIdentifier =
        DimensionIdentifier.of(
            null, // The null program
            null, // The null stage
            StringUid.of(nonFullyQualifiedDimension));

    CommonRequestParams commonParsedParams =
        new CommonRequestParams()
            .withUserOrgUnit("PEZNsGbZaVJ")
            .withDimension(new HashSet<>(asList(nonFullyQualifiedDimension + ":" + queryItem)));

    CommonRequestParamsParser commonRequestParamsParser =
        new CommonRequestParamsParser(
            settingsProvider,
            dataQueryService,
            eventDataQueryService,
            programService,
            dimensionIdentifierConverter);

    when(dataQueryService.getUserOrgUnits(null, commonParsedParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(commonParsedParams.getProgram())).thenReturn(noPrograms);
    when(dimensionIdentifierConverter.fromString(noPrograms, nonFullyQualifiedDimension))
        .thenReturn(deDimensionIdentifier);
    when((dataQueryService.getDimension(
            deDimensionIdentifier.getDimension().getUid(),
            asList(queryItem),
            commonParsedParams.getRelativePeriodDate(),
            organisationUnits,
            true,
            commonParsedParams.getDisplayProperty(),
            UID)))
        .thenReturn(null);

    // When
    IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class, () -> commonRequestParamsParser.parse(commonParsedParams));

    // Then
    assertEquals(
        "Dimension is not a fully qualified: `yLIPuJHRgey`",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void mapWhenOrIsUsed() {
    // Given
    Program program1 = new Program("prg-1");
    program1.setUid("lxAQ7Zs9VYR");
    Program program2 = new Program("prg-2");
    program2.setUid("ur1Edk5Oe2n");

    ProgramStage programStage1 = new ProgramStage("ps-1", program1);
    String queryItem1 = "EQ:john";
    String queryItem2 = "EQ:joe";
    String dimension = "ur1Edk5Oe2n[1].jdRD35YwbRH[y].yLIPuJHRgey";

    List<OrganisationUnit> organisationUnits =
        List.of(new OrganisationUnit("org-1"), new OrganisationUnit("org-2"));
    List<Program> programs = List.of(program1, program2);

    DimensionIdentifier<StringUid> deDimensionIdentifier =
        DimensionIdentifier.of(
            ElementWithOffset.of(program1, 1),
            ElementWithOffset.of(programStage1, 2),
            StringUid.of("yLIPuJHRgey"));

    DimensionalObject dimensionalObject =
        new BaseDimensionalObject(
            deDimensionIdentifier.getDimension().getUid(),
            DATA_X,
            null,
            DISPLAY_NAME_DATA_X,
            emptyList(),
            new DimensionItemKeywords());

    CommonRequestParams aCommonRequestParams =
        new CommonRequestParams()
            .withUserOrgUnit("PEZNsGbZaVJ")
            .withProgram(Set.of("ur1Edk5Oe2n", "lxAQ7Zs9VYR"))
            .withDimension(
                new HashSet<>(
                    asList(
                        dimension
                            + ":"
                            + queryItem1
                            + DIMENSION_OR_SEPARATOR
                            + dimension
                            + ":"
                            + queryItem2)));

    when(dataQueryService.getUserOrgUnits(null, aCommonRequestParams.getUserOrgUnit()))
        .thenReturn(organisationUnits);
    when(programService.getPrograms(aCommonRequestParams.getProgram())).thenReturn(programs);
    when(dimensionIdentifierConverter.fromString(programs, dimension))
        .thenReturn(deDimensionIdentifier);

    Stream.of(queryItem1, queryItem2)
        .forEach(
            s ->
                when((dataQueryService.getDimension(
                        deDimensionIdentifier.getDimension().getUid(),
                        asList(s),
                        aCommonRequestParams.getRelativePeriodDate(),
                        organisationUnits,
                        true,
                        aCommonRequestParams.getDisplayProperty(),
                        UID)))
                    .thenReturn(dimensionalObject));

    // When
    CommonParsedParams params =
        new CommonRequestParamsParser(
                settingsProvider,
                dataQueryService,
                eventDataQueryService,
                programService,
                dimensionIdentifierConverter)
            .parse(aCommonRequestParams);

    Set<String> groups =
        params.getDimensionIdentifiers().stream()
            .map(DimensionIdentifier::getGroupId)
            .collect(Collectors.toSet());
    // Then
    assertThat(groups, hasSize(2));
    assertThat(params.getDimensionIdentifiers(), hasSize(2));
  }
}
