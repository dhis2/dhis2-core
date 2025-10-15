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
package org.hisp.dhis.analytics.data.handler;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.OutputFormat.ANALYTICS;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.common.IdScheme.ID;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.common.IdScheme.UUID;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Data;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class SchemeIdResponseMapperTest {
  private final SchemeIdResponseMapper schemeIdResponseMapper = new SchemeIdResponseMapper();

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToName() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(ANALYTICS).outputIdScheme(NAME).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getName())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getName())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getName())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getName())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getName())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getName())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToCode() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(ANALYTICS).outputIdScheme(CODE).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getCode())));
    assertThat(responseMap.get(periodIsoDate), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getCode())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToUuid() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(createOrganisationUnit('A')))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(ANALYTICS).outputIdScheme(UUID).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(emptyOrNullString()));
    assertThat(responseMap.get(periodIsoDate), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsSetToUid() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(ANALYTICS).outputIdScheme(UUID).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(emptyOrNullString()));
    assertThat(responseMap.get(periodIsoDate), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToName() {
    List<DataElement> dataElements = stubDataElements();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElements(List.of(dataElements.get(0), dataElements.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputDataElementIdScheme(NAME).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElements.get(0);
    DataElement dataElementB = dataElements.get(1);

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getUid())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getName())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getName())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToCode() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputDataElementIdScheme(CODE).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getUid())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getCode())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToUuid() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputDataElementIdScheme(UUID).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getUid())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeIsSetToUid() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    DataElement dataElement = stubDataElement();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit, dataElement))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputDataElementIdScheme(UID).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getUid())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElement.getUid()), is(dataElement.getUid()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToName() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputOrgUnitIdScheme(NAME).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getName())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToCode() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputOrgUnitIdScheme(CODE).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getCode())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToUuid() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputOrgUnitIdScheme(UUID).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(emptyOrNullString()));
    assertThat(responseMap.get(periodIsoDate), is(period.getUid()));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeIsSetToUid() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder().outputFormat(DATA_VALUE_SET).outputOrgUnitIdScheme(UID).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getUid())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getUid())));
    assertThat(responseMap.get(dataElementA.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(dataElementB.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
    assertThat(responseMap.get(categoryOptionComboC.getUid()), is(emptyOrNullString()));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputOrgUnitIdSchemeOverridesOutputIdScheme() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period))
            .build();

    Settings schemeSettings =
        Settings.builder()
            .outputFormat(DATA_VALUE_SET)
            .outputIdScheme(NAME)
            .outputOrgUnitIdScheme(CODE)
            .build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getCode())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getName())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getName())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getName())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getName())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getName())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataElementIdSchemeOverridesOutputOrgUnitIdScheme() {
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings =
        Settings.builder()
            .outputFormat(DATA_VALUE_SET)
            .outputIdScheme(NAME)
            .outputDataElementIdScheme(CODE)
            .build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getName())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getName())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getCode())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataElementOrgUnitIdSchemeOverrideOutputIdScheme() {
    DataElement dataElement = stubDataElement();
    Indicator indicator = stubIndicator();
    ProgramIndicator programIndicator = stubProgramIndicator();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();

    Data schemeData =
        Data.builder()
            .dataElements(List.of(dataElement))
            .indicators(List.of(indicator))
            .programIndicators(List.of(programIndicator))
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period))
            .build();

    Settings schemeSettings =
        Settings.builder()
            .outputFormat(DATA_VALUE_SET)
            .outputIdScheme(NAME)
            .outputDataElementIdScheme(CODE)
            .outputOrgUnitIdScheme(ID)
            .build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();
    CategoryOptionCombo categoryOptionComboC = dataElementOperands.get(0).getCategoryOptionCombo();

    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getName())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getCode())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
    assertThat(
        responseMap.get(categoryOptionComboC.getUid()),
        is(equalTo(categoryOptionComboC.getCode())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputDataItemIdSchemeOverridesOutputIdScheme() {
    DataElement dataElement = stubDataElement();
    Indicator indicator = stubIndicator();
    ProgramIndicator programIndicator = stubProgramIndicator();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .dataElements(List.of(dataElement))
            .indicators(List.of(indicator))
            .programIndicators(List.of(programIndicator))
            .organizationUnits(List.of(organisationUnit))
            .dimensionalItemObjects(Set.of(period, organisationUnit, dataElement, indicator))
            .build();

    Settings schemeSettings =
        Settings.builder()
            .outputFormat(ANALYTICS)
            .outputIdScheme(NAME)
            .outputDataItemIdScheme(CODE)
            .build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    assertThat(responseMap.get(organisationUnit.getUid()), is(equalTo(organisationUnit.getName())));
    assertThat(responseMap.get(period.getIsoDate()), is(equalTo(period.getName())));
    assertThat(responseMap.get(dataElement.getUid()), is(equalTo(dataElement.getCode())));
    assertThat(responseMap.get(indicator.getUid()), is(equalTo(indicator.getCode())));
    assertThat(responseMap.get(programIndicator.getUid()), is(equalTo(programIndicator.getCode())));
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsNameInCommonParams() {
    Program program = createProgram('A');
    program.setName("Name");
    program.setCode("Code");

    SchemeInfo schemeInfo = new SchemeInfo(stubSchemeSettings(NAME), stubSchemeData(program));

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    assertEquals(responseMap.get(program.getUid()), program.getName());
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsCodeInCommonParams() {
    Program program = createProgram('A');
    program.setName("Name");
    program.setCode("Code");

    SchemeInfo schemeInfo = new SchemeInfo(stubSchemeSettings(CODE), stubSchemeData(program));

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    assertEquals(responseMap.get(program.getUid()), program.getCode());
  }

  @Test
  void testGetSchemeIdResponseMapWhenOutputIdSchemeIsUidInCommonParams() {
    Program program = createProgram('A');
    program.setName("Name");
    program.setCode("Code");

    SchemeInfo schemeInfo = new SchemeInfo(stubSchemeSettings(UID), stubSchemeData(program));

    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    // Uid is the default, so conversion is not needed. The map will not contain any conversion on
    // this case.
    assertNull(responseMap.get(program.getUid()));
  }

  @Test
  void testApplyOptionAndLegendSetMappingWhenHasOptionSetAndSchemeName() {
    // Given
    Option option = new Option("name", "code");
    option.setUid("uid");

    OptionSet optionSet = new OptionSet();
    optionSet.addOption(option);

    GridHeader gridHeader = new GridHeader("header", "column", TEXT, false, false, optionSet, null);

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addMetaData("code", "value");
    grid.addRow();
    grid.addValue("code");
    grid.addValue(11);

    // When
    schemeIdResponseMapper.applyOptionAndLegendSetMapping(NAME, grid);

    // Then
    assertEquals("name", grid.getRow(0).get(0));
  }

  @Test
  void testApplyOptionAndLegendSetMappingWhenHasOptionSetAndSchemeCode() {
    // Given
    Option option = new Option("name", "code");
    option.setUid("uid");

    OptionSet optionSet = new OptionSet();
    optionSet.addOption(option);

    GridHeader gridHeader = new GridHeader("header", "column", TEXT, false, false, optionSet, null);

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addMetaData("code", "value");
    grid.addRow();
    grid.addValue("code");
    grid.addValue(11);

    // When
    schemeIdResponseMapper.applyOptionAndLegendSetMapping(CODE, grid);

    // Then
    assertEquals("code", grid.getRow(0).get(0));
  }

  @Test
  void testApplyOptionAndLegendSetMappingWhenHasOptionSetAndSchemeUid() {
    // Given
    Option option = new Option("name", "code");
    option.setUid("uid");

    OptionSet optionSet = new OptionSet();
    optionSet.addOption(option);

    GridHeader gridHeader = new GridHeader("header", "column", TEXT, false, false, optionSet, null);

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addMetaData("code", "value");
    grid.addRow();
    grid.addValue("code");
    grid.addValue(11);

    // When
    schemeIdResponseMapper.applyOptionAndLegendSetMapping(UID, grid);

    // Then
    assertEquals("uid", grid.getRow(0).get(0));
  }

  @Test
  void testApplyOptionAndLegendSetMappingWhenHasLegendSetAndSchemeName() {
    // Given
    Legend legend = new Legend();
    legend.setUid("uid");
    legend.setCode("code");
    legend.setName("name");

    LegendSet legendSet = new LegendSet();
    legendSet.getLegends().add(legend);

    GridHeader gridHeader = new GridHeader("header", "column", TEXT, false, false, null, legendSet);

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addMetaData("uid", "value");
    grid.addRow();
    grid.addValue("uid");
    grid.addValue(11);

    // When
    schemeIdResponseMapper.applyOptionAndLegendSetMapping(NAME, grid);

    // Then
    assertEquals("name", grid.getRow(0).get(0));
  }

  @Test
  void testApplyOptionAndLegendSetMappingWhenHasLegendSetAndSchemeCode() {
    // Given
    Legend legend = new Legend();
    legend.setUid("uid");
    legend.setCode("code");
    legend.setName("name");

    LegendSet legendSet = new LegendSet();
    legendSet.getLegends().add(legend);

    GridHeader gridHeader = new GridHeader("header", "column", TEXT, false, false, null, legendSet);

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addMetaData("uid", "value");
    grid.addRow();
    grid.addValue("uid");
    grid.addValue(11);

    // When
    schemeIdResponseMapper.applyOptionAndLegendSetMapping(CODE, grid);

    // Then
    assertEquals("code", grid.getRow(0).get(0));
  }

  @Test
  void testApplyOptionAndLegendSetMappingWhenHasLegendSetAndSchemeUid() {
    // Given
    Legend legend = new Legend();
    legend.setUid("uid");
    legend.setCode("code");
    legend.setName("name");

    LegendSet legendSet = new LegendSet();
    legendSet.getLegends().add(legend);

    GridHeader gridHeader = new GridHeader("header", "column", TEXT, false, false, null, legendSet);

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addMetaData("uid", "value");
    grid.addRow();
    grid.addValue("uid");
    grid.addValue(11);

    // When
    schemeIdResponseMapper.applyOptionAndLegendSetMapping(UID, grid);

    // Then
    assertEquals("uid", grid.getRow(0).get(0));
  }

  @Test
  void testApplyOptionSetMappingWhenDataIdSchemeIsName() {
    // Given
    List<DataElementOperand> dataElementOperands = stubDataElementOperands();
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    PeriodDimension period = stubPeriod();

    Data schemeData =
        Data.builder()
            .organizationUnits(List.of(organisationUnit))
            .dataElementOperands(List.of(dataElementOperands.get(0), dataElementOperands.get(1)))
            .dimensionalItemObjects(Set.of(period, organisationUnit))
            .build();

    Settings schemeSettings = Settings.builder().outputFormat(ANALYTICS).dataIdScheme(NAME).build();

    SchemeInfo schemeInfo = new SchemeInfo(schemeSettings, schemeData);

    // When
    Map<String, String> responseMap = schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo);

    // Then
    String orgUnitUid = organisationUnit.getUid();
    String periodIsoDate = period.getIsoDate();
    DataElement dataElementA = dataElementOperands.get(0).getDataElement();
    DataElement dataElementB = dataElementOperands.get(1).getDataElement();

    assertThat(responseMap.get(orgUnitUid), is(equalTo(organisationUnit.getName())));
    assertThat(responseMap.get(periodIsoDate), is(equalTo(period.getName())));
    assertThat(responseMap.get(dataElementA.getUid()), is(equalTo(dataElementA.getName())));
    assertThat(responseMap.get(dataElementB.getUid()), is(equalTo(dataElementB.getName())));
  }

  private Settings stubSchemeSettings(IdScheme idScheme) {
    return Settings.builder().outputIdScheme(idScheme).build();
  }

  private Data stubSchemeData(Program program) {
    return Data.builder()
        .programs(List.of(program))
        .dimensionalItemObjects(
            Set.of(stubPeriod(), createOrganisationUnit('A'), stubDataElement()))
        .build();
  }

  private PeriodDimension stubPeriod() {
    PeriodDimension period = PeriodDimension.of(getPeriodFromIsoString("202010"));
    period.setUid("pe");
    period.setName("October 2020");

    return period;
  }

  private List<DataElementOperand> stubDataElementOperands() {
    DataElement dataElementA = new DataElement("NameA");
    dataElementA.setUid("uid1234567A");
    dataElementA.setCode("CodeA");

    DataElement dataElementB = new DataElement("NameB");
    dataElementB.setUid("uid1234567B");
    dataElementB.setCode("CodeB");

    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setName("NameC");
    categoryOptionCombo.setUid("uid1234567C");
    categoryOptionCombo.setCode("CodeC");

    DataElementOperand dataElementOperandA =
        new DataElementOperand(dataElementA, categoryOptionCombo);
    DataElementOperand dataElementOperandB =
        new DataElementOperand(dataElementB, categoryOptionCombo);

    new BaseDimensionalItemObject();

    return newArrayList(dataElementOperandA, dataElementOperandB);
  }

  private List<DataElement> stubDataElements() {
    DataElement dataElementA = new DataElement("NameA");
    dataElementA.setUid("uid1234567A");
    dataElementA.setCode("CodeA");

    DataElement dataElementB = new DataElement("NameB");
    dataElementB.setUid("uid1234567B");
    dataElementB.setCode("CodeB");

    return newArrayList(dataElementA, dataElementB);
  }

  private DataElement stubDataElement() {
    DataElement dataElementA = new DataElement();
    dataElementA.setUid("fM8kR4FOTR6");
    dataElementA.setName("DataElementNameA");
    dataElementA.setCode("DataElementCodeA");

    return dataElementA;
  }

  private Indicator stubIndicator() {
    Indicator indicatorA = new Indicator();
    indicatorA.setUid("SN78k0lNyvd");
    indicatorA.setName("IndicatorNameA");
    indicatorA.setCode("IndicatorCodeA");

    return indicatorA;
  }

  private ProgramIndicator stubProgramIndicator() {
    ProgramIndicator programIndicatorA = new ProgramIndicator();
    programIndicatorA.setUid("fCGiVvsXbkY");
    programIndicatorA.setName("ProgramIndicatorNameA");
    programIndicatorA.setCode("ProgramIndicatorCodeA");

    return programIndicatorA;
  }
}
