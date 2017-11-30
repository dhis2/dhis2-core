package org.hisp.dhis.security.acl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "data", namespace = DxfNamespaces.DXF_2_0 )
public class AccessData
{
    private boolean write;

    private boolean read;

    public AccessData()
    {
    }

    public AccessData( boolean read, boolean write )
    {
        this.read = read;
        this.write = write;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "write", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isWrite()
    {
        return write;
    }

    public void setWrite( boolean write )
    {
        this.write = write;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "read", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRead()
    {
        return read;
    }

    public void setRead( boolean read )
    {
        this.read = read;
    }
}
