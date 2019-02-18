package org.hisp.dhis.dataapproval;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class DataApprovalWorkflowDeletionHandler
    extends DeletionHandler
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Override
    public String getClassName()
    {
        return DataApprovalWorkflow.class.getSimpleName();
    }

    @Override
    public void deleteDataApprovalLevel( DataApprovalLevel level )
    {
        for ( DataApprovalWorkflow workflow : idObjectManager.getAllNoAcl( DataApprovalWorkflow.class ) )
        {
            if ( workflow.getLevels().contains( level ) )
            {
                workflow.getLevels().remove( level );
                idObjectManager.updateNoAcl( workflow );
            }
        }
    }
}
