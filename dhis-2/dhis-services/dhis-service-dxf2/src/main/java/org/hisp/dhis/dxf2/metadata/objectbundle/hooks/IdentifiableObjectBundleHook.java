/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.apache.commons.collections4.CollectionUtils;
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

<<<<<<< HEAD
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
<<<<<<< HEAD
=======

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    @Override
    public void preCreate( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
        ((BaseIdentifiableObject) identifiableObject).setAutoFields();

        BaseIdentifiableObject identifableObject = (BaseIdentifiableObject) identifiableObject;
        identifableObject.setAutoFields();
        identifableObject.setLastUpdatedBy( bundle.getUser() );

        Schema schema = schemaService.getDynamicSchema( identifiableObject.getClass() );
        handleAttributeValues( identifiableObject, bundle, schema );
        handleSkipSharing( identifiableObject, bundle );
<<<<<<< HEAD
=======
        handleSkipTranslation( identifiableObject, bundle );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        BaseIdentifiableObject identifiableObject = (BaseIdentifiableObject) object;
        identifiableObject.setAutoFields();
        identifiableObject.setLastUpdatedBy( bundle.getUser() );

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        handleAttributeValues( object, bundle, schema );
    }

    private void handleAttributeValues( IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema )
    {
        if ( !schema.havePersistedProperty( "attributeValues" ) )
            return;

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

<<<<<<< HEAD
            Attribute attribute =  bundle.getPreheat().get( bundle.getPreheatIdentifier(), Attribute.class, attributeValue.getAttribute().getUid() );
=======
            Attribute attribute = bundle.getPreheat().get( bundle.getPreheatIdentifier(), Attribute.class,
                attributeValue.getAttribute().getUid() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

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
<<<<<<< HEAD
        if ( !bundle.isSkipSharing() ) return;

        aclService.clearSharing( identifiableObject, bundle.getUser() );
    }
=======
        if ( !bundle.isSkipSharing() )
            return;

        aclService.clearSharing( identifiableObject, bundle.getUser() );
    }

    private void handleSkipTranslation( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
        if ( bundle.isSkipTranslation() && !CollectionUtils.isEmpty( identifiableObject.getTranslations() ) )
        {
            identifiableObject.getTranslations().clear();
        }
    }

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
}