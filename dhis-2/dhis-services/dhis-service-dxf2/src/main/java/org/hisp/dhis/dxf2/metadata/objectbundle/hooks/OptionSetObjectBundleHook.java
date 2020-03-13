package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hibernate.SessionFactory;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.springframework.stereotype.Component;

@Component
public class OptionSetObjectBundleHook
    extends AbstractObjectBundleHook
{
    private SessionFactory sessionFactory;

    public OptionSetObjectBundleHook( SessionFactory sessionFactory )
    {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Need to clear cache for OptionSets and Options because
     * Option is saved before OptionSet,
     * therefore after transaction committed, the cached Option objects have OptionSet reference = null
     * although the database foreign keys are updated correctly.
     * @param klass
     */
    @Override
    public void clearCache( Class klass )
    {
        if ( !klass.isAssignableFrom( OptionSet.class ) )
        {
            return;
        }

        sessionFactory.getCache().evictEntityRegion( OptionSet.class );
        sessionFactory.getCache().evictEntityRegion( Option.class );
        sessionFactory.getCache().evictCollectionRegion( OptionSet.class.getName() + ".options" );
        sessionFactory.getCache().evictQueryRegion( Option.class.getName() );
        sessionFactory.getCache().evictQueryRegion( OptionSet.class.getName() );
    }
}
