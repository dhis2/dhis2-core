package org.hisp.dhis.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class TypeReport
{
    private String klass;

    private List<ObjectReport> objectReports;

    private ImportCount stats;

    public String getKlass()
    {
        return klass;
    }

    public void setKlass( String klass )
    {
        this.klass = klass;
    }

    public List<ObjectReport> getObjectReports()
    {
        return objectReports;
    }

    public void setObjectReports( List<ObjectReport> objectReports )
    {
        this.objectReports = objectReports;
    }

    public ImportCount getStats()
    {
        return stats;
    }

    public void setStats( ImportCount stats )
    {
        this.stats = stats;
    }
}
