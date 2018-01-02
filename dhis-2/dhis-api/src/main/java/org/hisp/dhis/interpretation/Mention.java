package org.hisp.dhis.interpretation;

import java.io.Serializable;

import java.util.Date;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "mentions", namespace = DxfNamespaces.DXF_2_0 )
public class Mention implements Serializable
{

    private String username;
    
    private String userUid;
    
    private Date created;

    @JsonProperty( value = "userId" )
    @JacksonXmlProperty( localName = "userId")
    public String getUserUid()
    {
        return userUid;
    }

    public void setUserUid( String userUid )
    {
        this.userUid = userUid;
    }
    
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }
}