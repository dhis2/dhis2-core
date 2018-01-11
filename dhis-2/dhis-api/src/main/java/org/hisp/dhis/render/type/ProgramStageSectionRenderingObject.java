package org.hisp.dhis.render.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;

public class ProgramStageSectionRenderingObject
{
    private ProgramStageSectionRenderType type;

    public ProgramStageSectionRenderingObject()
    {
        this.type = ProgramStageSectionRenderType.LISTING;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStageSectionRenderType getType()
    {
        return type;
    }

    public void setType( ProgramStageSectionRenderType type )
    {
        this.type = type;
    }
}
