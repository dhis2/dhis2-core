package org.hisp.dhis.importexport.action.util;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.SecurityContextRunnable;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ImportMetaDataTask
    extends SecurityContextRunnable
{
    private static final Log log = LogFactory.getLog( ImportMetaDataTask.class );

    private final MetadataImportService importService;

    private final SchemaService schemaService;

    private final MetadataImportParams importParams;

    private final InputStream inputStream;

    private final String format;

    public ImportMetaDataTask( MetadataImportService importService, SchemaService schemaService,
        MetadataImportParams importParams, InputStream inputStream, String format )
    {
        super();
        this.importService = importService;
        this.schemaService = schemaService;
        this.importParams = importParams;
        this.inputStream = inputStream;
        this.format = format;
    }

    @Override
    public void call()
    {
        Metadata metadata;

        try
        {
            if ( "json".equals( format ) )
            {
                metadata = DefaultRenderService.getJsonMapper().readValue( inputStream, Metadata.class );
                log.info( "Read JSON file. Importing metadata." );
            }
            else
            {
                metadata = DefaultRenderService.getXmlMapper().readValue( inputStream, Metadata.class );
                log.info( "Read XML file. Importing metadata." );
            }
        }
        catch ( IOException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            throw new RuntimeException( "Failed to parse meta data input stream", ex );
        }

        importParams.addMetadata( schemaService.getMetadataSchemas(), metadata );
        importService.importMetadata( importParams );
    }
}
