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

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * Job will fetch all the image FileResources with flag hasMultiple set to false. It will process those image FileResources create three images files for each of them.
 * Once created, images will be stored at EWS and flag hasMultiple is set to true.
 *
 * @Author Zubair Asghar.
 */

@Component( "imageProcessingJob" )
public class FileResourceProcessingJob extends AbstractJob
{
    private static final Log log = LogFactory.getLog( FileResourceProcessingJob.class );

    private final FileResourceContentStore fileResourceContentStore;

    private final FileResourceService fileResourceService;

    private final FileResourceUploadCallback uploadCallback;

    private final SchedulingManager schedulingManager;

    private final ImageProcessingService imageProcessingService;

    public FileResourceProcessingJob( FileResourceContentStore fileResourceContentStore, FileResourceService fileResourceService,
        FileResourceUploadCallback uploadCallback, SchedulingManager schedulingManager, ImageProcessingService imageProcessingService )
    {
        this.fileResourceContentStore = fileResourceContentStore;
        this.fileResourceService = fileResourceService;
        this.uploadCallback = uploadCallback;
        this.schedulingManager = schedulingManager;
        this.imageProcessingService = imageProcessingService;
    }

    @Override
    public JobType getJobType()
    {
        return JobType.IMAGE_PROCESSING;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration ) throws Exception
    {
        List<FileResource> fileResources = fileResourceService.getAllUnProcessedImagesFiles();

        File tmpFile = null;

        String uid = null;

        int count = 0;

        for ( FileResource fileResource : fileResources )
        {
            String key = fileResource.getStorageKey();

            try
            {
                if ( !fileResourceContentStore.fileResourceContentExists( key ) )
                {
                    log.error( "The referenced file could not be found for FileResource: " + fileResource.getUid() );
                    continue;
                }

                ByteSource content = Resources.asByteSource( fileResourceContentStore.getSignedGetContentUri( key ).toURL() );


                tmpFile = new File( UUID.randomUUID().toString() );

                FileOutputStream fileOutputStream = new FileOutputStream( tmpFile );

                fileOutputStream.write( content.read() );

                Map<ImageFileDimension, File> imageFiles = imageProcessingService.createImages( fileResource, tmpFile );

                uid = saveFileResourceInternal( imageFiles, fileResource );
            }
            catch ( Exception e )
            {
                DebugUtils.getStackTrace( e );
                e.printStackTrace();
            }

            if ( uid == null )
            {
                log.warn( "Error in saving file" );
            }
            else
            {
                count++;
            }
        }

        log.info( String.format( "Number of file resources processed: %d", count ) );
    }

    private String saveFileResourceInternal( Map<ImageFileDimension, File> imageFiles, FileResource fileResource )
    {
        final String uid = fileResource.getUid();

        final ListenableFuture<String> saveContentTask = schedulingManager.executeJob( () -> fileResourceContentStore.saveFileResourceContent( fileResource, imageFiles ) );

        saveContentTask.addCallback( uploadCallback.newInstance( uid ) );

        return uid;
    }
}
