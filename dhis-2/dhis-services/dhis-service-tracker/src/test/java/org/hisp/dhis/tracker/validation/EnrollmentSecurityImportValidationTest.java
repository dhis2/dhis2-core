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
package org.hisp.dhis.tracker.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hisp.dhis.security.acl.AccessStringHelper.Permission.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EnrollmentSecurityImportValidationTest extends AbstractImportValidationTest {
  //
  @Autowired protected TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private RenderService _renderService;

  @Autowired private UserService _userService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private ProgramStage programStageA;

  private ProgramStage programStageB;

  private TrackedEntityType trackedEntityType;

  private void setupMetadata() {
    User admin = userService.getUser(ADMIN_USER_UID);
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B');
    manager.save(organisationUnitA);
    manager.save(organisationUnitB);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementA.setValueType(ValueType.INTEGER);
    dataElementB.setValueType(ValueType.INTEGER);
    manager.save(dataElementA);
    manager.save(dataElementB);
    programStageA = createProgramStage('A', 0);
    programStageB = createProgramStage('B', 0);
    programStageB.setRepeatable(true);
    manager.save(programStageA);
    manager.save(programStageB);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.getSharing().setOwner(admin);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.getSharing().setOwner(admin);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    TrackedEntityType trackedEntityTypeFromProgram = createTrackedEntityType('C');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeFromProgram);
    manager.save(programA);
    ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementA);
    programStageDataElement.setProgramStage(programStageA);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageA.getProgramStageDataElements().add(programStageDataElement);
    programStageA.setProgram(programA);
    programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementB);
    programStageDataElement.setProgramStage(programStageB);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageB.getProgramStageDataElements().add(programStageDataElement);
    programStageB.setProgram(programA);
    programStageB.setMinDaysFromStart(2);
    programA.getProgramStages().add(programStageA);
    programA.getProgramStages().add(programStageB);
    manager.update(programStageA);
    manager.update(programStageB);
    manager.update(programA);
    maleA = createTrackedEntityInstance('A', organisationUnitA);
    maleB = createTrackedEntityInstance(organisationUnitB);
    femaleA = createTrackedEntityInstance(organisationUnitA);
    femaleB = createTrackedEntityInstance(organisationUnitB);
    maleA.setTrackedEntityType(trackedEntityType);
    maleB.setTrackedEntityType(trackedEntityType);
    femaleA.setTrackedEntityType(trackedEntityType);
    femaleB.setTrackedEntityType(trackedEntityType);
    manager.save(maleA);
    manager.save(maleB);
    manager.save(femaleA);
    manager.save(femaleB);
    manager.flush();
  }

  @Override
  protected void setUpTest() throws IOException {
    renderService = _renderService;
    userService = _userService;
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("tracker/tracker_basic_metadata.json").getInputStream(),
            RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    ObjectBundleValidationReport validationReport = objectBundleValidationService.validate(bundle);
    assertFalse(validationReport.hasErrorReports());
    ObjectBundleCommitReport commit = objectBundleService.commit(bundle);
    assertFalse(commit.hasErrorReports());
    TrackerImportParams trackerBundleParams =
        createBundleFromJson("tracker/validations/enrollments_te_te-data.json");
    User admin = userService.getUser(ADMIN_USER_UID);
    trackerBundleParams.setUserId(admin.getUid());
    injectSecurityContext(admin);
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerBundleParams);
    assertEquals(0, trackerImportReport.getValidationReport().getErrors().size());
    assertEquals(TrackerStatus.OK, trackerImportReport.getStatus());
  }

  @Test
  void testNoWriteAccessToOrg() throws IOException {
    TrackerImportParams params =
        createBundleFromJson("tracker/validations/enrollments_te_enrollments-data.json");
    User user = userService.getUser(USER_2);
    injectSecurityContext(user);
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertEquals(4, trackerImportReport.getValidationReport().getErrors().size());
    assertThat(
        trackerImportReport.getValidationReport().getErrors(),
        hasItem(hasProperty("errorCode", equalTo(TrackerErrorCode.E1000))));
  }

  @Test
  void testUserNoAccessToTrackedEntity() throws IOException {
    clearSecurityContext();

    setupMetadata();
    programA.setPublicAccess(
        AccessStringHelper.newInstance().enable(READ).enable(DATA_WRITE).build());
    TrackedEntityType bPJ0FMtcnEh = trackedEntityTypeService.getTrackedEntityType("bPJ0FMtcnEh");
    programA.setTrackedEntityType(bPJ0FMtcnEh);
    manager.update(programA);
    User user = createUser("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    userService.addUser(user);
    injectSecurityContext(user);
    TrackerImportParams params =
        createBundleFromJson("tracker/validations/enrollments_no-access-tei.json");
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertEquals(1, trackerImportReport.getValidationReport().getErrors().size());
    assertThat(
        trackerImportReport.getValidationReport().getErrors(),
        hasItem(hasProperty("errorCode", equalTo(TrackerErrorCode.E1104))));
  }

  @Test
  void testUserNoWriteAccessToProgram() throws IOException {
    clearSecurityContext();

    setupMetadata();
    programA.setPublicAccess(
        AccessStringHelper.newInstance().enable(READ).enable(DATA_READ).build());
    trackedEntityType.setPublicAccess(AccessStringHelper.DATA_READ);
    programA.setTrackedEntityType(trackedEntityType);
    manager.updateNoAcl(programA);
    User user = createUser("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    userService.addUser(user);
    injectSecurityContext(user);
    TrackerImportParams params =
        createBundleFromJson("tracker/validations/enrollments_no-access-program.json");
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertEquals(1, trackerImportReport.getValidationReport().getErrors().size());
    assertThat(
        trackerImportReport.getValidationReport().getErrors(),
        hasItem(hasProperty("errorCode", equalTo(TrackerErrorCode.E1091))));
  }

  @Test
  void testUserHasWriteAccessToProgram() throws IOException {
    clearSecurityContext();

    setupMetadata();
    programA.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityType.setPublicAccess(AccessStringHelper.DATA_READ);
    programA.setTrackedEntityType(trackedEntityType);
    manager.updateNoAcl(programA);
    User user = createUser("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    userService.addUser(user);
    injectSecurityContext(user);
    TrackerImportParams params =
        createBundleFromJson("tracker/validations/enrollments_no-access-program.json");
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertEquals(0, trackerImportReport.getValidationReport().getErrors().size());
  }

  @Test
  void testUserHasNoAccessToProgramTeiType() throws IOException {
    clearSecurityContext();

    setupMetadata();
    programA.setPublicAccess(
        AccessStringHelper.newInstance().enable(READ).enable(DATA_WRITE).build());
    programA.setTrackedEntityType(trackedEntityType);
    manager.update(programA);
    manager.flush();
    User user = createUser("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    injectSecurityContext(user);
    TrackerImportParams params =
        createBundleFromJson("tracker/validations/enrollments_program-teitype-missmatch.json");
    params.setUser(user);
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    TrackerImportReport trackerImportReport = trackerImportService.importTracker(params);
    assertEquals(1, trackerImportReport.getValidationReport().getErrors().size());
    assertThat(
        trackerImportReport.getValidationReport().getErrors(),
        hasItem(hasProperty("errorCode", equalTo(TrackerErrorCode.E1104))));
  }

  @Override
  public boolean emptyDatabaseAfterTest() {
    return true;
  }
}
