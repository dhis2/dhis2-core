package org.hisp.dhis.dto.schemas;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class Schema
{
    private ArrayList<SchemaProperty> properties;

    private String plural;

    public String getPlural()
    {
        return plural;
    }

    public void setPlural( String plural )
    {
        this.plural = plural;
    }

    public ArrayList<SchemaProperty> getProperties()
    {
        return properties;
    }

    public void setProperties( ArrayList<SchemaProperty> properties )
    {
        this.properties = properties;
    }

    public List<SchemaProperty> getRequiredProperties()
    {
        return properties.stream()
            .filter( (schemaProperty -> schemaProperty.isRequired()) )
            .collect( Collectors.toList() );
    }
}
