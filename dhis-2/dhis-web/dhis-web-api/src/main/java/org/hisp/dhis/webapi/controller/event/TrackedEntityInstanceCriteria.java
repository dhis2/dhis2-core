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

    /**
     * Semicolon-delimited list of Organizational Unit UIDs
     */
    private String ou;

    /**
     * Selection mode for the specified organisation units, default is ACCESSIBLE.
     */
    private OrganisationUnitSelectionMode ouMode;

    /**
     * a Program UID for which instances in the response must be enrolled in.
     */
    private String program;

    /**
     * The {@see ProgramStatus} of the Tracked Entity Instance in the given program.
     */
    private ProgramStatus programStatus;

    /**
     * Indicates whether the Tracked Entity Instance is marked for follow up for the
     * specified Program.
     */
    private Boolean followUp;

    /**
     * Start date for last updated.
     */
    private Date lastUpdatedStartDate;

    /**
     * End date for last updated.
     */
    private Date lastUpdatedEndDate;

    /**
     * The last updated duration filter.
     */
    private String lastUpdatedDuration;

    /**
     * The given Program start date.
     */
    private Date programStartDate;

    /**
     * The given Program end date.
     */
    private Date programEndDate;

    /**
     * Start date for enrollment in the given program.
     */
    private Date programEnrollmentStartDate;

    /**
     * End date for enrollment in the given program.
     */
    private Date programEnrollmentEndDate;

    /**
     * Start date for incident in the given program.
     */
    private Date programIncidentStartDate;

    /**
     * End date for incident in the given program.
     */
    private Date programIncidentEndDate;

    /**
     * Only returns Tracked Entity Instances of this type.
     */
    private String trackedEntityType;

    /**
     * Semicolon-delimited list of Tracked Entity Instance UIDs
     */
    private String trackedEntityInstance;

    /**
     * Selection mode for user assignment of events.
     */
    private AssignedUserSelectionMode assignedUserMode;

    /**
     * Semicolon-delimited list of user UIDs to filter based on events assigned to
     * the users.
     */
    private String assignedUser;

    /**
     * Program Stage UID, used for filtering TEIs based on the selected Program Stage
     */
    private String programStage;

    /**
     * Status of any events in the specified program.
     */
    private EventStatus eventStatus;

    /**
     * Start date for Event for the given Program.
     */
    private Date eventStartDate;

    /**
     * End date for Event for the given Program.
     */
    private Date eventEndDate;

    /**
     * Indicates whether not to include meta data in the response.
     */
    private boolean skipMeta;

    /**
     * Page number to return.
     */
    private Integer page;

    /**
     * Page size.
     */
    private Integer pageSize;

    /**
     * Indicates whether to include the total number of pages in the paging
     * response.
     */
    private boolean totalPages;

    /**
     * Indicates whether paging should be skipped.
     */
    private Boolean skipPaging;

    /**
     * Indicated whether paging is enabled
     */
    private Boolean paging;

    /**
     * Indicates whether to include soft-deleted elements
     */
    private boolean includeDeleted;

    /**
     * Indicates whether to include all TEI attributes
     */
    private boolean includeAllAttributes;

    /**
     * TEI order params
     */
    private String order;
    
    /**
     * The file name in case of exporting as file
     */
    private String attachment;

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
