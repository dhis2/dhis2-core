package org.hisp.dhis.fileresource;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultFileResourceService
    implements FileResourceService
{
    private static final Duration IS_ORPHAN_TIME_DELTA = Hours.TWO.toStandardDuration();

    private static final Predicate<FileResource> IS_ORPHAN_PREDICATE =
        ( fr -> !fr.isAssigned() );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private IdentifiableObjectStore<FileResource> fileResourceStore;

    @Autowired
    private SessionFactory sessionFactory;

    public void setFileResourceStore( IdentifiableObjectStore<FileResource> fileResourceStore )
    {
        this.fileResourceStore = fileResourceStore;
    }

    private FileResourceContentStore fileResourceContentStore;

    public void setFileResourceContentStore( FileResourceContentStore fileResourceContentStore )
    {
        this.fileResourceContentStore = fileResourceContentStore;
    }

    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
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
        return checkStorageStatus( fileResourceStore.getByUid( uid ) );
    }

    @Override
    @Transactional
    public List<FileResource> getFileResources( List<String> uids )
    {
        return fileResourceStore.getByUid( uids ).stream()
            .map( this::checkStorageStatus )
            .collect( Collectors.toList() );
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

        deleteFileResource( fileResource );
    }

    @Override
    @Transactional
    public void deleteFileResource( FileResource fileResource )
    {
        if ( fileResource == null )
        {
            return;
        }

        fileResourceContentStore.deleteFileResourceContent( fileResource.getStorageKey() );
        fileResourceStore.delete( fileResource );
    }

    @Override
    public InputStream getFileResourceContent( FileResource fileResource )
    {
        return fileResourceContentStore.getFileResourceContent( fileResource.getStorageKey() );
    }
    
    @Override
    @Transactional( readOnly = true )
    public long getFileResourceContentLength( FileResource fileResource )
    {
        return fileResourceContentStore.getFileResourceContentLength( fileResource.getStorageKey() );
    }

    @Override
    @Transactional(readOnly = true)
    public void copyFileResourceContent( FileResource fileResource, OutputStream outputStream )
        throws IOException, NoSuchElementException
    {
        fileResourceContentStore.copyContent( fileResource.getStorageKey(), outputStream );
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
        sessionFactory.getCurrentSession().flush();

        final String uid = fileResource.getUid();

        final ListenableFuture<String> saveContentTask = schedulingManager.executeJob( saveCallable );

        saveContentTask.addCallback( uploadCallback.newInstance( uid ) );

        return uid;
    }

    private FileResource checkStorageStatus( FileResource fileResource )
    {
        if ( fileResource != null )
        {
            boolean exists = fileResourceContentStore.fileResourceContentExists( fileResource.getStorageKey() );

            if ( exists )
            {
                fileResource.setStorageStatus( FileResourceStorageStatus.STORED );
            }
            else
            {
                fileResource.setStorageStatus( FileResourceStorageStatus.PENDING );
            }
        }

        return fileResource;
    }
}
