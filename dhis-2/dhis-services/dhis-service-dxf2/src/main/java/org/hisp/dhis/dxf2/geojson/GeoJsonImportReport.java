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
package org.hisp.dhis.dxf2.geojson;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.webmessage.WebMessageResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tracks the conflicts during import of geo-json data.
 *
 * @author Jan Bernitt
 */
public final class GeoJsonImportReport implements ImportConflicts, WebMessageResponse
{
    @Getter
    @JsonProperty
    private final ImportCount importCount = new ImportCount();

    /**
     * Number of total conflicts. In contrast to the collection of
     * {@link ImportConflict}s which deduplicate this variable counts each
     * conflict added.
     */
    @JsonProperty
    private int totalConflictOccurrenceCount = 0;

    private final Map<String, ImportConflict> conflicts = new LinkedHashMap<>();

    @JsonProperty
    public ImportStatus getStatus()
    {
        int ignored = importCount.getIgnored();
        if ( ignored == 0 || totalConflictOccurrenceCount == 0 )
        {
            return ImportStatus.SUCCESS;
        }
        int imported = importCount.getImported();
        int updated = importCount.getUpdated();
        return imported + updated == 0 ? ImportStatus.ERROR : ImportStatus.WARNING;
    }

    @Override
    @JsonProperty
    public Iterable<ImportConflict> getConflicts()
    {
        return conflicts.values();
    }

    @Override
    public void addConflict( ImportConflict conflict )
    {
        totalConflictOccurrenceCount++;
        conflicts.compute( conflict.getGroupingKey(),
            ( key, aggregate ) -> aggregate == null ? conflict : aggregate.mergeWith( conflict ) );
    }

    @Override
    public String getConflictsDescription()
    {
        return conflicts.toString();
    }

    @Override
    public int getConflictCount()
    {
        return conflicts.size();
    }

    @Override
    public int getTotalConflictOccurrenceCount()
    {
        return totalConflictOccurrenceCount;
    }

    @Override
    public String toString()
    {
        return importCount + "\n" + conflicts;
    }
}
