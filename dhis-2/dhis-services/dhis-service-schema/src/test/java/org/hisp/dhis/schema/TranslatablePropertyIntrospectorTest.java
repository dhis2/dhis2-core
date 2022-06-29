/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.schema.introspection.TranslatablePropertyIntrospector;
import org.junit.jupiter.api.Test;

class TranslatablePropertyIntrospectorTest
{

    private final TranslatablePropertyIntrospector introspector = new TranslatablePropertyIntrospector();

    @Test
    void testGetTranslatableProperties()
    {
        Property propTranslation = new Property( DataElement.class );
        propTranslation.setName( "translations" );
        propTranslation.setFieldName( "translations" );
        propTranslation.setPersisted( true );
        Property propName = new Property( DataElement.class );
        propName.setName( "name" );
        propName.setFieldName( "name" );
        propName.setPersisted( true );
        Property propCode = new Property( DataElement.class );
        propCode.setName( "code" );
        propCode.setFieldName( "code" );
        propCode.setPersisted( true );
        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put( "name", propName );
        propertyMap.put( "code", propCode );
        propertyMap.put( "translations", propTranslation );
        assertFalse( propertyMap.get( "name" ).isTranslatable() );
        introspector.introspect( DataElement.class, propertyMap );
        assertTrue( propertyMap.get( "name" ).isTranslatable() );
        assertFalse( propertyMap.get( "code" ).isTranslatable() );
    }

    /**
     * If an object doesn't have column translations in database, then none of
     * properties will have translations = true even if it is marked with
     * annotation Translatable
     */
    @Test
    void testNonTranslatableObject()
    {
        Property propName = new Property( DataElement.class );
        propName.setName( "name" );
        propName.setPersisted( true );
        Property propCode = new Property( DataElement.class );
        propCode.setName( "code" );
        propCode.setPersisted( true );
        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put( "name", propName );
        propertyMap.put( "code", propCode );
        assertFalse( propertyMap.get( "name" ).isTranslatable() );
        introspector.introspect( DataElement.class, propertyMap );
        assertFalse( propertyMap.get( "name" ).isTranslatable() );
        assertFalse( propertyMap.get( "code" ).isTranslatable() );
    }

    @Test
    void testI18nTranslationKey()
    {
        Property propTranslation = new Property( DataSet.class );
        propTranslation.setName( "translations" );
        propTranslation.setFieldName( "translations" );
        propTranslation.setPersisted( true );
        Property property = new Property( DataSet.class );
        property.setFieldName( "formName" );
        property.setName( "formName" );
        property.setPersisted( true );
        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put( "formName", property );
        propertyMap.put( "translations", propTranslation );
        introspector.introspect( DataSet.class, propertyMap );
        assertEquals( "form_name", propertyMap.get( "formName" ).getI18nTranslationKey() );
    }

    @Test
    void testTranslatableEmbeddedProperty()
    {
        Property propTranslation = new Property( Predictor.class );
        propTranslation.setName( "translations" );
        propTranslation.setFieldName( "translations" );
        propTranslation.setPersisted( true );
        Property propGenerator = new Property( Predictor.class );
        propGenerator.setName( "generator" );
        propGenerator.setFieldName( "generator" );
        propGenerator.setEmbeddedObject( true );
        propGenerator.setPersisted( true );
        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put( "generator", propGenerator );
        propertyMap.put( "translations", propTranslation );
        assertFalse( propertyMap.get( "generator" ).isTranslatable() );
        introspector.introspect( Predictor.class, propertyMap );
        assertTrue( propertyMap.get( "generator" ).isTranslatable() );
    }

    @Test
    void testNotPersistedProperty()
    {
        Property propTranslation = createProperty( ProgramStageSection.class, "translations" );
        propTranslation.setPersisted( true );

        Property propShortName = createProperty( ProgramStageSection.class, "shortName" );
        propShortName.setPersisted( false );

        Map<String, Property> propertyMap = new HashMap<>();
        propertyMap.put( "shortName", propShortName );
        propertyMap.put( "translations", propTranslation );

        introspector.introspect( ProgramStageSection.class, propertyMap );

        assertFalse( propertyMap.get( "shortName" ).isTranslatable() );
    }

    private Property createProperty( Class klass, String name )
    {
        Property property = new Property( klass );
        property.setName( name );
        property.setFieldName( name );
        return property;
    }
}
