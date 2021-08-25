/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.metadata;

import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link MetadataExportControllerTest}.
 *
 * @author Volker Schmidt
 */
public class MetadataExportControllerTest
{
    @Mock
    private MetadataExportService metadataExportService;

    @Mock
    private ContextService contextService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserSettingService userSettingService;

    @InjectMocks
    private MetadataImportExportController controller;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void withoutDownload()
    {
        ResponseEntity<RootNode> responseEntity = controller.getMetadata( false, null, false );
        Assert.assertNull( responseEntity.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
    }

    @Test
    public void withDownload()
    {
        ResponseEntity<RootNode> responseEntity = controller.getMetadata( false, null, true );
        Assert.assertNotNull( responseEntity.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( "attachment; filename=metadata",
            responseEntity.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).get( 0 ) );
    }
}