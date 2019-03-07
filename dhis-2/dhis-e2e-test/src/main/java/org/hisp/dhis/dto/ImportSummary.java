package org.hisp.dhis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    public class ImportCount {
        private int ignored;
        private int deleted;
        private int updated;
        private int imported;

        public int getIgnored()
        {
            return ignored;
        }

        public void setIgnored( int ignored )
        {
            this.ignored = ignored;
        }

        public int getDeleted()
        {
            return deleted;
        }

        public void setDeleted( int deleted )
        {
            this.deleted = deleted;
        }

        public int getUpdated()
        {
            return updated;
        }

        public void setUpdated( int updated )
        {
            this.updated = updated;
        }

        public int getImported()
        {
            return imported;
        }

        public void setImported( int imported )
        {
            this.imported = imported;
        }
    }
}
