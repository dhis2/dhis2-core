/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.fileresource.events.FileSavedEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Luciano Fiandesio
 */
public class FileResourceServiceTest
{
    @Mock
    private FileResourceStore fileResourceStore;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private FileResourceContentStore fileResourceContentStore;

    @Mock
    private ImageProcessingService imageProcessingService;

    @Mock
    private ApplicationEventPublisher fileEventPublisher;

    @Mock
    private Session session;

    @Captor
    protected ArgumentCaptor<FileSavedEvent> publishEventCaptor;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private FileResourceService subject;

    @Before
    public void setUp()
    {
        subject = new DefaultFileResourceService( fileResourceStore, sessionFactory, fileResourceContentStore,
            imageProcessingService, fileEventPublisher );

        when( sessionFactory.getCurrentSession() ).thenReturn( session );
    }

    @Test
    public void verifySaveFileAsImage()
    {
        FileResource fileResource = new FileResource( "mycat.jpg", MimeTypeUtils.IMAGE_JPEG.toString(), 1000, "md5",
            FileResourceDomain.PUSH_ANALYSIS );

        fileResource.setUid( "fileRes1" );

        File file = new File( "" );

        subject.saveFileResource( fileResource, file );

        verify( fileResourceStore ).save( fileResource );
        verify( session ).flush();

        verify( fileEventPublisher, times( 1 ) ).publishEvent( publishEventCaptor.capture() );

        FileSavedEvent event = publishEventCaptor.getValue();
        assertThat( event.getFileResource(), is( "fileRes1" ) );
        assertThat( event.getFile(), is( file ) );
    }

}