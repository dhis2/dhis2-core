package org.hisp.dhis.trackedentitycomment;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class TrackedEntityCommentDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------    
 
    @Autowired
    TrackedEntityCommentService commentService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return ProgramInstance.class.getSimpleName();
    }

    @Override
    public void deleteProgramInstance( ProgramInstance programInstance )
    {
        for( TrackedEntityComment comment : programInstance.getComments())
        {
            commentService.deleteTrackedEntityComment( comment );
        }
    }
    
    @Override
    public void deleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        for( TrackedEntityComment comment : programStageInstance.getComments())
        {
            commentService.deleteTrackedEntityComment( comment );
        }
    }
}
