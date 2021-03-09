package org.hisp.dhis.dto;



import com.google.gson.annotations.Expose;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnit
{
    private String id;

    private String name;

    private String shortName;

    private String openingDate;

    private String code;

    @Expose( serialize = false, deserialize = false )
    private String parent;

    private Integer level;

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getShortName()
    {
        return shortName;
    }

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    public String getOpeningDate()
    {
        return openingDate;
    }

    public void setOpeningDate( String openingDate )
    {
        this.openingDate = openingDate;
    }

    public String getParent()
    {
        return parent;
    }

    public void setParent( String parent )
    {
        this.parent = parent;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel( Integer level )
    {
        this.level = level;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }
}
