package org.hisp.dhis.attribute;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.exception.MissingMandatoryAttributeValueException;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class DefaultAttributeService
    implements AttributeService
{
    private static final Predicate<AttributeValue> SHOULD_DELETE_ON_UPDATE =
        ( attributeValue ) ->
            attributeValue.getValue() == null && attributeValue.getAttribute().getValueType() == ValueType.TRUE_ONLY;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private AttributeStore attributeStore;

    public void setAttributeStore( AttributeStore attributeStore )
    {
        this.attributeStore = attributeStore;
    }

    private AttributeValueStore attributeValueStore;

    public void setAttributeValueStore( AttributeValueStore attributeValueStore )
    {
        this.attributeValueStore = attributeValueStore;
    }

    @Autowired
    private IdentifiableObjectManager manager;

    // -------------------------------------------------------------------------
    // Attribute implementation
    // -------------------------------------------------------------------------

    @Override
    public void addAttribute( Attribute attribute )
    {
        attributeStore.save( attribute );
    }

    @Override
    public void updateAttribute( Attribute attribute )
    {
        attributeStore.update( attribute );
    }

    @Override
    public void deleteAttribute( Attribute attribute )
    {
        attributeStore.delete( attribute );
    }

    @Override
    public Attribute getAttribute( int id )
    {
        return attributeStore.get( id );
    }

    @Override
    public Attribute getAttribute( String uid )
    {
        return attributeStore.getByUid( uid );
    }

    @Override
    public Attribute getAttributeByName( String name )
    {
        return attributeStore.getByName( name );
    }

    @Override
    public Attribute getAttributeByCode( String code )
    {
        return attributeStore.getByCode( code );
    }

    @Override
    public List<Attribute> getAllAttributes()
    {
        return new ArrayList<>( attributeStore.getAll() );
    }

    @Override
    public List<Attribute> getAttributes( Class<?> klass )
    {
        return new ArrayList<>( attributeStore.getAttributes( klass ) );
    }

    @Override
    public List<Attribute> getMandatoryAttributes( Class<?> klass )
    {
        return new ArrayList<>( attributeStore.getMandatoryAttributes( klass ) );
    }

    @Override
    public List<Attribute> getUniqueAttributes( Class<?> klass )
    {
        return new ArrayList<>( attributeStore.getUniqueAttributes( klass ) );
    }

    // -------------------------------------------------------------------------
    // AttributeValue implementation
    // -------------------------------------------------------------------------

    @Override
    public <T extends IdentifiableObject> void addAttributeValue( T object, AttributeValue attributeValue ) throws NonUniqueAttributeValueException
    {
        if ( object == null || attributeValue == null || attributeValue.getAttribute() == null ||
            !attributeValue.getAttribute().getSupportedClasses().contains( object.getClass() ) )
        {
            return;
        }

        if ( attributeValue.getAttribute().isUnique() )
        {
            List<AttributeValue> values = manager.getAttributeValueByAttributeAndValue( object.getClass(), attributeValue.getAttribute(), attributeValue.getValue() );

            if ( !values.isEmpty() )
            {
                throw new NonUniqueAttributeValueException( attributeValue );
            }
        }

        attributeValue.setAutoFields();
        attributeValueStore.save( attributeValue );
        object.getAttributeValues().add( attributeValue );
    }

    @Override
    public <T extends IdentifiableObject> void updateAttributeValue( T object, AttributeValue attributeValue ) throws NonUniqueAttributeValueException
    {
        if ( object == null || attributeValue == null || attributeValue.getAttribute() == null ||
            !attributeValue.getAttribute().getSupportedClasses().contains( object.getClass() ) )
        {
            return;
        }

        if ( attributeValue.getAttribute().isUnique() )
        {
            List<AttributeValue> values = manager.getAttributeValueByAttributeAndValue( object.getClass(), attributeValue.getAttribute(), attributeValue.getValue() );

            if ( values.size() > 1 || (values.size() == 1 && !object.getAttributeValues().contains( values.get( 0 ) )) )
            {
                throw new NonUniqueAttributeValueException( attributeValue );
            }
        }

        attributeValue.setAutoFields();
        attributeValueStore.update( attributeValue );
        object.getAttributeValues().add( attributeValue );
    }

    @Override
    public void deleteAttributeValue( AttributeValue attributeValue )
    {
        attributeValueStore.delete( attributeValue );
    }

    @Override
    public AttributeValue getAttributeValue( int id )
    {
        return attributeValueStore.get( id );
    }

    @Override
    public List<AttributeValue> getAllAttributeValues()
    {
        return new ArrayList<>( attributeValueStore.getAll() );
    }

    @Override
    public List<AttributeValue> getAllAttributeValuesByAttributes( List<Attribute> attributes )
    {
        return attributeValueStore.getAllByAttributes( attributes );
    }

    @Override
    public List<AttributeValue> getAllAttributeValuesByAttribute( Attribute attribute )
    {
        return attributeValueStore.getAllByAttribute( attribute );
    }

    @Override
    public List<AttributeValue> getAllAttributeValuesByAttributeAndValue( Attribute attribute, String value )
    {
        return attributeValueStore.getAllByAttributeAndValue( attribute, value );
    }

    @Override
    public <T extends IdentifiableObject> boolean isAttributeValueUnique( T object, AttributeValue attributeValue )
    {
        return attributeValueStore.isAttributeValueUnique( object, attributeValue );
    }

    @Override
    public int getAttributeValueCount()
    {
        return attributeValueStore.getCount();
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validateAttributeValues( T object, Set<AttributeValue> attributeValues )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( attributeValues.isEmpty() )
        {
            return errorReports;
        }

        Map<String, AttributeValue> attributeValueMap = attributeValues.stream()
            .filter( av -> av.getAttribute() != null )
            .collect( Collectors.toMap( av -> av.getAttribute().getUid(), av -> av ) );

        Iterator<AttributeValue> iterator = object.getAttributeValues().iterator();
        List<Attribute> mandatoryAttributes = getMandatoryAttributes( object.getClass() );

        while ( iterator.hasNext() )
        {
            AttributeValue attributeValue = iterator.next();

            if ( attributeValue.getAttribute() != null && attributeValueMap.containsKey( attributeValue.getAttribute().getUid() ) )
            {
                AttributeValue av = attributeValueMap.get( attributeValue.getAttribute().getUid() );

                if ( attributeValue.isUnique() )
                {
                    if ( !manager.isAttributeValueUnique( object.getClass(), object, attributeValue.getAttribute(), av.getValue() ) )
                    {
                        errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E4009, attributeValue.getAttribute().getUid(), av.getValue() ) );
                    }
                }

                attributeValueMap.remove( attributeValue.getAttribute().getUid() );
                mandatoryAttributes.remove( attributeValue.getAttribute() );
            }
        }

        for ( String uid : attributeValueMap.keySet() )
        {
            AttributeValue attributeValue = attributeValueMap.get( uid );

            if ( attributeValue.getAttribute() != null && !attributeValue.getAttribute().getSupportedClasses().contains( object.getClass() ) )
            {
                errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E4010, attributeValue.getAttribute().getUid(), object.getClass().getSimpleName() ) );
            }
            else
            {
                mandatoryAttributes.remove( attributeValue.getAttribute() );
            }
        }

        mandatoryAttributes.forEach( att -> errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E4011, att.getUid() ) ) );

        return errorReports;
    }

    @Override
    public <T extends IdentifiableObject> void updateAttributeValues( T object, List<String> jsonAttributeValues ) throws Exception
    {
        updateAttributeValues( object, getJsonAttributeValues( jsonAttributeValues ) );
    }

    @Override
    public <T extends IdentifiableObject> void updateAttributeValues( T object, Set<AttributeValue> attributeValues ) throws Exception
    {
        if ( attributeValues.isEmpty() )
        {
            return;
        }

        Set<AttributeValue> toBeDeleted = attributeValues.stream()
            .filter( SHOULD_DELETE_ON_UPDATE )
            .collect( Collectors.toSet() );

        Map<String, AttributeValue> attributeValueMap = attributeValues.stream()
            .filter( SHOULD_DELETE_ON_UPDATE.negate() )
            .collect( Collectors.toMap( av -> av.getAttribute().getUid(), av -> av ) );

        Iterator<AttributeValue> iterator = object.getAttributeValues().iterator();

        List<Attribute> mandatoryAttributes = getMandatoryAttributes( object.getClass() );

        while ( iterator.hasNext() )
        {
            AttributeValue attributeValue = iterator.next();

            if ( attributeValueMap.containsKey( attributeValue.getAttribute().getUid() ) )
            {
                AttributeValue av = attributeValueMap.get( attributeValue.getAttribute().getUid() );

                if ( attributeValue.isUnique() )
                {
                    if ( manager.isAttributeValueUnique( object.getClass(), object, attributeValue.getAttribute(), av.getValue() ) )
                    {
                        attributeValue.setValue( av.getValue() );
                    }
                    else
                    {
                        throw new NonUniqueAttributeValueException( attributeValue, av.getValue() );
                    }
                }
                else
                {
                    attributeValue.setValue( av.getValue() );
                }

                attributeValueMap.remove( attributeValue.getAttribute().getUid() );
                mandatoryAttributes.remove( attributeValue.getAttribute() );
            }
            else
            {
                iterator.remove();
            }
        }

        for ( String uid : attributeValueMap.keySet() )
        {
            AttributeValue attributeValue = attributeValueMap.get( uid );
            addAttributeValue( object, attributeValue );
            mandatoryAttributes.remove( attributeValue.getAttribute() );
        }

        for ( AttributeValue attributeValue : toBeDeleted )
        {
            mandatoryAttributes.remove( attributeValue.getAttribute() );
            deleteAttributeValue( attributeValue );
        }

        if ( !mandatoryAttributes.isEmpty() )
        {
            throw new MissingMandatoryAttributeValueException( mandatoryAttributes );
        }
    }

    //--------------------------------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------------------------------

    private Set<AttributeValue> getJsonAttributeValues( List<String> jsonAttributeValues )
        throws IOException
    {
        Set<AttributeValue> attributeValues = new HashSet<>();

        Map<Integer, String> attributeValueMap = jsonToMap( jsonAttributeValues );

        for ( Map.Entry<Integer, String> entry : attributeValueMap.entrySet() )
        {
            int id = entry.getKey();
            String value = entry.getValue();

            Attribute attribute = getAttribute( id );

            if ( attribute == null )
            {
                continue;
            }

            AttributeValue attributeValue = parseAttributeValue( attribute, value );

            if ( attributeValue == null )
            {
                continue;
            }

            attributeValues.add( attributeValue );
        }

        return attributeValues;
    }

    /**
     * Parse and create AttributeValue from attribute, id and string value.
     * Sets null for all non-"true" TRUE_ONLY AttributeValues.
     */
    private AttributeValue parseAttributeValue( Attribute attribute, String value )
    {
        AttributeValue attributeValue = null;

        if ( attribute.getValueType() == ValueType.TRUE_ONLY )
        {
            value = !StringUtils.isEmpty( value ) && "true".equalsIgnoreCase( value ) ? "true" : null;

            attributeValue = new AttributeValue( value, attribute );
        }
        else if ( !StringUtils.isEmpty( value ) )
        {
            attributeValue = new AttributeValue( value, attribute );
        }

        return attributeValue;
    }

    /**
     * Parses raw JSON into a map of ID -> Value.
     * Allows null and empty values (must be handled later).
     */
    private Map<Integer, String> jsonToMap( List<String> jsonAttributeValues )
        throws IOException
    {
        Map<Integer, String> parsed = Maps.newHashMap();

        ObjectMapper mapper = new ObjectMapper();

        for ( String jsonString : jsonAttributeValues )
        {
            JsonNode node = mapper.readValue( jsonString, JsonNode.class );

            JsonNode nId = node.get( "id" );
            JsonNode nValue = node.get( "value" );

            if ( nId == null || nId.isNull() )
            {
                continue;
            }

            parsed.put( nId.asInt(), nValue.asText() );
        }

        return parsed;
    }
}
