package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class DataValidationJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 4611088348113126038L;

    private String organisationUnitUid;

    DataValidationJobParameters()
    {
    }

    DataValidationJobParameters( String organisationUnitUid )
    {
        this.organisationUnitUid = organisationUnitUid;
    }

    public String getOrganisationUnitUid()
    {
        return organisationUnitUid;
    }

    public void setOrganisationUnitUid( String organisationUnitUid )
    {
        this.organisationUnitUid = organisationUnitUid;
    }
}
