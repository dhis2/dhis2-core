package org.hisp.dhis.dxf2.metadata.sync;

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

import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;

import java.util.List;
import java.util.Map;

/**
 * MetadataSyncService defines the methods available for initiating sync related methods
 *
 * @author vanyas
 */
public interface MetadataSyncService
{
    /**
     * Gets the MetadataSyncParams from the map of parameters in the incoming request.
     *
     * @param parameters
     * @return MetadataSyncParams
     */
    MetadataSyncParams getParamsFromMap( Map<String, List<String>> parameters );

    /**
     * Checks whether metadata sync needs to be be done or not.
     * If version already exists in system it does do the sync
     * @param syncParams
     * @return
     */
    public boolean isSyncRequired ( MetadataSyncParams syncParams );
    /**
     * Does the actual metadata sync logic. Calls the underlying importer to import the relevant
     * MetadataVersion snapshot downloaded from the remote server.
     *
     * @param syncParams
     * @return
     */
    MetadataSyncSummary doMetadataSync( MetadataSyncParams syncParams ) throws DhisVersionMismatchException;
}
