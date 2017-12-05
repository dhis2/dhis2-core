package org.hisp.dhis.keyjsonvalue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Dog
{
    private String id;
    
    private String name;
    
    private String color;
    
    public Dog()
    {
    }
    
    public Dog( String id, String name, String color )
    {
        this.id = id;
        this.name = name;
        this.color = color;
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
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public String getColor()
    {
        return color;
    }

    public void setColor( String color )
    {
        this.color = color;
    }
}
