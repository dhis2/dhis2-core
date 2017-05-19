package org.hisp.dhis.dxf2.synch;

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

import java.util.Date;

import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;

/**
 * @author Lars Helge Overland
 */
public interface SynchronizationManager
{
    /**
     * Executes a data push to remote server.
     * 
     * @return an {@link ImportSummary}.
     */
    ImportSummary executeDataPush() throws WebMessageParseException;

    /**
     * Executes an event push to remote server.
     * 
     * @return an {@link ImportSummaries}.
     */
    ImportSummaries executeEventPush() throws WebMessageParseException;
    
    /**
     * Returns the time of the last successful data sync operation.
     * 
     * @return the time of the last successful data sync operation as a {@link Date}.
     */
    Date getLastDataSynchSuccess();

    /**
     * Returns the time of the last successful event sync operation.
     * 
     * @return the time of the last successful event sync operation as a {@link Date}.
     */
    Date getLastEventSynchSuccess();
    
    /**
     * Executes a meta data pull operation from remote server.
     * 
     * @param url the URL to the remote server.
     * @return an {@link ImportReport}.
     */
    ImportReport executeMetadataPull( String url );
    
    /**
     * Indicates the availability status of the remote server.
     * 
     * @return the {@link AvailabilityStatus} of the remote server.
     */
    AvailabilityStatus isRemoteServerAvailable();    
}
