package org.hisp.dhis.reservedvalue.hibernate;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.reservedvalue.SequentialNumberCounter;
import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;
import org.springframework.beans.factory.annotation.Required;

import javax.transaction.Transactional;
import java.util.stream.IntStream;

@Transactional
public class HibernateSequentialNumberCounterStore
    implements SequentialNumberCounterStore
{

    protected SessionFactory sessionFactory;

    @Required
    public void setSessionFactory( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public int getNextValue( String uid, String key )
    {
        return getNextValues( uid, key, 1 )[0];
    }

    @Override
    public int[] getNextValues( String uid, String key, int length )
    {
        Session session = sessionFactory.getCurrentSession();

        try
        {
            int count;

            SequentialNumberCounter counter = (SequentialNumberCounter) session
                .createQuery( "FROM SequentialNumberCounter WHERE  ownerUID = ? AND key = ?" )
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

            return IntStream.range( count, count + length ).toArray();
        }
        catch ( RuntimeException e )
        {
            throw e;
        }

    }

    @Override
    public void deleteCounter( String uid )
    {
        Session session = sessionFactory.getCurrentSession();

        sessionFactory.getCurrentSession()
            .createQuery( "FROM SequentialNumberCounter WHERE ownerUID = ?" )
            .setParameter( 0, uid )
            .list()
            .stream()
            .forEach( session::delete );

    }
}
