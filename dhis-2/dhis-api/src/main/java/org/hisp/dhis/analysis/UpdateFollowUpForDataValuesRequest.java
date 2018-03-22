package org.hisp.dhis.analysis;

import java.util.List;

public class UpdateFollowUpForDataValuesRequest
{
    private List<FollowupParams> followups;

    public UpdateFollowUpForDataValuesRequest()
    {
    }

    public UpdateFollowUpForDataValuesRequest( List<FollowupParams> followups )
    {
        this.followups = followups;
    }

    public List<FollowupParams> getFollowups()
    {
        return followups;
    }

    public void setFollowups( List<FollowupParams> followups )
    {
        this.followups = followups;
    }
}
