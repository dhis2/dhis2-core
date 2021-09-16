package org.hisp.dhis.metadata;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;

/**
 * @author Jan Bernitt
 */
@Repository
@AllArgsConstructor
public class HibernateMetadataProposalStore implements MetadataProposalStore
{

    private final SessionFactory sessionFactory;

    private Session getSession()
    {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public MetadataProposal getByUid( String uid )
    {
        return getSession().createQuery( "from MetadataProposal p where p.uid = :uid", MetadataProposal.class )
            .setParameter( "uid", uid )
            .getSingleResult();
    }

    @Override
    public void save( MetadataProposal proposal )
    {
        proposal.setAutoFields();
        getSession().save( proposal );
    }

    @Override
    public void update( MetadataProposal proposal )
    {
        getSession().saveOrUpdate( proposal );
    }

    @Override
    public void delete( MetadataProposal proposal )
    {
        getSession().delete( proposal );
    }
}
