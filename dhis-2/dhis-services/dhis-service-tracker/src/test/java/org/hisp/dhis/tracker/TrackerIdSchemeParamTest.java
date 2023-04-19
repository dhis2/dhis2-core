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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.tracker.imports.TrackerIdScheme.ATTRIBUTE;
import static org.hisp.dhis.tracker.imports.TrackerIdScheme.CODE;
import static org.hisp.dhis.tracker.imports.TrackerIdScheme.NAME;
import static org.hisp.dhis.tracker.imports.TrackerIdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.junit.jupiter.api.Test;

class TrackerIdSchemeParamTest
{

    @Test
    void toMetadataIdentifierUIDFromString()
    {

        MetadataIdentifier id = TrackerIdSchemeParam.UID.toMetadataIdentifier( "z3Z4TD3oBCP" );

        assertEquals( UID, id.getIdScheme() );
        assertEquals( "z3Z4TD3oBCP", id.getIdentifier() );
    }

    @Test
    void toMetadataIdentifierCodeFromString()
    {

        MetadataIdentifier id = TrackerIdSchemeParam.CODE.toMetadataIdentifier( "AB" );

        assertEquals( CODE, id.getIdScheme() );
        assertEquals( "AB", id.getIdentifier() );
    }

    @Test
    void toMetadataIdentifierAttributeFromString()
    {

        TrackerIdSchemeParam param = TrackerIdSchemeParam.ofAttribute( "z3Z4TD3oBCP" );

        MetadataIdentifier id = param.toMetadataIdentifier( "AB" );

        assertEquals( ATTRIBUTE, id.getIdScheme() );
        assertEquals( "z3Z4TD3oBCP", id.getIdentifier() );
        assertEquals( "AB", id.getAttributeValue() );
    }

    @Test
    void toMetadataIdentifierAttributeFromStringNull()
    {

        TrackerIdSchemeParam param = TrackerIdSchemeParam.ofAttribute( "z3Z4TD3oBCP" );

        MetadataIdentifier id = param.toMetadataIdentifier( (String) null );

        assertEquals( ATTRIBUTE, id.getIdScheme() );
        assertEquals( "z3Z4TD3oBCP", id.getIdentifier() );
        assertNull( id.getAttributeValue() );
    }

    @Test
    void toMetadataIdentifierUIDFromIdentifiableObject()
    {

        Program program = new Program();
        program.setUid( "z3Z4TD3oBCP" );
        MetadataIdentifier id = TrackerIdSchemeParam.UID.toMetadataIdentifier( program );

        assertEquals( UID, id.getIdScheme() );
        assertEquals( "z3Z4TD3oBCP", id.getIdentifier() );
    }

    @Test
    void toMetadataIdentifierUIDFromIdentifiableObjectNull()
    {

        MetadataIdentifier id = TrackerIdSchemeParam.UID.toMetadataIdentifier( (IdentifiableObject) null );

        assertEquals( UID, id.getIdScheme() );
        assertNull( id.getIdentifier() );
    }

    @Test
    void toMetadataIdentifierNameFromIdentifiableObject()
    {

        Program program = new Program( "programA" );
        MetadataIdentifier id = TrackerIdSchemeParam.NAME.toMetadataIdentifier( program );

        assertEquals( NAME, id.getIdScheme() );
        assertEquals( "programA", id.getIdentifier() );
    }

    @Test
    void toMetadataIdentifierAttributeFromIdentifiableObject()
    {

        TrackerIdSchemeParam param = TrackerIdSchemeParam.ofAttribute( "z3Z4TD3oBCP" );
        Program program = new Program();
        Attribute att = new Attribute( "z3Z4TD3oBCP" );
        program.setAttributeValues(
            Set.of( new AttributeValue( att, "sunshine" ), attributeValue( "grass" ), attributeValue( "rocks" ) ) );

        MetadataIdentifier id = param.toMetadataIdentifier( program );

        assertEquals( ATTRIBUTE, id.getIdScheme() );
        assertEquals( "z3Z4TD3oBCP", id.getIdentifier() );
        assertEquals( "sunshine", id.getAttributeValue() );
    }

    @Test
    void toMetadataIdentifierAttributeFromIdentifiableObjectNull()
    {

        TrackerIdSchemeParam param = TrackerIdSchemeParam.ofAttribute( "z3Z4TD3oBCP" );

        MetadataIdentifier id = param.toMetadataIdentifier( (IdentifiableObject) null );

        assertEquals( ATTRIBUTE, id.getIdScheme() );
        assertEquals( "z3Z4TD3oBCP", id.getIdentifier() );
        assertNull( id.getAttributeValue() );
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