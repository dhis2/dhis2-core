/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dataexchange.client.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal representation of a DHIS 2 import summary response. Supports the
 * import summary up to DHIS 2.37 where the summary was located at the root, and
 * the import summary after 2.38 where the summary was located in the
 * {@code response} object.
 *
 * @author Lars Helge Overland
 */
@Getter
@Setter
@NoArgsConstructor
public class InternalImportSummaryResponse
    extends Dhis2Response
{
    /**
     * DHIS 2.38 and later.
     */
    @JsonProperty
    private ImportSummary response;

    /**
     * DHIS 2.37 and earlier.
     */
    private ImportOptions importOptions;

    /**
     * DHIS 2.37 and earlier.
     */
    @JsonProperty
    private String description;

    /**
     * DHIS 2.37 and earlier.
     */
    @JsonProperty
    private ImportCount importCount;

    /**
     * DHIS 2.37 and earlier.
     */
    @JsonProperty
    private final List<ImportConflict> conflicts = new ArrayList<>();

    /**
     * Returns the {@link ImportSummary} response.
     *
     * @return the {@link ImportSummary}.
     */
    public ImportSummary getImportSummary()
    {
        return hasImportSummary238() ? getImportSummary238() : getImportSummary237();
    }

    /**
     * Indicates whether a response object is present, which if so means the
     * response is version 2.38 or later.
     */
    private boolean hasImportSummary238()
    {
        return response != null && response.getStatus() != null;
    }

    /**
     * Returns an {@link ImportSummary}.
     *
     * @return an {@link ImportSummary}.
     */
    private ImportSummary getImportSummary238()
    {
        return response;
    }

    /**
     * Returns an {@link ImportSummary}.
     *
     * @return an {@link ImportSummary}.
     */
    ImportSummary getImportSummary237()
    {
        ImportSummary summary = new ImportSummary();
        summary.setStatus( toImportStatus( status ) );
        summary.setImportOptions( importOptions );
        summary.setImportCount( importCount );
        summary.setDescription( description );
        summary.setConflicts( conflicts );
        return summary;
    }

    /**
     * Returns an {@link ImportStatus} based on the given {@link Status}.
     *
     * @param status the {@link Status}.
     * @return an {@link ImportStatus}.
     */
    ImportStatus toImportStatus( Status status )
    {
        return status != null ? ImportStatus.valueOf( status.name() ) : null;
    }
}
