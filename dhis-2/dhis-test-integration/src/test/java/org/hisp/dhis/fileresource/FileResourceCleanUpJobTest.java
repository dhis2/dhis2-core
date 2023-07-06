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
package org.hisp.dhis.fileresource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Wærstad
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class FileResourceCleanUpJobTest extends IntegrationTestBase {

  private FileResourceCleanUpJob cleanUpJob;

  @Autowired private SystemSettingManager systemSettingManager;

  @Autowired private FileResourceService fileResourceService;

  @Autowired private DataValueAuditService dataValueAuditService;

  /** We use the store directly to backdate audit entries what is usually not possible */
  @Autowired private DataValueAuditStore dataValueAuditStore;

  @Autowired private DataValueService dataValueService;

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private PeriodService periodService;

  @Autowired private ExternalFileResourceService externalFileResourceService;

  @Autowired private UserService _userService;

  @Mock private FileResourceContentStore fileResourceContentStore;

  private DataValue dataValueA;

  private DataValue dataValueB;

  private byte[] content;

  private Period period;

  @BeforeEach
  public void init() {
    userService = _userService;

    cleanUpJob =
        new FileResourceCleanUpJob(
            fileResourceService, systemSettingManager, fileResourceContentStore);

    period = createPeriod(PeriodType.getPeriodTypeByName("Monthly"), new Date(), new Date());
    periodService.addPeriod(period);
  }

  @Test
  void testNoRetention() {
    when(fileResourceContentStore.fileResourceContentExists(any(String.class))).thenReturn(true);

    systemSettingManager.saveSystemSetting(
        SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.NONE);

    content = "filecontentA".getBytes();
    dataValueA = createFileResourceDataValue('A', content);
    assertNotNull(fileResourceService.getFileResource(dataValueA.getValue()));

    dataValueService.deleteDataValue(dataValueA);

    cleanUpJob.execute(null, NoopJobProgress.INSTANCE);

    assertNull(fileResourceService.getFileResource(dataValueA.getValue()));
  }

  @Test
  void testRetention() {
    when(fileResourceContentStore.fileResourceContentExists(any(String.class))).thenReturn(true);

    systemSettingManager.saveSystemSetting(
        SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.THREE_MONTHS);

    content = "filecontentA".getBytes(StandardCharsets.UTF_8);
    dataValueA = createFileResourceDataValue('A', content);
    assertNotNull(fileResourceService.getFileResource(dataValueA.getValue()));

    content = "filecontentB".getBytes(StandardCharsets.UTF_8);
    dataValueB = createFileResourceDataValue('B', content);
    assertNotNull(fileResourceService.getFileResource(dataValueB.getValue()));

    content = "fileResourceC".getBytes(StandardCharsets.UTF_8);
    FileResource fileResource = createFileResource('C', content);
    dataValueB.setValue(fileResource.getUid());
    dataValueService.updateDataValue(dataValueB);
    fileResource.setAssigned(true);

    DataValueAudit audit = dataValueAuditService.getDataValueAudits(dataValueB).get(0);
    audit.setCreated(getDate(2000, 1, 1));
    dataValueAuditStore.updateDataValueAudit(audit);

    cleanUpJob.execute(null, NoopJobProgress.INSTANCE);

    assertNotNull(fileResourceService.getFileResource(dataValueA.getValue()));
    assertTrue(fileResourceService.getFileResource(dataValueA.getValue()).isAssigned());
    assertNull(
        dataValueService.getDataValue(
            dataValueA.getDataElement(), dataValueA.getPeriod(), dataValueA.getSource(), null));
    assertNull(fileResourceService.getFileResource(dataValueB.getValue()));
  }

  @Test
  void testOrphan() {
    when(fileResourceContentStore.fileResourceContentExists(any(String.class))).thenReturn(false);

    systemSettingManager.saveSystemSetting(
        SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.NONE);

    content = "filecontentA".getBytes(StandardCharsets.UTF_8);
    FileResource fileResourceA = createFileResource('A', content);
    fileResourceA.setCreated(DateTime.now().minus(Days.ONE).toDate());
    String uidA = fileResourceService.saveFileResource(fileResourceA, content);

    content = "filecontentB".getBytes(StandardCharsets.UTF_8);
    FileResource fileResourceB = createFileResource('A', content);
    fileResourceB.setCreated(DateTime.now().minus(Days.ONE).toDate());
    String uidB = fileResourceService.saveFileResource(fileResourceB, content);

    User userB = makeUser("B");
    userB.setAvatar(fileResourceB);
    userService.addUser(userB);

    assertNotNull(fileResourceService.getFileResource(uidA));
    assertNotNull(fileResourceService.getFileResource(uidB));

    cleanUpJob.execute(null, NoopJobProgress.INSTANCE);

    assertNull(fileResourceService.getFileResource(uidA));
    assertNotNull(fileResourceService.getFileResource(uidB));

    // The following is needed because HibernateDbmsManager.emptyDatabase
    // empties fileresource before userinfo (which it must because
    // fileresource references userinfo).

    userB.setAvatar(null);
    userService.updateUser(userB);
  }

  @Disabled
  @Test
  void testFalsePositive() {
    systemSettingManager.saveSystemSetting(
        SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.THREE_MONTHS);

    content = "externalA".getBytes();
    ExternalFileResource ex = createExternal('A', content);

    String uid = ex.getFileResource().getUid();
    ex.getFileResource().setAssigned(false);
    fileResourceService.updateFileResource(ex.getFileResource());

    cleanUpJob.execute(null, NoopJobProgress.INSTANCE);

    assertNotNull(
        externalFileResourceService.getExternalFileResourceByAccessToken(ex.getAccessToken()));
    assertNotNull(fileResourceService.getFileResource(uid));
    assertTrue(fileResourceService.getFileResource(uid).isAssigned());
  }

  @Disabled
  @Test
  void testFailedUpload() {
    systemSettingManager.saveSystemSetting(
        SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.THREE_MONTHS);

    content = "externalA".getBytes();
    ExternalFileResource ex = createExternal('A', content);

    String uid = ex.getFileResource().getUid();
    ex.getFileResource().setStorageStatus(FileResourceStorageStatus.PENDING);
    fileResourceService.updateFileResource(ex.getFileResource());

    cleanUpJob.execute(null, NoopJobProgress.INSTANCE);

    assertNull(
        externalFileResourceService.getExternalFileResourceByAccessToken(ex.getAccessToken()));
    assertNull(fileResourceService.getFileResource(uid));
  }

  private DataValue createFileResourceDataValue(char uniqueChar, byte[] content) {
    DataElement fileElement =
        createDataElement(uniqueChar, ValueType.FILE_RESOURCE, AggregationType.NONE);
    OrganisationUnit orgUnit = createOrganisationUnit(uniqueChar);

    dataElementService.addDataElement(fileElement);
    organisationUnitService.addOrganisationUnit(orgUnit);

    FileResource fileResource = createFileResource(uniqueChar, content);
    String uid = fileResourceService.saveFileResource(fileResource, content);

    DataValue dataValue = createDataValue(fileElement, period, orgUnit, uid, null);
    fileResource.setAssigned(true);
    fileResource.setCreated(DateTime.now().minus(Days.ONE).toDate());
    fileResource.setStorageStatus(FileResourceStorageStatus.STORED);

    fileResourceService.updateFileResource(fileResource);
    dataValueService.addDataValue(dataValue);

    return dataValue;
  }

  private ExternalFileResource createExternal(char uniqueChar, byte[] content) {
    ExternalFileResource externalFileResource = createExternalFileResource(uniqueChar, content);

    fileResourceService.saveFileResource(externalFileResource.getFileResource(), content);
    externalFileResourceService.saveExternalFileResource(externalFileResource);

    FileResource fileResource = externalFileResource.getFileResource();
    fileResource.setCreated(DateTime.now().minus(Days.ONE).toDate());
    fileResource.setStorageStatus(FileResourceStorageStatus.STORED);

    fileResourceService.updateFileResource(fileResource);
    return externalFileResource;
  }
}
