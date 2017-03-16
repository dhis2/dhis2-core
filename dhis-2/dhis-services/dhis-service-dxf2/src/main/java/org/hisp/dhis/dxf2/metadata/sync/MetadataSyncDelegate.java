package org.hisp.dhis.dxf2.metadata.sync;

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


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handling remote calls for metadata sync
 *
 * @author aamerm
 */
public class MetadataSyncDelegate
{
    private static final Log log = LogFactory.getLog( MetadataSyncDelegate.class );

    @Autowired
    private DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SystemService systemService;

    public boolean shouldStopSync( String metadataVersionSnapshot )
    {
        SystemInfo systemInfo = systemService.getSystemInfo();
        String systemVersion = systemInfo.getVersion();

        if ( StringUtils.isEmpty( systemVersion ) || !metadataSystemSettingService.getStopMetadataSyncSetting() )
        {
            return false;
        }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( metadataVersionSnapshot.getBytes( StandardCharsets.UTF_8 ) );
        String remoteVersion = "";

        try
        {
            JsonNode systemObject = renderService.getSystemObject( byteArrayInputStream, RenderFormat.JSON );

            if ( systemObject == null )
            {
                return false;
            }

            remoteVersion = systemObject.get( "version" ).textValue();

            if ( StringUtils.isEmpty( remoteVersion ) )
            {
                return false;
            }
        }
        catch ( IOException e )
        {
            log.error( "Exception occurred when parsing the metadata snapshot" + e.getMessage() );
        }

        return !systemVersion.trim().equals( remoteVersion.trim() );
    }
}
