package org.hisp.dhis.webapi.json.domain;

import java.util.List;

import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.JsonURL;

/**
 * Web API equivalent of a {@link org.hisp.dhis.schema.Property}.
 *
 * @author Jan Bernitt
 */
public interface JsonProperty extends JsonObject
{
    default Class<?> getKlass()
    {
        return getString( "klass" ).parsedClass();
    }

    default Class<?> getItemKlass()
    {
        return getString( "itemKlass" ).parsedClass();
    }

    default List<String> getConstants()
    {
        return getArray( "constants" ).stringValues();
    }

    default PropertyType getPropertyType()
    {
        return getString( "propertyType" ).parsed( PropertyType::valueOf );
    }

    default PropertyType getItemPropertyType()
    {
        return getString( "itemPropertyType" ).parsed( PropertyType::valueOf );
    }

    default String getFieldName()
    {
        return getString( "fieldName" ).string();
    }

    default String getName()
    {
        return getString( "name" ).string();
    }

    default String getCollectionName()
    {
        return getString( "collectionName" ).string();
    }

    default JsonURL getNamespace()
    {
        return get( "namespace", JsonURL.class );
    }

    default String getRelativeApiEndpoint()
    {
        return getString( "relativeApiEndpoint" ).string();
    }

    default Number getMin()
    {
        return getNumber( "min" ).number();
    }

    default Number getMax()
    {
        return getNumber( "max" ).number();
    }

    default boolean isSimple()
    {
        return getBoolean( "simple" ).booleanValue();
    }

    default boolean isRequired()
    {
        return getBoolean( "required" ).booleanValue();
    }

    default boolean isWritable()
    {
        return getBoolean( "writable" ).booleanValue();
    }

    default boolean isNameableObject()
    {
        return getBoolean( "nameableObject" ).booleanValue();
    }

    default boolean isOneToOne()
    {
        return getBoolean( "oneToOne" ).booleanValue();
    }

    default boolean isPropertyTransformer()
    {
        return getBoolean( "propertyTransformer" ).booleanValue();
    }

    default boolean isAttribute()
    {
        return getBoolean( "attribute" ).booleanValue();
    }

    default boolean isOwner()
    {
        return getBoolean( "owner" ).booleanValue();
    }

    default boolean isReadable()
    {
        return getBoolean( "readable" ).booleanValue();
    }

    default boolean isTranslatable()
    {
        return getBoolean( "translatable" ).booleanValue();
    }

    default boolean isOrdered()
    {
        return getBoolean( "ordered" ).booleanValue();
    }

    default boolean isIdentifiableObject()
    {
        return getBoolean( "identifiableObject" ).booleanValue();
    }

    default boolean isManyToMany()
    {
        return getBoolean( "manyToMany" ).booleanValue();
    }

    default boolean isCollection()
    {
        return getBoolean( "collection" ).booleanValue();
    }

    default boolean isCollectionWrapping()
    {
        return getBoolean( "collectionWrapping" ).booleanValue();
    }

    default boolean isAnalyticalObject()
    {
        return getBoolean( "analyticalObject" ).booleanValue();
    }

    default boolean isEmbeddedObject()
    {
        return getBoolean( "embeddedObject" ).booleanValue();
    }

    default boolean isUnique()
    {
        return getBoolean( "unique" ).booleanValue();
    }

    default boolean isPersisted()
    {
        return getBoolean( "persisted" ).booleanValue();
    }

    default boolean isManyToOne()
    {
        return getBoolean( "manyToOne" ).booleanValue();
    }

}
