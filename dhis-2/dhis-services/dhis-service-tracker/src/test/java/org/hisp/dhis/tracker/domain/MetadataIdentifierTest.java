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
package org.hisp.dhis.tracker.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

class MetadataIdentifierTest
{

    @Test
    void isEqualToNull()
    {

        Program p = new Program();
        p.setUid( CodeGenerator.generateUid() );

        MetadataIdentifier id = MetadataIdentifier.ofUid( p.getUid() );

        assertFalse( id.isEqualTo( null ) );
    }

    @Test
    void isEqualToUID()
    {

        Program p = new Program();
        p.setUid( CodeGenerator.generateUid() );

        MetadataIdentifier id = MetadataIdentifier.ofUid( p.getUid() );

        assertTrue( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToUIDWithDifferentUID()
    {

        Program p = new Program();
        p.setUid( CodeGenerator.generateUid() );

        MetadataIdentifier id = MetadataIdentifier.ofUid( CodeGenerator.generateUid() );

        assertFalse( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToUIDWithNull()
    {

        Program p = new Program();
        p.setUid( CodeGenerator.generateUid() );

        MetadataIdentifier id = MetadataIdentifier.ofUid( null );

        assertFalse( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToCode()
    {

        Program p = new Program();
        p.setCode( "PROGRAM" );

        MetadataIdentifier id = MetadataIdentifier.ofCode( "PROGRAM" );

        assertTrue( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToCodeWithDifferentCode()
    {

        Program p = new Program();
        p.setName( "PROGRAM" );

        MetadataIdentifier id = MetadataIdentifier.ofCode( "PROGRAMB" );

        assertFalse( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToName()
    {

        Program p = new Program();
        p.setName( "program" );

        MetadataIdentifier id = MetadataIdentifier.ofName( "program" );

        assertTrue( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToNameWithDifferentName()
    {

        Program p = new Program();
        p.setName( "program" );

        MetadataIdentifier id = MetadataIdentifier.ofName( "programB" );

        assertFalse( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToNameWithNull()
    {

        Program p = new Program();
        p.setName( "program" );

        MetadataIdentifier id = MetadataIdentifier.ofName( null );

        assertFalse( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToAttribute()
    {

        Program p = new Program();
        Attribute att = new Attribute();
        att.setUid( CodeGenerator.generateUid() );
        p.setAttributeValues( Set.of( new AttributeValue( att, "sunshine" ) ) );

        MetadataIdentifier id = MetadataIdentifier.ofAttribute( att.getUid(), "sunshine" );

        assertTrue( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToAttributeWithDifferentUID()
    {

        Program p = new Program();
        p.setAttributeValues(
            Set.of( attributeValue( "sunshine" ), attributeValue( "grass" ), attributeValue( "rocks" ) ) );

        MetadataIdentifier id = MetadataIdentifier.ofAttribute( CodeGenerator.generateUid(), "sunshine" );

        assertFalse( id.isEqualTo( p ) );
    }

    @Test
    void isEqualToAttributeWithDifferentValue()
    {

        Program p = new Program();
        Attribute att = new Attribute();
        att.setUid( CodeGenerator.generateUid() );
        p.setAttributeValues(
            Set.of( new AttributeValue( att, "sunshine" ), attributeValue( "grass" ), attributeValue( "rocks" ) ) );

        MetadataIdentifier id = MetadataIdentifier.ofAttribute( att.getUid(), "clouds" );

        assertFalse( id.isEqualTo( p ) );
    }

    private AttributeValue attributeValue( String value )
    {
        return attributeValue( CodeGenerator.generateUid(), value );
    }

    private AttributeValue attributeValue( String uid, String value )
    {
        return new AttributeValue( new Attribute( uid ), value );
    }
}