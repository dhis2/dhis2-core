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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.*;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class TrackedEntityTypeValidationTest extends DhisSpringTest {

  @Autowired private RenderService _renderService;

  @Autowired private UserService _userService;

  @Autowired private MetadataImportService importService;

  @Override
  protected void setUpTest() throws Exception {
    renderService = _renderService;
    userService = _userService;
  }

  @Test
  void shouldSuccessTrackedEntityAttributeExists() throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("dxf2/simple_metadata.json").getInputStream(), RenderFormat.JSON);
    MetadataImportParams importParams = new MetadataImportParams();
    importParams.setImportMode(ObjectBundleMode.COMMIT);
    importParams.setImportStrategy(ImportStrategy.CREATE);
    importParams.setObjects(metadata);
    ImportReport report = importService.importMetadata(importParams);
    assertEquals(Status.OK, report.getStatus());
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> importMetadata =
        renderService.fromMetadata(
            new ClassPathResource("dxf2/import/te_type_tea_ok.json").getInputStream(),
            RenderFormat.JSON);
    MetadataImportParams importParamsFail = new MetadataImportParams();
    importParamsFail.setImportMode(ObjectBundleMode.COMMIT);
    importParamsFail.setImportStrategy(ImportStrategy.CREATE_AND_UPDATE);
    importParamsFail.setObjects(importMetadata);
    ImportReport importReport = importService.importMetadata(importParamsFail);
    assertEquals(Status.OK, importReport.getStatus());
  }

  @Test
  void shouldFailMissingTrackedEntityAttribute() throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("dxf2/simple_metadata.json").getInputStream(), RenderFormat.JSON);
    MetadataImportParams importParams = new MetadataImportParams();
    importParams.setImportMode(ObjectBundleMode.COMMIT);
    importParams.setImportStrategy(ImportStrategy.CREATE);
    importParams.setObjects(metadata);
    ImportReport report = importService.importMetadata(importParams);
    assertEquals(Status.OK, report.getStatus());
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> importMetadata =
        renderService.fromMetadata(
            new ClassPathResource("dxf2/import/te_type_missing_tea.json").getInputStream(),
            RenderFormat.JSON);
    MetadataImportParams importParamsFail = new MetadataImportParams();
    importParamsFail.setImportMode(ObjectBundleMode.COMMIT);
    importParamsFail.setImportStrategy(ImportStrategy.CREATE_AND_UPDATE);
    importParamsFail.setObjects(importMetadata);
    ImportReport importReport = importService.importMetadata(importParamsFail);
    assertEquals(Status.ERROR, importReport.getStatus());
  }
}
