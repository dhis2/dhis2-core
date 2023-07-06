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
package org.hisp.dhis.dxf2.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

class EventImportWithMetadataTest extends DhisSpringTest {

  @Autowired private RenderService _renderService;

  @Autowired private CsvEventService csvEventService;

  @Autowired private EventService eventService;

  @Autowired private UserService _userService;

  @Autowired private MetadataImportService importService;

  private static final IdSchemes idSchemes = new IdSchemes();

  private static List<Event> events;

  @Override
  protected void setUpTest() throws Exception {
    renderService = _renderService;
    userService = _userService;
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("dxf2/import/create_program_stages.json").getInputStream(),
            RenderFormat.JSON);
    MetadataImportParams params = new MetadataImportParams();
    params.setImportMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    params.setSkipSharing(true);
    ImportReport report = importService.importMetadata(params);
    assertEquals(Status.OK, report.getStatus());
    idSchemes.setDataElementIdScheme("UID");
    idSchemes.setOrgUnitIdScheme("CODE");
    idSchemes.setProgramStageInstanceIdScheme("UID");
    events =
        csvEventService.readEvents(
            new ClassPathResource("dxf2/import/csv/events_import_code.csv").getInputStream(), true);
  }

  @Test
  void shouldSuccessImportCsvLookUpByCode() {
    idSchemes.setIdScheme("CODE");
    ImportOptions importOptions = new ImportOptions();
    importOptions.setIdSchemes(idSchemes);
    ImportSummaries importSummaries = eventService.addEvents(events, importOptions, null);
    assertEquals(ImportStatus.SUCCESS, importSummaries.getStatus());
  }

  @Test
  void shouldFailImportCsvLookUpByUid() {
    idSchemes.setIdScheme("UID");
    ImportOptions importOptions = new ImportOptions();
    importOptions.setIdSchemes(idSchemes);
    ImportSummaries importSummaries = eventService.addEvents(events, importOptions, null);
    assertEquals(ImportStatus.ERROR, importSummaries.getStatus());
  }
}
