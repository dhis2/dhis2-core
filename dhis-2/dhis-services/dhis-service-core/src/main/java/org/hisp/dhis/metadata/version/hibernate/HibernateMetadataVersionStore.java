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

package org.hisp.dhis.metadata.version.hibernate;

import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Projections;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionStore;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Implementation of MetadataVersionStore.
 *
 * @author aamerm
 */
public class HibernateMetadataVersionStore
    extends HibernateGenericStore<MetadataVersion>
    implements MetadataVersionStore
{
    @Override
    public MetadataVersion getVersionByKey( int key )
    {
        return (MetadataVersion) getCriteria( Restrictions.eq( "id", key ) ).uniqueResult();
    }

    @Override
    public MetadataVersion getVersionByName( String versionName )
    {
        return (MetadataVersion) getCriteria( Restrictions.eq( "name", versionName ) ).uniqueResult();
    }

    @Override
    public MetadataVersion getCurrentVersion()
    {
        Timestamp lastUpdatedDate = (Timestamp) getCriteria().setCacheable( false ).setProjection( Projections.max( "created" ) ).uniqueResult();
        return (MetadataVersion) getCriteria().setCacheable( false ).add( Property.forName( "created" ).eq( lastUpdatedDate ) ).uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MetadataVersion> getAllVersionsInBetween( Date startDate, Date endDate )
    {
        return getCriteria().add( Restrictions.between( "created", startDate, endDate ) ).list();
    }

    @Override
    public MetadataVersion getInitialVersion()
    {
        Timestamp initialCreateDate = (Timestamp) getCriteria().setCacheable( false ).setProjection( Projections.min( "created" ) ).uniqueResult();
        return (MetadataVersion) getCriteria().setCacheable( false ).add( Property.forName( "created" ).eq( initialCreateDate ) ).uniqueResult();
    }
}
