package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hibernate.Session;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Transactional
public class ProgramStageObjectBundleHook
    extends AbstractObjectBundleHook
{

    @Override
    public <T extends IdentifiableObject> void postCreate( T object, ObjectBundle bundle )
    {
        if ( !ProgramStage.class.isInstance( object ) )
        {
            return;
        }

        ProgramStage programStage = (ProgramStage) object;

        if ( programStage.getProgramStageSections().isEmpty() )
        {
            return;
        }

        Session session = sessionFactory.getCurrentSession();
        programStage.getProgramStageSections().stream()
            .filter( pss -> pss.getProgramStage() == null )
            .forEach( pss -> {
                pss.setProgramStage( programStage );
                session.update( pss );
            });
    }
}
