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

import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.events.BinaryFileSavedEvent;
import org.hisp.dhis.fileresource.events.FileDeletedEvent;
import org.hisp.dhis.fileresource.events.FileSavedEvent;
import org.hisp.dhis.fileresource.events.ImageFileSavedEvent;
import org.hisp.dhis.period.PeriodService;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.fileresource.FileResourceService" )
public class DefaultFileResourceService
    implements FileResourceService
{
    private static final Duration IS_ORPHAN_TIME_DELTA = Hours.TWO.toStandardDuration();

    public static final Predicate<FileResource> IS_ORPHAN_PREDICATE = (fr -> !fr.isAssigned());

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final FileResourceStore fileResourceStore;

    private final PeriodService periodService;

    private final SessionFactory sessionFactory;

    private final FileResourceContentStore fileResourceContentStore;

    private final ImageProcessingService imageProcessingService;

    private final ApplicationEventPublisher fileEventPublisher;

    // -------------------------------------------------------------------------
    // FileResourceService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public FileResource getFileResource( String uid )
    {
        return checkStorageStatus( fileResourceStore.getByUid( uid ) );
    }

    @Override
    @Transactional( readOnly = true )
    public Optional<FileResource> getFileResource( String uid, FileResourceDomain domain )
    {
        return fileResourceStore.findByUidAndDomain( uid, domain );
    }

    @Override
    @Transactional( readOnly = true )
    public List<FileResource> getFileResources( @Nonnull List<String> uids )
    {
        return fileResourceStore.getByUid( uids ).stream()
            .map( this::checkStorageStatus )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<FileResource> getOrphanedFileResources()
    {
        return fileResourceStore.getAllLeCreated( new DateTime().minus( IS_ORPHAN_TIME_DELTA ).toDate() ).stream()
            .filter( IS_ORPHAN_PREDICATE )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public Optional<FileResource> findByStorageKey( @CheckForNull String storageKey )
    {
        return storageKey == null ? Optional.empty() : fileResourceStore.findByStorageKey( storageKey );
    }

    @Override
    @Transactional( readOnly = true )
    public List<FileResourceOwner> findOwnersByStorageKey( @CheckForNull String storageKey )
    {
        Optional<FileResource> maybeFr = findByStorageKey( storageKey );
        if ( maybeFr.isEmpty() )
            return List.of();
        FileResource fr = maybeFr.get();
        String uid = fr.getUid();
        switch ( fr.getDomain() )
        {
            case PUSH_ANALYSIS:
                return List.of();
            case ORG_UNIT:
                return fileResourceStore.findOrganisationUnitsByImageFileResource( uid ).stream()
                    .map( id -> new FileResourceOwner( FileResourceDomain.ORG_UNIT, id ) )
                    .collect( toUnmodifiableList() );
            case DOCUMENT:
                return fileResourceStore.findDocumentsByFileResource( uid ).stream()
                    .map( id -> new FileResourceOwner( FileResourceDomain.DOCUMENT, id ) )
                    .collect( toUnmodifiableList() );
            case MESSAGE_ATTACHMENT:
                return fileResourceStore.findMessagesByFileResource( uid ).stream()
                    .map( id -> new FileResourceOwner( FileResourceDomain.MESSAGE_ATTACHMENT, id ) )
                    .collect( toUnmodifiableList() );
            case USER_AVATAR:
                return fileResourceStore.findUsersByAvatarFileResource( uid ).stream()
                    .map( id -> new FileResourceOwner( FileResourceDomain.USER_AVATAR, id ) )
                    .collect( toUnmodifiableList() );
            case DATA_VALUE:
                return fileResourceStore.findDataValuesByFileResourceValue( uid ).stream()
                    .map(
                        dv -> new FileResourceOwner( dv.de(), dv.ou(), periodService.getPeriod( dv.pe() ).getIsoDate(),
                            dv.co() ) )
                    .collect( toUnmodifiableList() );
        }
        return List.of();
    }

    @Override
    @Transactional
    public void saveFileResource( FileResource fileResource, File file )
    {
        validateFileResource( fileResource );

        fileResource.setStorageStatus( FileResourceStorageStatus.PENDING );
        fileResourceStore.save( fileResource );
        sessionFactory.getCurrentSession().flush();

        if ( FileResource.isImage( fileResource.getContentType() ) &&
            FileResourceDomain.isDomainForMultipleImages( fileResource.getDomain() ) )
        {
            Map<ImageFileDimension, File> imageFiles = imageProcessingService.createImages( fileResource, file );

            fileEventPublisher.publishEvent( new ImageFileSavedEvent( fileResource.getUid(), imageFiles ) );
            return;
        }

        fileEventPublisher.publishEvent( new FileSavedEvent( fileResource.getUid(), file ) );
    }

    @Override
    @Transactional
    public String saveFileResource( FileResource fileResource, byte[] bytes )
    {
        fileResource.setStorageStatus( FileResourceStorageStatus.PENDING );
        fileResourceStore.save( fileResource );
        sessionFactory.getCurrentSession().flush();

        final String uid = fileResource.getUid();

        fileEventPublisher.publishEvent( new BinaryFileSavedEvent( fileResource.getUid(), bytes ) );

        return uid;
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

        FileResource existingResource = fileResourceStore.get( fileResource.getId() );

        if ( existingResource == null )
        {
            return;
        }

        FileDeletedEvent deleteFileEvent = new FileDeletedEvent( existingResource.getStorageKey(),
            existingResource.getContentType(), existingResource.getDomain() );

        fileResourceStore.delete( existingResource );

        fileEventPublisher.publishEvent( deleteFileEvent );
    }

    @Override
    @Transactional( readOnly = true )
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
    @Transactional( readOnly = true )
    public void copyFileResourceContent( FileResource fileResource, OutputStream outputStream )
        throws IOException,
        NoSuchElementException
    {
        fileResourceContentStore.copyContent( fileResource.getStorageKey(), outputStream );
    }

    @Override
    @Transactional( readOnly = true )
    public byte[] copyFileResourceContent( FileResource fileResource )
        throws IOException,
        NoSuchElementException
    {
        return fileResourceContentStore.copyContent( fileResource.getStorageKey() );
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
    @Transactional( readOnly = true )
    public URI getSignedGetFileResourceContentUri( String uid )
    {
        FileResource fileResource = getFileResource( uid );

        if ( fileResource == null )
        {
            return null;
        }

        return fileResourceContentStore.getSignedGetContentUri( fileResource.getStorageKey() );
    }

    @Override
    @Transactional( readOnly = true )
    public URI getSignedGetFileResourceContentUri( FileResource fileResource )
    {
        if ( fileResource == null )
        {
            return null;
        }

        return fileResourceContentStore.getSignedGetContentUri( fileResource.getStorageKey() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<FileResource> getExpiredFileResources( FileResourceRetentionStrategy retentionStrategy )
    {
        DateTime expires = DateTime.now().minus( retentionStrategy.getRetentionTime() );
        return fileResourceStore.getExpiredFileResources( expires );
    }

    @Override
    @Transactional( readOnly = true )
    public List<FileResource> getAllUnProcessedImagesFiles()
    {
        return fileResourceStore.getAllUnProcessedImages();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Validates the given {@link FileResource}. Throws an exception if not.
     *
     * @param fileResource the file resource.
     * @throws IllegalQueryException if the given file resource is invalid.
     */
    private void validateFileResource( FileResource fileResource )
        throws IllegalQueryException
    {
        if ( fileResource.getName() == null )
        {
            throw new IllegalQueryException( ErrorCode.E6100 );
        }

        if ( !FileResourceBlocklist.isValid( fileResource ) )
        {
            throw new IllegalQueryException( ErrorCode.E6101 );
        }
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
