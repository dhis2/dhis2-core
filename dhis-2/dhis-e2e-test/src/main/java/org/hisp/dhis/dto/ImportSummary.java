package org.hisp.dhis.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class ImportSummary
{
    private String status;

    private String description;

    private String reference;

    private ImportCount importCount;

    private List<Conflict> conflicts;

    public String getStatus()
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getReference()
    {
        return reference;
    }

    public void setReference( String reference )
    {
        this.reference = reference;
    }

    public ImportCount getImportCount()
    {
        return importCount;
    }

    public void setImportCount( ImportCount importCount )
    {
        this.importCount = importCount;
    }

    public List<Conflict> getConflicts()
    {
        return conflicts;
    }

    public void setConflicts( List<Conflict> conflicts )
    {
        this.conflicts = conflicts;
    }
}
