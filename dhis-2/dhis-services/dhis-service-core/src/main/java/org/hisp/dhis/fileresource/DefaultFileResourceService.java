package org.hisp.dhis.fileresource;

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

import com.google.common.io.ByteSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.joda.time.Seconds;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultFileResourceService
    implements FileResourceService
{
    private static final Log log = LogFactory.getLog(DefaultFileResourceService.class);

    private static final Duration IS_ORPHAN_TIME_DELTA = Hours.TWO.toStandardDuration();

    private static final Predicate<FileResource> IS_ORPHAN_PREDICATE =
        ( fr -> !fr.isAssigned() || fr.getStorageStatus() != FileResourceStorageStatus.STORED );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<FileResource> fileResourceStore;

    public void setFileResourceStore( GenericIdentifiableObjectStore<FileResource> fileResourceStore )
    {
        this.fileResourceStore = fileResourceStore;
    }

    private FileResourceContentStore fileResourceContentStore;

    public void setFileResourceContentStore( FileResourceContentStore fileResourceContentStore )
    {
        this.fileResourceContentStore = fileResourceContentStore;
    }

    private Scheduler scheduler;

    public void setScheduler( Scheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    private FileResourceUploadCallback uploadCallback;

    public void setUploadCallback( FileResourceUploadCallback uploadCallback )
    {
        this.uploadCallback = uploadCallback;
    }

    // -------------------------------------------------------------------------
    // FileResourceService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FileResource getFileResource( String uid )
    {
        // TODO ensureStorageStatus(..) is a temp fix. Should be removed.
        return ensureStorageStatus( fileResourceStore.getByUid( uid ) );
    }

    @Override
    @Transactional
    public List<FileResource> getFileResources( List<String> uids )
    {
        return fileResourceStore.getByUid( uids );
    }

    @Override
    @Transactional
    public List<FileResource> getOrphanedFileResources( )
    {
        return fileResourceStore.getAllLeCreated( new DateTime().minus( IS_ORPHAN_TIME_DELTA ).toDate() )
            .stream().filter( IS_ORPHAN_PREDICATE ).collect( Collectors.toList() );
    }

    @Override
    @Transactional
    public String saveFileResource( FileResource fileResource, File file )
    {
        return saveFileResourceInternal( fileResource, () -> fileResourceContentStore.saveFileResourceContent( fileResource, file ) );
    }

    @Override
    @Transactional
    public String saveFileResource( FileResource fileResource, byte[] bytes )
    {
        return saveFileResourceInternal( fileResource, () -> fileResourceContentStore.saveFileResourceContent( fileResource, bytes ) );
    }

    @Override
    @Transactional
    public void deleteFileResource( String uid )
    {
        if ( uid == null )
        {
            return;
        }

        FileResource fileResource = fileResourceStore.getByUid( uid );

        if ( fileResource == null )
        {
            return;
        }

        fileResourceContentStore.deleteFileResourceContent( fileResource.getStorageKey() );
        fileResourceStore.delete( fileResource );
    }

    @Override
    public ByteSource getFileResourceContent( FileResource fileResource )
    {
        return fileResourceContentStore.getFileResourceContent( fileResource.getStorageKey() );
    }

    @Override
    @Transactional
    public boolean fileResourceExists( String uid )
    {
        return fileResourceStore.getByUid( uid ) != null;
    }

    @Override
    @Transactional
    public void updateFileResource( FileResource fileResource )
    {
        fileResourceStore.update( fileResource );
    }

    @Override
    public URI getSignedGetFileResourceContentUri( String uid )
    {
        FileResource fileResource = getFileResource( uid );

        if ( fileResource == null )
        {
            return null;
        }

        return fileResourceContentStore.getSignedGetContentUri( fileResource.getStorageKey() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String saveFileResourceInternal( FileResource fileResource, Callable<String> saveCallable )
    {
        fileResource.setStorageStatus( FileResourceStorageStatus.PENDING );
        fileResourceStore.save( fileResource );

        final ListenableFuture<String> saveContentTask = scheduler.executeTask( saveCallable );

        final String uid = fileResource.getUid();

        saveContentTask.addCallback( uploadCallback.newInstance( uid ) );

        return uid;
    }

    /**
     * TODO Temporary fix. Remove at some point.
     *
     * Ensures correctness of the storageStatus of this FileResource.
     *
     * If the status has been 'PENDING' for more than 1 second we check to see if the content may actually have been stored.
     * If this is the case the status is corrected to STORED.
     *
     * This method is a TEMPORARY fix for the for now unsolved issue with a race occurring between the Hibernate object cache
     * and the upload callback attempting to modify the FileResource object upon completion.
     *
     * Resolving that issue (likely by breaking the StorageStatus into a separate table) should make this method redundant.
     */
    private FileResource ensureStorageStatus( FileResource fileResource )
    {
        if ( fileResource != null && fileResource.getStorageStatus() == FileResourceStorageStatus.PENDING )
        {
            Duration pendingDuration = new Duration( new DateTime( fileResource.getLastUpdated() ), DateTime.now() );

            if ( pendingDuration.isLongerThan( Seconds.seconds( 1 ).toStandardDuration() ) )
            {
                // Upload has been finished for 5+ seconds and is still PENDING.
                // Check if content has actually been stored and correct to STORED if this is the case.

                boolean contentIsStored = fileResourceContentStore.fileResourceContentExists( fileResource.getStorageKey() );

                if ( contentIsStored )
                {
                    // We fix
                    fileResource.setStorageStatus( FileResourceStorageStatus.STORED );
                    fileResourceStore.update( fileResource );
                    log.warn( "Corrected issue: FileResource '" + fileResource.getUid() +
                        "' had storageStatus PENDING but content was actually stored." );
                }
            }
        }

        return fileResource;
    }
}
