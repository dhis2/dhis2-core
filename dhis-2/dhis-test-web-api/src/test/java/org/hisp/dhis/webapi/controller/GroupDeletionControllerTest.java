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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient.HttpResponse;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Deleting a group or group set removes the group and its memberships, never the member objects
 * (DHIS2-22154).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GroupDeletionControllerTest extends DhisControllerIntegrationTest {

  private static final String GROUP_A = "GroupUid001";

  private static final String GROUP_B = "GroupUid002";

  /**
   * @param groupType API path and metadata property of the group type
   * @param memberType API path of the member type, also the group property listing the members
   * @param memberUid UID of the single member every group of the fixture contains
   * @param prerequisites metadata fragment creating the member and everything it depends on
   * @param group group JSON, {@code %1$s} is replaced by the group UID
   */
  record GroupFixture(
      String groupType, String memberType, String memberUid, String prerequisites, String group) {

    String metadata(String... groupUids) {
      String groups = Stream.of(groupUids).map(group::formatted).collect(Collectors.joining(","));
      return "{%s,'%s':[%s]}".formatted(prerequisites, groupType, groups);
    }

    @Override
    public String toString() {
      return groupType;
    }
  }

  private static final String OPTION =
      "'optionSets':[{'id':'OptSetUid01','name':'OS1','valueType':'TEXT','options':[{'id':'OptionUid01'}]}],"
          + "'options':[{'id':'OptionUid01','name':'O1','code':'O1','optionSet':{'id':'OptSetUid01'},'sortOrder':1}]";

  private static final String INDICATOR =
      "'indicatorTypes':[{'id':'IndTypeUi01','name':'IT1','factor':1}],"
          + "'indicators':[{'id':'IndicatorU1','name':'I1','shortName':'I1','indicatorType':{'id':'IndTypeUi01'},'numerator':'1','denominator':'1'}]";

  private static final String DATA_ELEMENT =
      "'dataElements':[{'id':'DataElemU01','name':'DE1','shortName':'DE1','valueType':'NUMBER','aggregationType':'SUM','domainType':'AGGREGATE'}]";

  private static final String CATEGORY_OPTION =
      "'categoryOptions':[{'id':'CatOptionU1','name':'CO1','shortName':'CO1'}]";

  private static final String ORG_UNIT =
      "'organisationUnits':[{'id':'OrgUnitUi01','name':'OU1','shortName':'OU1','openingDate':'2020-01-01'}]";

  static Stream<GroupFixture> groupTypes() {
    return Stream.of(
        new GroupFixture(
            "optionGroups",
            "options",
            "OptionUid01",
            OPTION,
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','optionSet':{'id':'OptSetUid01'},'options':[{'id':'OptionUid01'}]}"),
        new GroupFixture(
            "optionGroupSets",
            "optionGroups",
            "OptGroupU01",
            OPTION
                + ",'optionGroups':[{'id':'OptGroupU01','name':'OG1','shortName':'OG1','optionSet':{'id':'OptSetUid01'},'options':[{'id':'OptionUid01'}]}]",
            "{'id':'%1$s','name':'G%1$s','optionSet':{'id':'OptSetUid01'},'dataDimension':false,'optionGroups':[{'id':'OptGroupU01'}]}"),
        new GroupFixture(
            "indicatorGroups",
            "indicators",
            "IndicatorU1",
            INDICATOR,
            "{'id':'%1$s','name':'G%1$s','indicators':[{'id':'IndicatorU1'}]}"),
        new GroupFixture(
            "indicatorGroupSets",
            "indicatorGroups",
            "IndGroupU01",
            INDICATOR
                + ",'indicatorGroups':[{'id':'IndGroupU01','name':'IG1','indicators':[{'id':'IndicatorU1'}]}]",
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','indicatorGroups':[{'id':'IndGroupU01'}]}"),
        new GroupFixture(
            "dataElementGroups",
            "dataElements",
            "DataElemU01",
            DATA_ELEMENT,
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','dataElements':[{'id':'DataElemU01'}]}"),
        new GroupFixture(
            "dataElementGroupSets",
            "dataElementGroups",
            "DeGroupUi01",
            DATA_ELEMENT
                + ",'dataElementGroups':[{'id':'DeGroupUi01','name':'DEG1','shortName':'DEG1','dataElements':[{'id':'DataElemU01'}]}]",
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','dataElementGroups':[{'id':'DeGroupUi01'}]}"),
        new GroupFixture(
            "categoryOptionGroups",
            "categoryOptions",
            "CatOptionU1",
            CATEGORY_OPTION,
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','dataDimensionType':'DISAGGREGATION','categoryOptions':[{'id':'CatOptionU1'}]}"),
        new GroupFixture(
            "categoryOptionGroupSets",
            "categoryOptionGroups",
            "CoGroupUi01",
            CATEGORY_OPTION
                + ",'categoryOptionGroups':[{'id':'CoGroupUi01','name':'COG1','shortName':'COG1','dataDimensionType':'DISAGGREGATION','categoryOptions':[{'id':'CatOptionU1'}]}]",
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','dataDimensionType':'DISAGGREGATION','categoryOptionGroups':[{'id':'CoGroupUi01'}]}"),
        new GroupFixture(
            "organisationUnitGroups",
            "organisationUnits",
            "OrgUnitUi01",
            ORG_UNIT,
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','organisationUnits':[{'id':'OrgUnitUi01'}]}"),
        new GroupFixture(
            "organisationUnitGroupSets",
            "organisationUnitGroups",
            "OuGroupUi01",
            ORG_UNIT
                + ",'organisationUnitGroups':[{'id':'OuGroupUi01','name':'OUG1','shortName':'OUG1','organisationUnits':[{'id':'OrgUnitUi01'}]}]",
            "{'id':'%1$s','name':'G%1$s','shortName':'G%1$s','organisationUnitGroups':[{'id':'OuGroupUi01'}]}"),
        new GroupFixture(
            "programIndicatorGroups",
            "programIndicators",
            "ProgIndUi01",
            // creating an event program also creates its enrollment in a root org unit
            ORG_UNIT
                + ",'programs':[{'id':'ProgramUi01','name':'P1','shortName':'P1','programType':'WITHOUT_REGISTRATION'}],"
                + "'programIndicators':[{'id':'ProgIndUi01','name':'PI1','shortName':'PI1','program':{'id':'ProgramUi01'},'expression':'1'}]",
            "{'id':'%1$s','name':'G%1$s','programIndicators':[{'id':'ProgIndUi01'}]}"),
        new GroupFixture(
            "validationRuleGroups",
            "validationRules",
            "ValRuleUi01",
            "'validationRules':[{'id':'ValRuleUi01','name':'VR1','operator':'equal_to','periodType':'Monthly','importance':'MEDIUM',"
                + "'leftSide':{'expression':'1','missingValueStrategy':'NEVER_SKIP'},'rightSide':{'expression':'1','missingValueStrategy':'NEVER_SKIP'}}]",
            "{'id':'%1$s','name':'G%1$s','validationRules':[{'id':'ValRuleUi01'}]}"),
        new GroupFixture(
            "predictorGroups",
            "predictors",
            "PredictorU1",
            "'dataElements':[{'id':'DataElemU02','name':'DE2','shortName':'DE2','valueType':'NUMBER','aggregationType':'SUM','domainType':'AGGREGATE'}],"
                + "'predictors':[{'id':'PredictorU1','name':'PR1','shortName':'PR1','organisationUnitDescendants':'SELECTED','output':{'id':'DataElemU02'},'periodType':'Monthly','generator':{'expression':'1'},'sequentialSampleCount':0,'annualSampleCount':0,'organisationUnitLevels':[]}]",
            "{'id':'%1$s','name':'G%1$s','predictors':[{'id':'PredictorU1'}]}"),
        new GroupFixture(
            "userGroups",
            "users",
            "UserUidU001",
            "'userRoles':[{'id':'UserRoleU01','name':'UR1','authorities':[]}],"
                + "'users':[{'id':'UserUidU001','username':'groupmember1','firstName':'Group','surname':'Member','password':'Pass123456!','userRoles':[{'id':'UserRoleU01'}]}]",
            "{'id':'%1$s','name':'G%1$s','users':[{'id':'UserUidU001'}]}"));
  }

  /** Group sets limit a group to one set of the type, so only groups can share a member. */
  static Stream<GroupFixture> groupTypesAllowingSharedMembers() {
    return groupTypes().filter(fixture -> !fixture.groupType().endsWith("Sets"));
  }

  @ParameterizedTest
  @MethodSource("groupTypes")
  void deletingGroupKeepsItsMembers(GroupFixture fixture) {
    importMetadata(fixture.metadata(GROUP_A));

    assertStatus(HttpStatus.OK, DELETE("/" + fixture.groupType() + "/" + GROUP_A));

    assertStatus(HttpStatus.NOT_FOUND, GET("/" + fixture.groupType() + "/" + GROUP_A));
    assertStatus(HttpStatus.OK, GET("/" + fixture.memberType() + "/" + fixture.memberUid()));
  }

  @ParameterizedTest
  @MethodSource("groupTypesAllowingSharedMembers")
  void deletingGroupKeepsMemberSharedWithAnotherGroup(GroupFixture fixture) {
    importMetadata(fixture.metadata(GROUP_A, GROUP_B));

    assertStatus(HttpStatus.OK, DELETE("/" + fixture.groupType() + "/" + GROUP_A));

    JsonArray members =
        GET("/" + fixture.groupType() + "/" + GROUP_B + "?fields=" + fixture.memberType() + "[id]")
            .content(HttpStatus.OK)
            .getArray(fixture.memberType());
    assertEquals(1, members.size());
    assertEquals(fixture.memberUid(), members.getObject(0).getString("id").string());
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }
}
