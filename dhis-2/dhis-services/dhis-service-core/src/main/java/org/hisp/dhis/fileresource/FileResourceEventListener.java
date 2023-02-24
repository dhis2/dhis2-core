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

import java.io.File;
import java.util.Map;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.fileresource.events.BinaryFileSavedEvent;
import org.hisp.dhis.fileresource.events.FileDeletedEvent;
import org.hisp.dhis.fileresource.events.FileSavedEvent;
import org.hisp.dhis.fileresource.events.ImageFileSavedEvent;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @Author Zubair Asghar.
 */
@Slf4j
@Component( "org.hisp.dhis.fileresource.FileResourceEventListener" )
public class FileResourceEventListener
{
    private final FileResourceService fileResourceService;

    private final FileResourceContentStore fileResourceContentStore;

    public FileResourceEventListener( FileResourceService fileResourceService, FileResourceContentStore contentStore )
    {
        this.fileResourceService = fileResourceService;
        this.fileResourceContentStore = contentStore;
    }

    @TransactionalEventListener
    @Async
    public void save( FileSavedEvent fileSavedEvent )
    {
        DateTime startTime = DateTime.now();

        File file = fileSavedEvent.getFile();

        FileResource fileResource = fileResourceService.getFileResource( fileSavedEvent.getFileResource() );

        String storageId = fileResourceContentStore.saveFileResourceContent( fileResource, file );

        Period timeDiff = new Period( startTime, DateTime.now() );

        logMessage( storageId, fileResource, timeDiff );
    }

    @TransactionalEventListener
    @Async
    public void saveImageFile( ImageFileSavedEvent imageFileSavedEvent )
    {
        DateTime startTime = DateTime.now();

        Map<ImageFileDimension, File> imageFiles = imageFileSavedEvent.getImageFiles();

        FileResource fileResource = fileResourceService.getFileResource( imageFileSavedEvent.getFileResource() );

        String storageId = fileResourceContentStore.saveFileResourceContent( fileResource, imageFiles );

        if ( storageId != null )
        {
            fileResource.setHasMultipleStorageFiles( true );

            fileResourceService.updateFileResource( fileResource );
        }

        Period timeDiff = new Period( startTime, DateTime.now() );

        logMessage( storageId, fileResource, timeDiff );
    }

    @TransactionalEventListener
    @Async
    public void saveBinaryFile( BinaryFileSavedEvent binaryFileSavedEvent )
    {
        DateTime startTime = DateTime.now();

        byte[] bytes = binaryFileSavedEvent.getBytes();

        FileResource fileResource = fileResourceService.getFileResource( binaryFileSavedEvent.getFileResource() );

        String storageId = fileResourceContentStore.saveFileResourceContent( fileResource, bytes );

        Period timeDiff = new Period( startTime, DateTime.now() );

        logMessage( storageId, fileResource, timeDiff );
    }

    @TransactionalEventListener
    @Async
    public void deleteFile( FileDeletedEvent deleteFileEvent )
    {
        if ( !fileResourceContentStore.fileResourceContentExists( deleteFileEvent.getStorageKey() ) )
        {
            log.error( String.format( "No file exist for key: %s", deleteFileEvent.getStorageKey() ) );
            return;
        }

        if ( FileResource.isImage( deleteFileEvent.getContentType() ) &&
            deleteFileEvent.getDomain().hasImageDimensions() )
        {
            String storageKey = deleteFileEvent.getStorageKey();

            Stream.of( ImageFileDimension.values() ).forEach( d -> fileResourceContentStore
                .deleteFileResourceContent( StringUtils.join( storageKey, d.getDimension() ) ) );
        }
        else
        {
            fileResourceContentStore.deleteFileResourceContent( deleteFileEvent.getStorageKey() );
        }
    }

    private void logMessage( String storageId, FileResource fileResource, Period timeDiff )
    {
        if ( storageId == null )
        {
            log.error( String.format( "Saving content for file resource failed: %s", fileResource.getUid() ) );
            return;
        }

        log.info( String.format( "File stored with key: %s'. Upload finished in %s", storageId,
            timeDiff.toString( PeriodFormat.getDefault() ) ) );
    }
}
