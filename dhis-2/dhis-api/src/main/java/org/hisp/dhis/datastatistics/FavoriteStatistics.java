package org.hisp.dhis.datastatistics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * Created by yrjanaff on 20.05.2016.
 */
public class FavoriteStatistics
{
    private Integer position;
    private String name;
    private Integer views;
    private String id;
    private Date created;

    public FavoriteStatistics()
    {
    }

    @JsonProperty
    public Integer getPosition()
    {
        return position;
    }

    public void setPosition( Integer position )
    {
        this.position = position;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public Integer getViews()
    {
        return views;
    }

    public void setViews( Integer views )
    {
        this.views = views;
    }

    @JsonProperty
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @Override public String toString()
    {
        return "FavoriteStatistics{" +
            "position=" + position +
            ", name='" + name + '\'' +
            ", views=" + views +
            ", id='" + id + '\'' +
            ", created=" + created +
            '}';
    }
}
