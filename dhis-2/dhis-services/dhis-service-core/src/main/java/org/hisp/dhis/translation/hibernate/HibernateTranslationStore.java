package org.hisp.dhis.translation.hibernate;

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

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.system.util.LocaleUtils;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.translation.TranslationStore;

import java.util.List;
import java.util.Locale;

/**
 * @author Oyvind Brucker
 */
public class HibernateTranslationStore
    extends HibernateIdentifiableObjectStore<Translation>
    implements TranslationStore
{
    @Override
    @SuppressWarnings( "unchecked" )
    public Translation getTranslation( String className, Locale locale, String property, String objectUid )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );

        criteria.add( Restrictions.eq( "className", className ) );
        criteria.add( Restrictions.eq( "objectUid", objectUid ) );
        criteria.add( Restrictions.in( "locale", LocaleUtils.getLocaleFallbacks( locale ) ) );
        criteria.add( Restrictions.eq( "property", property ) );

        criteria.setCacheable( true );

        List<Translation> list = criteria.list();

        List<Translation> translations = LocaleUtils.getTranslationsHighestSpecifity( list );

        return !translations.isEmpty() ? translations.get( 0 ) : null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Translation getTranslationNoFallback( String className, Locale locale, String property, String objectUid )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );

        criteria.add( Restrictions.eq( "className", className ) );
        criteria.add( Restrictions.eq( "objectUid", objectUid ) );
        criteria.add( Restrictions.eq( "locale", locale.toString() ) );
        criteria.add( Restrictions.eq( "property", property ) );

        criteria.setCacheable( true );

        List<Translation> translations = criteria.list();

        return !translations.isEmpty() ? translations.get( 0 ) : null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Translation> getTranslations( String className, Locale locale, String objectUid )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );

        criteria.add( Restrictions.eq( "className", className ) );
        criteria.add( Restrictions.eq( "objectUid", objectUid ) );
        criteria.add( Restrictions.in( "locale", LocaleUtils.getLocaleFallbacks( locale ) ) );

        criteria.setCacheable( true );

        List<Translation> translations = criteria.list();

        return LocaleUtils.getTranslationsHighestSpecifity( translations );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Translation> getTranslationsNoFallback( String className, String objectUid, Locale locale )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );

        criteria.add( Restrictions.eq( "className", className ) );
        criteria.add( Restrictions.eq( "objectUid", objectUid ) );
        criteria.add( Restrictions.eq( "locale", locale.toString() ) );

        criteria.setCacheable( true );

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Translation> getTranslations( String className, Locale locale )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );

        criteria.add( Restrictions.eq( "className", className ) );
        criteria.add( Restrictions.in( "locale", LocaleUtils.getLocaleFallbacks( locale ) ) );

        criteria.setCacheable( true );

        List<Translation> translations = criteria.list();

        return LocaleUtils.getTranslationsHighestSpecifity( translations );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<Translation> getTranslations( Locale locale )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );

        criteria.add( Restrictions.eq( "locale", locale.toString() ) );

        criteria.setCacheable( true );

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void deleteTranslations( String className, String objectUid )
    {
        Session session = sessionFactory.getCurrentSession();

        Query query = session.createQuery( "from Translation t where t.className = :className and t.objectUid = :objectUid" );

        query.setString( "className", className );
        query.setString( "objectUid", objectUid );

        List<Object> objlist = query.list();

        for ( Object object : objlist )
        {
            session.delete( object );
        }
    }

    @Override
    public boolean hasTranslations( String className )
    {
        Session session = sessionFactory.getCurrentSession();

        Criteria criteria = session.createCriteria( Translation.class );
        criteria.add( Restrictions.eq( "className", className ) );
        criteria.setMaxResults( 1 );

        criteria.setCacheable( true );

        return !criteria.list().isEmpty();
    }
}
