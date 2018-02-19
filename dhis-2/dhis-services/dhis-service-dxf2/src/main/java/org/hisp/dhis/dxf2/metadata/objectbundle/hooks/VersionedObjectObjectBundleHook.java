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
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles increase of version for objects such as data set and option set.
 * 
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class VersionedObjectObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( VersionedObject.class.isInstance( object ) )
        {
            VersionedObject versionObj = (VersionedObject) object;
            int persistedVersion = ( ( VersionedObject ) persistedObject ).getVersion();

            versionObj.setVersion( persistedVersion > versionObj.getVersion() ? persistedVersion :
                persistedVersion < versionObj.getVersion() ? versionObj.getVersion() : versionObj.increaseVersion() );
        }
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        VersionedObject versionedObject = null;
        
        if ( Section.class.isInstance( persistedObject ) )
        {
            versionedObject = ((Section) persistedObject).getDataSet();
        }
        else if ( Option.class.isInstance( persistedObject ) )
        {
            versionedObject = ((Option) persistedObject).getOptionSet();
        }

        if ( versionedObject != null )
        {
            versionedObject.increaseVersion();
            sessionFactory.getCurrentSession().save( versionedObject );
        }
    }

    @Override
    public <T extends IdentifiableObject> void postTypeImport( Class<? extends IdentifiableObject> klass, List<T> objects, ObjectBundle bundle )
    {
        if ( Section.class.isAssignableFrom( klass ) )
        {
            Set<DataSet> dataSets = new HashSet<>();
            objects.forEach( o ->
            {
                DataSet dataSet = ((Section) o).getDataSet();

                if ( dataSet != null && dataSet.getId() > 0 )
                {
                    dataSets.add( dataSet );
                }
            } );

            dataSets.forEach( ds ->
            {
                ds.increaseVersion();
                sessionFactory.getCurrentSession().save( ds );
            } );
        }
        else if ( Option.class.isAssignableFrom( klass ) )
        {
            Set<OptionSet> optionSets = new HashSet<>();

            objects.forEach( o ->
            {
                Option option = (Option) o;

                if ( option.getOptionSet() != null && option.getId() > 0 )
                {
                    optionSets.add( option.getOptionSet() );
                }
            } );

            optionSets.forEach( os ->
            {
                os.increaseVersion();
                sessionFactory.getCurrentSession().save( os );
            } );
        }
    }
}
