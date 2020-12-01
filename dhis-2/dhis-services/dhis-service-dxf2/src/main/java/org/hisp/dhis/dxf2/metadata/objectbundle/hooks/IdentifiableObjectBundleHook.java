package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Order( 0 )
public class IdentifiableObjectBundleHook extends AbstractObjectBundleHook
{
    private final AclService aclService;

    public IdentifiableObjectBundleHook( AclService aclService )
    {
        checkNotNull( aclService );
        this.aclService = aclService;
    }

    @Override
    public void preCreate( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
        BaseIdentifiableObject baseIdentifiableObject = (BaseIdentifiableObject) identifiableObject;

        baseIdentifiableObject.setAutoFields();
        baseIdentifiableObject.setLastUpdatedBy( bundle.getUser() );

        if ( baseIdentifiableObject.getUser() == null )
        {
            baseIdentifiableObject.setUser( bundle.getUser() );
        }

        Schema schema = schemaService.getDynamicSchema( identifiableObject.getClass() );
        handleAttributeValues( identifiableObject, bundle, schema );
        handleSkipSharing( identifiableObject, bundle );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        BaseIdentifiableObject baseIdentifiableObject = (BaseIdentifiableObject) object;
        baseIdentifiableObject.setAutoFields();
        baseIdentifiableObject.setLastUpdatedBy( bundle.getUser() );

        if ( baseIdentifiableObject.getUser() == null )
        {
            baseIdentifiableObject.setUser( bundle.getUser() );
        }

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        handleAttributeValues( object, bundle, schema );
    }

    private void handleAttributeValues( IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema )
    {
        if ( !schema.havePersistedProperty( "attributeValues" ) ) return;

        Iterator<AttributeValue> iterator = identifiableObject.getAttributeValues().iterator();

        while ( iterator.hasNext() )
        {
            AttributeValue attributeValue = iterator.next();

            // if value null or empty, just skip it
            if ( StringUtils.isEmpty( attributeValue.getValue() ) )
            {
                iterator.remove();
                continue;
            }

            Attribute attribute = bundle.getPreheat().get( bundle.getPreheatIdentifier(), Attribute.class, attributeValue.getAttribute().getUid() );

            if ( attribute == null )
            {
                iterator.remove();
                continue;
            }

            attributeValue.setAttribute( attribute );
        }
    }

    private void handleSkipSharing( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
        if ( !bundle.isSkipSharing() ) return;

        aclService.clearSharing( identifiableObject, bundle.getUser() );
    }
}