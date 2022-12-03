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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.Iterator;

import lombok.AllArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Order( 0 )
@AllArgsConstructor
public class IdentifiableObjectBundleHook extends AbstractObjectBundleHook<IdentifiableObject>
{
    private final AclService aclService;

    @Override
    public void preCreate( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
        BaseIdentifiableObject baseIdentifiableObject = (BaseIdentifiableObject) identifiableObject;

        baseIdentifiableObject.setAutoFields();
        baseIdentifiableObject.setLastUpdatedBy( bundle.getUser() );

        if ( baseIdentifiableObject.getCreatedBy() == null )
        {
            baseIdentifiableObject.setCreatedBy( bundle.getUser() );
        }

        if ( baseIdentifiableObject.getSharing().getOwner() == null )
        {
            baseIdentifiableObject.getSharing()
                .setOwner( baseIdentifiableObject.getCreatedBy() );
        }

        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( identifiableObject ) );
        handleAttributeValues( identifiableObject, bundle, schema );
        handleSkipSharing( identifiableObject, bundle );
        handleSkipTranslation( identifiableObject, bundle );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        BaseIdentifiableObject baseIdentifiableObject = (BaseIdentifiableObject) object;
        baseIdentifiableObject.setAutoFields();
        baseIdentifiableObject.setLastUpdatedBy( bundle.getUser() );

        handleCreatedByProperty( object, persistedObject, bundle );

        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) );
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

            Attribute attribute = bundle.getPreheat().get( bundle.getPreheatIdentifier(), Attribute.class,
                attributeValue.getAttribute().getUid() );

            if ( attribute == null )
            {
                iterator.remove();
                continue;
            }

            attributeValue.setAttribute( attribute );

            attributeValue.setValue( attributeValue.getValue().replaceAll( "\\s{2,}", StringUtils.SPACE ) );
        }
    }

    /**
     * Make sure createdBy is immutable unless persistedObject.createdBy is
     * null.
     *
     * @param object object from payload.
     * @param persistedObject persistedObject.
     * @param bundle The {@link ObjectBundle} of the importing service.
     */
    private void handleCreatedByProperty( IdentifiableObject object, IdentifiableObject persistedObject,
        ObjectBundle bundle )
    {
        if ( persistedObject.getCreatedBy() == null )
        {
            object.setCreatedBy( object.getCreatedBy() != null ? object.getCreatedBy() : bundle.getUser() );
        }
        else if ( !IdentifiableObjectUtils.equalByUID( object.getCreatedBy(), persistedObject.getCreatedBy() ) )
        {
            object.setCreatedBy( persistedObject.getCreatedBy() );
            bundle.getPreheat().put( PreheatIdentifier.UID, object.getCreatedBy() );
        }
    }

    private void handleSkipSharing( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
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

}
