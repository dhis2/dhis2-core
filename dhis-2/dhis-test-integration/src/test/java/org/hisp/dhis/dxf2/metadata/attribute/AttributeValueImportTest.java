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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

class AttributeValueImportTest extends NonTransactionalIntegrationTest
{
    @Autowired
    private RenderService renderService;

    @Autowired
    private MetadataImportService importService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    void testValidateAttributeNotAssigned()
        throws IOException
    {
        // Import an Attribute which only assigned to DataSet
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> attributes = renderService
            .fromMetadata( new ClassPathResource( "dxf2/attribute/attribute.json" ).getInputStream(),
                RenderFormat.JSON );

        MetadataImportParams params = createParams( ImportStrategy.CREATE, attributes );
        final ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        // Import DataElement with created Attribute.
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> dataElements = renderService
            .fromMetadata( new ClassPathResource( "dxf2/attribute/de_with_attribute.json" ).getInputStream(),
                RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE, dataElements );
        final ImportReport importReport = importService.importMetadata( params );

        assertEquals( Status.ERROR, importReport.getStatus() );
        assertEquals( ErrorCode.E6012, importReport.getTypeReport( DataElement.class ).getObjectReports().get( 0 )
            .getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testImportInvalidValueType()
        throws IOException
    {
        // Import an Attribute which only assigned to DataSet
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> attributes = renderService
            .fromMetadata( new ClassPathResource( "dxf2/attribute/attribute.json" ).getInputStream(),
                RenderFormat.JSON );

        MetadataImportParams params = createParams( ImportStrategy.CREATE, attributes );
        final ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        // Import DataElement with created Attribute.
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> dataSets = renderService
            .fromMetadata(
                new ClassPathResource( "dxf2/attribute/dataSet_with_attribute_invalid_value.json" ).getInputStream(),
                RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE, dataSets );
        final ImportReport importReport = importService.importMetadata( params );

        assertEquals( Status.ERROR, importReport.getStatus() );
        assertEquals( ErrorCode.E6016, importReport.getTypeReport( DataSet.class ).getObjectReports().get( 0 )
            .getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    void testSaveAttributeValueAfterUpdateAttribute()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> attributes = renderService
            .fromMetadata( new ClassPathResource( "dxf2/attribute/attribute.json" ).getInputStream(),
                RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, attributes );
        final ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> dataSets = renderService
            .fromMetadata( new ClassPathResource( "dxf2/attribute/dataSet.json" ).getInputStream(), RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE, dataSets );
        final ImportReport importReport = importService.importMetadata( params );
        assertEquals( Status.OK, importReport.getStatus() );

        DataSet dataSet = manager.get( DataSet.class, "sPnR8BCInMV" );
        assertEquals( "true", dataSet.getAttributeValue( "PtyV6lLcmol" ).getValue() );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> attributesUpdate = renderService
            .fromMetadata( new ClassPathResource( "dxf2/attribute/attribute_update.json" ).getInputStream(),
                RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, attributesUpdate );
        final ImportReport importReport1 = importService.importMetadata( params );
        assertEquals( Status.OK, importReport1.getStatus() );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> dataSetUpdate = renderService.fromMetadata(
            new ClassPathResource( "dxf2/attribute/dataSet_update.json" ).getInputStream(), RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, dataSetUpdate );
        final ImportReport importReport2 = importService.importMetadata( params );
        assertEquals( Status.OK, importReport2.getStatus() );
        DataSet updatedDataSet = manager.get( DataSet.class, "sPnR8BCInMV" );
        assertEquals( "false", updatedDataSet.getAttributeValue( "PtyV6lLcmol" ).getValue() );
    }

    private MetadataImportParams createParams( ImportStrategy importStrategy,
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata )
    {
        return new MetadataImportParams()
            .setImportMode( ObjectBundleMode.COMMIT )
            .setImportStrategy( importStrategy )
            .setObjects( metadata );
    }
}
