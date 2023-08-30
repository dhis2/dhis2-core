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
package org.hisp.dhis.dxf2.datavalueset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.batchhandler.DataValueAuditBatchHandler;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.quick.BatchHandlerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
class DataValueSetServiceTest extends DhisConvenienceTest {

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  @Mock private CategoryService categoryService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private PeriodService periodService;

  @Mock private BatchHandlerFactory batchHandlerFactory;

  @Mock private CompleteDataSetRegistrationService completeDataSetRegistrationService;

  @Mock private CurrentUserService currentUserService;

  @Mock private DataValueSetStore dataValueSetStore;

  @Mock private SystemSettingManager systemSettingManager;

  @Mock private LockExceptionStore lockExceptionStore;

  @Mock private I18nManager i18nManager;

  @Mock private Notifier notifier;

  @Mock private InputUtils inputUtils;

  @Mock private CalendarService calendarService;

  @Mock private DataValueService dataValueService;

  @Mock private FileResourceService fileResourceService;

  @Mock private AclService aclService;

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock private ObjectMapper objectMapper;

  @Mock private DataValueSetImportValidator dataValueSetImportValidator;

  @Mock private SchemaService schemaService;

  @InjectMocks private DefaultDataValueSetService dataValueSetService;

  @Test
  void testImportDataValuesUpdatedSkipNoChange() {
    Calendar calendar = mock(Calendar.class);
    when(calendarService.getSystemCalendar()).thenReturn(calendar);

    DataValueBatchHandler batchHandler = mock(DataValueBatchHandler.class);
    when(batchHandler.init()).thenReturn(batchHandler);
    when(batchHandlerFactory.createBatchHandler(DataValueBatchHandler.class))
        .thenReturn(batchHandler);

    DataValueAuditBatchHandler auditBatchHandler = mock(DataValueAuditBatchHandler.class);
    when(batchHandlerFactory.createBatchHandler(DataValueAuditBatchHandler.class))
        .thenReturn(auditBatchHandler);

    when(notifier.clear(any())).thenReturn(notifier);
    when(notifier.notify(any(), any(), anyString())).thenReturn(notifier);
    when(notifier.notify(any(), any(), anyString(), anyBoolean())).thenReturn(notifier);

    DataSet dataSet = createDataSet('A', new MonthlyPeriodType());
    dataSet.setUid("pBOMPrpg1QX");
    when(identifiableObjectManager.getObject(DataSet.class, IdScheme.UID, "pBOMPrpg1QX"))
        .thenReturn(dataSet);
    DataElement dataElement = createDataElement('A');
    dataElement.setUid("f7n9E0hX8qk");
    when(identifiableObjectManager.getObject(DataElement.class, IdScheme.UID, "f7n9E0hX8qk"))
        .thenReturn(dataElement);

    // simulate that the imported DataValue already exists and is identical
    // (no changes)
    when(batchHandler.findObject(any())).then(AdditionalAnswers.returnsFirstArg());

    ImportSummary summary =
        dataValueSetService.importDataValueSetXml(
            readFile("datavalueset/dataValueSetA.xml"), new ImportOptions());

    assertSuccessWithImportedUpdatedDeleted(0, 3, 0, summary);
    verify(batchHandler, never()).updateObject(any());
  }

  private InputStream readFile(String filename) {
    try {
      return new ClassPathResource(filename).getInputStream();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void assertSuccessWithImportedUpdatedDeleted(
      int imported, int updated, int deleted, ImportSummary summary) {
    assertAll(
        () -> assertHasNoConflicts(summary),
        () ->
            assertEquals(
                imported, summary.getImportCount().getImported(), "unexpected import count"),
        () ->
            assertEquals(updated, summary.getImportCount().getUpdated(), "unexpected update count"),
        () ->
            assertEquals(
                deleted, summary.getImportCount().getDeleted(), "unexpected deleted count"),
        () -> assertEquals(ImportStatus.SUCCESS, summary.getStatus(), summary.getDescription()));
  }

  private static void assertHasNoConflicts(ImportConflicts summary) {
    assertEquals(0, summary.getConflictCount(), summary.getConflictsDescription());
  }
}
