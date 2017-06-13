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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * @author Halvdan Hoem Grelland
 */
public class FileResourceUploadCallback
{
    Log log = LogFactory.getLog( FileResourceUploadCallback.class );

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    public ListenableFutureCallback<String> newInstance( String fileResourceUid )
    {
        return new ListenableFutureCallback<String>()
        {
            DateTime startTime = DateTime.now();

            @Override
            public void onFailure( Throwable ex )
            {
                log.error( "Saving content for file resource '" + fileResourceUid + "' failed", ex );

                FileResource fileResource = idObjectManager.get( FileResource.class, fileResourceUid );

                if ( fileResource != null )
                {
                    log.info( "File resource '" + fileResource.getUid() + "' storageStatus set to FAILED." );

                    fileResource.setStorageStatus( FileResourceStorageStatus.FAILED );
                    idObjectManager.update( fileResource );
                }
            }

            @Override
            public void onSuccess( String result )
            {
                Period timeDiff = new Period( startTime, DateTime.now() );

                log.info( "File stored with key: '" + result + "'. Upload finished in " + timeDiff.toString( PeriodFormat.getDefault() ) );

                FileResource fileResource = idObjectManager.get( FileResource.class, fileResourceUid );

                if ( result != null && fileResource != null )
                {
                    fileResource.setStorageStatus( FileResourceStorageStatus.STORED );
                    idObjectManager.update( fileResource );
                }
                else
                {
                    log.error( "Conflict: content was stored but FileResource with uid '" + fileResourceUid + "' could not be found." );
                }
            }
        };
    }
}
