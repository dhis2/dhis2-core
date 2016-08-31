package org.hisp.dhis.query;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryTest
{
    private Property createProperty( Class<?> klazz, String name, boolean simple, boolean persisted )
    {
        Property property = new Property( klazz );
        property.setName( name );
        property.setFieldName( name );
        property.setSimple( simple );
        property.setPersisted( persisted );

        return property;
    }

    private Schema createSchema()
    {
        Schema schema = new Schema( DataElement.class, "dataElement", "dataElements" );
        schema.addProperty( createProperty( String.class, "id", true, true ) );
        schema.addProperty( createProperty( String.class, "name", true, true ) );
        schema.addProperty( createProperty( String.class, "code", true, true ) );
        schema.addProperty( createProperty( Date.class, "created", true, true ) );
        schema.addProperty( createProperty( Date.class, "lastUpdated", true, true ) );

        schema.addProperty( createProperty( Integer.class, "int", true, true ) );
        schema.addProperty( createProperty( Long.class, "long", true, true ) );
        schema.addProperty( createProperty( Float.class, "float", true, true ) );
        schema.addProperty( createProperty( Double.class, "double", true, true ) );

        return schema;
    }

    @Test
    public void validRestrictionParameters()
    {
        Query query = Query.from( createSchema() );
        query.add( Restrictions.eq( "id", "anc" ) );
        query.add( Restrictions.like( "name", "anc", MatchMode.ANYWHERE ) );
        query.add( Restrictions.eq( "code", "anc" ) );

        assertEquals( 3, query.getCriterions().size() );
    }
}
