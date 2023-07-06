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
package org.hisp.dhis.dxf2.metadata.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

class AttributeValueImportTest extends TransactionalIntegrationTest {

  @Autowired private RenderService renderService;

  @Autowired private MetadataImportService importService;

  @Autowired private IdentifiableObjectManager manager;

  @Test
  void testSaveAttributeValueAfterUpdateAttribute() throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> attributes =
        renderService.fromMetadata(
            new ClassPathResource("attribute/attribute.json").getInputStream(), RenderFormat.JSON);
    MetadataImportParams params = new MetadataImportParams();
    params.setImportMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(attributes);
    final ImportReport report = importService.importMetadata(params);
    assertEquals(Status.OK, report.getStatus());
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> dataSets =
        renderService.fromMetadata(
            new ClassPathResource("attribute/dataSet.json").getInputStream(), RenderFormat.JSON);
    params = new MetadataImportParams();
    params.setImportMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(dataSets);
    final ImportReport importReport = importService.importMetadata(params);
    assertEquals(Status.OK, importReport.getStatus());
    manager.flush();
    DataSet dataSet = manager.get(DataSet.class, "sPnR8BCInMV");
    assertEquals("true", dataSet.getAttributeValue("PtyV6lLcmol").getValue());
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> attributesUpdate =
        renderService.fromMetadata(
            new ClassPathResource("attribute/attribute_update.json").getInputStream(),
            RenderFormat.JSON);
    params = new MetadataImportParams();
    params.setImportMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.UPDATE);
    params.setObjects(attributesUpdate);
    final ImportReport importReport1 = importService.importMetadata(params);
    assertEquals(Status.OK, importReport1.getStatus());
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> dataSetUpdate =
        renderService.fromMetadata(
            new ClassPathResource("attribute/dataSet_update.json").getInputStream(),
            RenderFormat.JSON);
    params = new MetadataImportParams();
    params.setImportMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.UPDATE);
    params.setObjects(dataSetUpdate);
    final ImportReport importReport2 = importService.importMetadata(params);
    assertEquals(Status.OK, importReport2.getStatus());
    DataSet updatedDataSet = manager.get(DataSet.class, "sPnR8BCInMV");
    assertEquals("false", updatedDataSet.getAttributeValue("PtyV6lLcmol").getValue());
  }
}
