package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PeriodTypeObjectBundleHook extends AbstractObjectBundleHook
{
    @Autowired
    private PeriodService periodService;

    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        for ( Property property : schema.getPropertyMap().values() )
        {
            if ( PeriodType.class.isAssignableFrom( property.getKlass() ) )
            {
                PeriodType periodType = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

                if ( periodType != null )
                {
                    periodType = bundle.getPreheat().getPeriodTypeMap().get( periodType.getName() );
                    periodType = periodService.reloadPeriodType( periodType );
                    ReflectionUtils.invokeMethod( object, property.getSetterMethod(), periodType );
                }
            }
        }
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        for ( Property property : schema.getPropertyMap().values() )
        {
            if ( PeriodType.class.isAssignableFrom( property.getKlass() ) )
            {
                PeriodType periodType = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

                if ( periodType != null )
                {
                    periodType = bundle.getPreheat().getPeriodTypeMap().get( periodType.getName() );
                    ReflectionUtils.invokeMethod( object, property.getSetterMethod(), periodType );
                }
            }
        }
    }
}