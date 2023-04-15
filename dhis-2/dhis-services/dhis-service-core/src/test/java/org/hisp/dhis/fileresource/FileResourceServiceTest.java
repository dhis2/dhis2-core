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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.fileresource.events.FileDeletedEvent;
import org.hisp.dhis.fileresource.events.FileSavedEvent;
import org.hisp.dhis.fileresource.events.ImageFileSavedEvent;
import org.hisp.dhis.period.PeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class FileResourceServiceTest
{
    @Mock
    private FileResourceStore fileResourceStore;

    @Mock
    private PeriodService periodService;

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
    private ArgumentCaptor<FileSavedEvent> fileSavedEventCaptor;

    @Captor
    private ArgumentCaptor<ImageFileSavedEvent> imageFileSavedEventCaptor;

    @Captor
    private ArgumentCaptor<FileDeletedEvent> fileDeletedEventCaptor;

    private FileResourceService subject;

    @BeforeEach
    public void setUp()
    {
        subject = new DefaultFileResourceService( fileResourceStore, periodService, sessionFactory,
            fileResourceContentStore,
            imageProcessingService, fileEventPublisher );
    }

    @Test
    void verifySaveFile()
    {
        FileResource fileResource = new FileResource( "mycat.pdf", "application/pdf", 1000, "md5",
            FileResourceDomain.PUSH_ANALYSIS );

        fileResource.setUid( "fileRes1" );

        File file = new File( "" );

        when( sessionFactory.getCurrentSession() ).thenReturn( session );

        subject.saveFileResource( fileResource, file );

        verify( fileResourceStore ).save( fileResource );
        verify( session ).flush();

        verify( fileEventPublisher, times( 1 ) ).publishEvent( fileSavedEventCaptor.capture() );

        FileSavedEvent event = fileSavedEventCaptor.getValue();

        assertThat( event.getFileResource(), is( "fileRes1" ) );
        assertThat( event.getFile(), is( file ) );
    }

    @Test
    void verifySaveIllegalFileTypeResourceA()
    {
        FileResource fileResource = new FileResource( "very_evil_script.html", "text/html", 1024, "md5",
            FileResourceDomain.USER_AVATAR );

        File file = new File( "very_evil_script.html" );
        assertThrows( IllegalQueryException.class, () -> subject.saveFileResource( fileResource, file ) );
    }

    @Test
    void verifySaveIllegalFileTypeResourceB()
    {
        FileResource fileResource = new FileResource( "suspicious_program.rpm", "application/x-rpm", 2048, "md5",
            FileResourceDomain.MESSAGE_ATTACHMENT );

        File file = new File( "suspicious_program.rpm" );
        assertThrows( IllegalQueryException.class, () -> subject.saveFileResource( fileResource, file ) );
    }

    @Test
    void verifySaveImageFile()
    {
        FileResource fileResource = new FileResource( "test.jpeg", MimeTypeUtils.IMAGE_JPEG.toString(), 1000, "md5",
            FileResourceDomain.DATA_VALUE );

        File file = new File( "" );

        Map<ImageFileDimension, File> imageFiles = Map.of( ImageFileDimension.LARGE, file );

        when( imageProcessingService.createImages( fileResource, file ) ).thenReturn( imageFiles );

        when( sessionFactory.getCurrentSession() ).thenReturn( session );

        fileResource.setUid( "imageUid1" );

        subject.saveFileResource( fileResource, file );

        verify( fileResourceStore ).save( fileResource );
        verify( session ).flush();

        verify( fileEventPublisher, times( 1 ) ).publishEvent( imageFileSavedEventCaptor.capture() );

        ImageFileSavedEvent event = imageFileSavedEventCaptor.getValue();

        assertThat( event.getFileResource(), is( "imageUid1" ) );
        assertFalse( event.getImageFiles().isEmpty() );
        assertThat( event.getImageFiles().size(), is( 1 ) );
        assertThat( event.getImageFiles(), hasKey( ImageFileDimension.LARGE ) );
    }

    @Test
    void verifyDeleteFile()
    {
        FileResource fileResource = new FileResource( "test.pdf", "application/pdf", 1000, "md5",
            FileResourceDomain.DOCUMENT );

        fileResource.setUid( "fileUid1" );

        when( fileResourceStore.get( anyLong() ) ).thenReturn( fileResource );

        subject.deleteFileResource( fileResource );

        verify( fileResourceStore ).delete( fileResource );

        verify( fileEventPublisher, times( 1 ) ).publishEvent( fileDeletedEventCaptor.capture() );

        FileDeletedEvent event = fileDeletedEventCaptor.getValue();

        assertThat( event.getContentType(), is( "application/pdf" ) );
        assertThat( event.getDomain(), is( FileResourceDomain.DOCUMENT ) );
    }

    @Test
    void verifySaveOrgUnitImageFile()
    {
        FileResource fileResource = new FileResource( "test.jpeg", MimeTypeUtils.IMAGE_JPEG.toString(), 1000, "md5",
            FileResourceDomain.ORG_UNIT );

        File file = new File( "" );

        Map<ImageFileDimension, File> imageFiles = Map.of( ImageFileDimension.LARGE, file );

        when( imageProcessingService.createImages( fileResource, file ) ).thenReturn( imageFiles );

        when( sessionFactory.getCurrentSession() ).thenReturn( session );

        fileResource.setUid( "imageUid1" );

        subject.saveFileResource( fileResource, file );

        verify( fileResourceStore ).save( fileResource );
        verify( session ).flush();

        verify( fileEventPublisher, times( 1 ) ).publishEvent( imageFileSavedEventCaptor.capture() );

        ImageFileSavedEvent event = imageFileSavedEventCaptor.getValue();

        assertThat( event.getFileResource(), is( "imageUid1" ) );
        assertFalse( event.getImageFiles().isEmpty() );
        assertThat( event.getImageFiles().size(), is( 1 ) );
        assertThat( event.getImageFiles(), hasKey( ImageFileDimension.LARGE ) );
    }
}