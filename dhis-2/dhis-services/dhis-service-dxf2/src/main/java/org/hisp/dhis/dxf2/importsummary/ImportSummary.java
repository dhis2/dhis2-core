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
package org.hisp.dhis.dxf2.importsummary;

import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "importSummary", namespace = DxfNamespaces.DXF_2_0 )
public class ImportSummary extends AbstractWebMessageResponse implements ImportConflicts
{
    private ImportStatus status = ImportStatus.SUCCESS;

    private ImportOptions importOptions;

    private String description;

    private ImportCount importCount = new ImportCount();

    /**
     * Number of total conflicts. In contrast to the collection of
     * {@link ImportConflict}s which deduplicate this variable counts each
     * conflict added.
     */
    private int totalConflictOccurrenceCount = 0;

    private final Map<String, ImportConflict> conflicts = new LinkedHashMap<>();

    private String dataSetComplete;

    private String reference;

    private String href;

    private ImportSummaries relationships;

    private ImportSummaries enrollments;

    private ImportSummaries events;

    public ImportSummary()
    {
    }

    public ImportSummary( String reference )
    {
        this.reference = reference;
    }

    public ImportSummary( ImportStatus status )
    {
        this();
        this.status = status;
    }

    public ImportSummary( ImportStatus status, String description )
    {
        this();
        this.status = status;
        this.description = description;
    }

    public ImportSummary( ImportStatus status, String description, ImportCount importCount )
    {
        this();
        this.status = status;
        this.description = description;
        this.importCount = importCount;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isStatus( ImportStatus status )
    {
        return this.status != null && this.status == status;
    }

    public static ImportSummary success()
    {
        return new ImportSummary();
    }

    public static ImportSummary error( final String description )
    {
        return new ImportSummary( ERROR, description ).incrementIgnored();
    }

    public static ImportSummary error( final String description, final String reference )
    {
        return new ImportSummary( ERROR, description ).setReference( reference ).incrementIgnored();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportStatus getStatus()
    {
        return status;
    }

    public ImportSummary setStatus( ImportStatus status )
    {
        this.status = status;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportOptions getImportOptions()
    {
        return importOptions;
    }

    public ImportSummary setImportOptions( ImportOptions importOptions )
    {
        this.importOptions = importOptions;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    public ImportSummary setDescription( String description )
    {
        this.description = description;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportCount getImportCount()
    {
        return importCount;
    }

    public ImportSummary setImportCount( ImportCount importCount )
    {
        this.importCount = importCount;
        return this;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "conflicts", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "conflict", namespace = DxfNamespaces.DXF_2_0 )
    public Iterable<ImportConflict> getConflicts()
    {
        return conflicts.values();
    }

    /**
     * This is strictly just provided to allow deserialisation form JSON.
     *
     * This method does not set but add the provided conflicts which in case of
     * a fresh summary from deserialisation has the same result.
     *
     * @param conflicts list of conflicts to add
     */
    public void setConflicts( List<ImportConflict> conflicts )
    {
        conflicts.forEach( this::addConflict );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDataSetComplete()
    {
        return dataSetComplete;
    }

    public ImportSummary setDataSetComplete( String dataSetComplete )
    {
        this.dataSetComplete = dataSetComplete;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getReference()
    {
        return reference;
    }

    public ImportSummary setReference( String reference )
    {
        this.reference = reference;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getHref()
    {
        return href;
    }

    public ImportSummary setHref( String href )
    {
        this.href = href;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportSummaries getRelationships()
    {
        return relationships;
    }

    public ImportSummary setRelationships( ImportSummaries relationships )
    {
        this.relationships = relationships;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportSummaries getEnrollments()
    {
        return enrollments;
    }

    public ImportSummary setEnrollments( ImportSummaries enrollments )
    {
        this.enrollments = enrollments;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportSummaries getEvents()
    {
        return events;
    }

    public void setEvents( ImportSummaries events )
    {
        this.events = events;
    }

    public ImportSummary incrementImported()
    {
        importCount.incrementImported();
        return this;
    }

    public ImportSummary incrementUpdated()
    {
        importCount.incrementUpdated();
        return this;
    }

    public ImportSummary incrementIgnored()
    {
        importCount.incrementIgnored();
        return this;
    }

    public ImportSummary incrementDeleted()
    {
        importCount.incrementDeleted();
        return this;
    }

    public String toCountString()
    {
        return String.format( "Imported %d, updated %d, deleted %d, ignored %d",
            importCount.getImported(), importCount.getUpdated(), importCount.getDeleted(), importCount.getIgnored() );
    }

    /**
     * Called to mark a data value that should be skipped/ignored during
     * validation.
     */
    public void skipValue()
    {
        importCount.incrementIgnored();
    }

    public int skippedValueCount()
    {
        return importCount.getIgnored();
    }

    @Override
    public void addConflict( ImportConflict conflict )
    {
        totalConflictOccurrenceCount++;
        conflicts.compute( conflict.getGroupingKey(),
            ( key, aggregate ) -> aggregate == null ? conflict : aggregate.mergeWith( conflict ) );
    }

    @Override
    public int getTotalConflictOccurrenceCount()
    {
        return totalConflictOccurrenceCount;
    }

    @Override
    public int getConflictCount()
    {
        return conflicts.size();
    }

    @Override
    public String getConflictsDescription()
    {
        return conflicts.toString();
    }

    @Override
    public String toString()
    {
        return "ImportSummary{" +
            "status=" + status +
            ", description='" + description + '\'' +
            ", importCount=" + importCount +
            ", conflicts=" + conflicts +
            ", dataSetComplete='" + dataSetComplete + '\'' +
            ", reference='" + reference + '\'' +
            ", href='" + href + '\'' +
            '}';
    }

}
