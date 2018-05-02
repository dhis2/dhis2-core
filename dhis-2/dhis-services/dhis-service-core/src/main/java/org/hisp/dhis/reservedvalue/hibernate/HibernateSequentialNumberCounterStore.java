package org.hisp.dhis.reservedvalue.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.reservedvalue.SequentialNumberCounter;
import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Stian Sandvold
 */
@Transactional
public class HibernateSequentialNumberCounterStore
    implements SequentialNumberCounterStore
{
    protected SessionFactory sessionFactory;

    public void setSessionFactory( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Integer> getNextValues( String uid, String key, int length )
    {
        Session session = sessionFactory.getCurrentSession();

        int count;

        SequentialNumberCounter counter = (SequentialNumberCounter) session
            .createQuery( "FROM SequentialNumberCounter WHERE owneruid = ? AND key = ?" )
            .setParameter( 0, uid )
            .setParameter( 1, key )
            .uniqueResult();

        if ( counter == null )
        {
            counter = new SequentialNumberCounter( uid, key, 1 );
        }

        count = counter.getCounter();
        counter.setCounter( count + length );
        session.saveOrUpdate( counter );

        return IntStream.range( count, count + length ).boxed().collect( Collectors.toList() );

    }

    @Override
    public void deleteCounter( String uid )
    {
        Session session = sessionFactory.getCurrentSession();

        sessionFactory.getCurrentSession()
            .createQuery( "DELETE SequentialNumberCounter WHERE owneruid = :uid" )
            .setParameter( "uid", uid )
            .executeUpdate();

    }
}
