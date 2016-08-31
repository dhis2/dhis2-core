package org.hisp.dhis.schema.descriptors;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class TrackedEntitySchemaDescriptor implements SchemaDescriptor
{
    public static final String SINGULAR = "trackedEntity";

    public static final String PLURAL = "trackedEntities";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( TrackedEntity.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1480 );

        schema.getAuthorities().add( new Authority( AuthorityType.CREATE, Lists.newArrayList( "F_TRACKED_ENTITY_ADD" ) ) );
        schema.getAuthorities().add( new Authority( AuthorityType.UPDATE, Lists.newArrayList( "F_TRACKED_ENTITY_UPDATE" ) ) );
        schema.getAuthorities().add( new Authority( AuthorityType.DELETE, Lists.newArrayList( "F_TRACKED_ENTITY_DELETE" ) ) );

        return schema;
    }
}
