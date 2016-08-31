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

import net.sf.json.JSONObject;
import org.hisp.dhis.attribute.exception.MissingMandatoryAttributeValueException;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.validation.ValidationViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.i18n.I18nUtils.i18n;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class DefaultAttributeService
    implements AttributeService
{
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

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
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
        return i18n( i18nService, attributeStore.get( id ) );
    }

    @Override
    public Attribute getAttribute( String uid )
    {
        return i18n( i18nService, attributeStore.getByUid( uid ) );
    }

    @Override
    public Attribute getAttributeByName( String name )
    {
        return i18n( i18nService, attributeStore.getByName( name ) );
    }

    @Override
    public Attribute getAttributeByCode( String code )
    {
        return i18n( i18nService, attributeStore.getByCode( code ) );
    }

    @Override
    public List<Attribute> getAllAttributes()
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAll() ) );
    }

    @Override
    public List<Attribute> getAttributes( Class<?> klass )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAttributes( klass ) ) );
    }

    @Override
    public List<Attribute> getMandatoryAttributes( Class<?> klass )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getMandatoryAttributes( klass ) ) );
    }

    @Override
    public List<Attribute> getUniqueAttributes( Class<?> klass )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getUniqueAttributes( klass ) ) );
    }

    @Override
    public int getAttributeCount()
    {
        return attributeStore.getCount();
    }

    @Override
    public int getAttributeCountByName( String name )
    {
        return attributeStore.getCountLikeName( name );
    }

    @Override
    public List<Attribute> getAttributesBetween( int first, int max )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAllOrderedName( first, max ) ) );
    }

    @Override
    public List<Attribute> getAttributesBetweenByName( String name, int first, int max )
    {
        return new ArrayList<>( i18n( i18nService, attributeStore.getAllLikeName( name, first, max ) ) );
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
    public <T extends IdentifiableObject> List<ValidationViolation> validateAttributeValues( T object, Set<AttributeValue> attributeValues )
    {
        List<ValidationViolation> validationViolations = new ArrayList<>();

        if ( attributeValues.isEmpty() )
        {
            return validationViolations;
        }

        Map<String, AttributeValue> attributeValueMap = attributeValues.stream()
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
                    if ( !manager.isAttributeValueUnique( object.getClass(), object, attributeValue.getAttribute(), av.getValue() ) )
                    {
                        validationViolations.add( new ValidationViolation( attributeValue.getAttribute().getUid(),
                            "Value '" + av.getValue() + "' already exists for attribute '"
                                + attributeValue.getAttribute().getDisplayName() + "' (" + attributeValue.getAttribute().getUid() + ")" ) );
                    }
                }

                attributeValueMap.remove( attributeValue.getAttribute().getUid() );
                mandatoryAttributes.remove( attributeValue.getAttribute() );
            }
        }

        for ( String uid : attributeValueMap.keySet() )
        {
            AttributeValue attributeValue = attributeValueMap.get( uid );

            if ( !attributeValue.getAttribute().getSupportedClasses().contains( object.getClass() ) )
            {
                validationViolations.add( new ValidationViolation( attributeValue.getAttribute().getUid(),
                    "Attribute '" + attributeValue.getAttribute().getDisplayName() + "' (" + attributeValue.getAttribute().getUid() + ") is not supported for type "
                        + object.getClass().getSimpleName() ) );
            }
            else
            {
                mandatoryAttributes.remove( attributeValue.getAttribute() );
            }
        }

        mandatoryAttributes.stream()
            .forEach( att -> validationViolations.add(
                new ValidationViolation( att.getUid(), "Missing mandatory attribute '" + att.getDisplayName() + "' (" + att.getUid() + ")" ) ) );

        return validationViolations;
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

        Map<String, AttributeValue> attributeValueMap = attributeValues.stream()
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

        if ( !mandatoryAttributes.isEmpty() )
        {
            throw new MissingMandatoryAttributeValueException( mandatoryAttributes );
        }
    }

    //--------------------------------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------------------------------

    private Set<AttributeValue> getJsonAttributeValues( List<String> jsonAttributeValues )
    {
        Set<AttributeValue> attributeValues = new HashSet<>();

        for ( String jsonValue : jsonAttributeValues )
        {
            JSONObject json = JSONObject.fromObject( jsonValue );
            Integer id = json.getInt( "id" );
            String value = json.getString( "value" );

            Attribute attribute = getAttribute( id );

            if ( attribute == null || StringUtils.isEmpty( value ) )
            {
                continue;
            }

            AttributeValue attributeValue = new AttributeValue( value, attribute );
            attributeValue.setId( id );

            attributeValues.add( attributeValue );
        }

        return attributeValues;
    }
}
