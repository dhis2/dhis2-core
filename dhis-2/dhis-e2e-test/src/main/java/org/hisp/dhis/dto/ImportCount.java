package org.hisp.dhis.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class ImportCount
{
    private int ignored;

    private int deleted;

    private int updated;

    private int imported;

    private int created;

    private int total;

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

    public int getCreated()
    {
        return created;
    }

    public void setCreated( int created )
    {
        this.created = created;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal( int total )
    {
        this.total = total;
    }
}
