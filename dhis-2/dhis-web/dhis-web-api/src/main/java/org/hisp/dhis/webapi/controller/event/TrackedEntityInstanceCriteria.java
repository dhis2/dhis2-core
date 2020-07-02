package org.hisp.dhis.webapi.controller.event;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;

/**
 * @author Luciano Fiandesio
 */
@Data
@NoArgsConstructor
public class TrackedEntityInstanceCriteria
{
    private String query;

    private Set<String> attribute;

    private Set<String> filter;

    private String ou;

    private OrganisationUnitSelectionMode ouMode;

    private String program;

    private ProgramStatus programStatus;

    private Boolean followUp;

    private Date lastUpdatedStartDate;

    private Date lastUpdatedEndDate;

    private String lastUpdatedDuration;

    private Date programStartDate;

    private Date programEnrollmentStartDate;

    private Date programEndDate;

    private Date programEnrollmentEndDate;

    private Date programIncidentStartDate;

    private Date programIncidentEndDate;

    private String trackedEntityType;

    private String trackedEntityInstance;

    private AssignedUserSelectionMode assignedUserMode;

    private String assignedUser;

    private EventStatus eventStatus;

    private Date eventStartDate;

    private Date eventEndDate;

    private boolean skipMeta;

    private Integer page;

    private Integer pageSize;

    private boolean totalPages;

    private Boolean skipPaging;

    private Boolean paging;

    private boolean includeDeleted;

    private boolean includeAllAttributes;

    private String order;

    private boolean useFast;

    public Set<String> getOrgUnits()
    {
        return ou != null ? TextUtils.splitToArray( ou, TextUtils.SEMICOLON ) : new HashSet<>();
    }

    public Set<String> getAssignedUsers()
    {
        return assignedUser != null ? TextUtils.splitToArray( assignedUser, TextUtils.SEMICOLON ) : new HashSet<>();
    }

    public boolean hasTrackedEntityInstance()
    {
        return StringUtils.isNotEmpty( this.trackedEntityInstance );
    }
    
    public Set<String> getTrackedEntityInstances()
    {
        if ( hasTrackedEntityInstance() )
        {
            return TextUtils.splitToArray( trackedEntityInstance, TextUtils.SEMICOLON );
        }
        return new HashSet<>();
    }
}
