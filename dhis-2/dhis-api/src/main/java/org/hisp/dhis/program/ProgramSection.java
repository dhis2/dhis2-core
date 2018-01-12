package org.hisp.dhis.program;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "programSection", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramSection
    extends BaseIdentifiableObject
    implements MetadataObject
{
    private String description;

    private ProgramStage programStage;

    private List<Attribute> attributes = new ArrayList<>();

    private Integer sortOrder;

    private ObjectStyle style;

    private String formName;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramSection()
    {
    }

    public ProgramSection( String name, List<Attribute> attributes )
    {
        this.name = name;
        this.attributes = attributes;
    }

    public ProgramSection( String name, List<Attribute> attributes, Integer sortOrder )
    {
        this( name, attributes );
        this.sortOrder = sortOrder;
    }
}
