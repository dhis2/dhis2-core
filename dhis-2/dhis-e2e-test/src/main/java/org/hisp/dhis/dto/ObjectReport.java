package org.hisp.dhis.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class ObjectReport
{
    private String klass;

    private String uid;

    private String displayName;

    private int index;

    private List<Object> errorReports;

    public String getKlass()
    {
        return klass;
    }

    public void setKlass( String klass )
    {
        this.klass = klass;
    }

    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    public List<Object> getErrorReports()
    {
        return errorReports;
    }

    public void setErrorReports( List<Object> errorReports )
    {
        this.errorReports = errorReports;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex( int index )
    {
        this.index = index;
    }
}
