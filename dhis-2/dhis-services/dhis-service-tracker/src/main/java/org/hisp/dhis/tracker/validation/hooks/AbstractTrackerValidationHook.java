package org.hisp.dhis.tracker.validation.hooks;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.tracker.validation.ValidationHookErrorReporter;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractTrackerValidationHook
    implements TrackerValidationHook

{
    @Autowired
    protected ProgramStageInstanceService programStageInstanceService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    protected EventService eventService;

    @Autowired
    protected FileResourceService fileResourceService;

    @Autowired
    protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    protected TrackedEntityAttributeService teAttrService;

    @Autowired
    protected ReservedValueService reservedValueService;

    @Autowired
    protected TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    @Autowired
    protected TrackerOwnershipManager trackerOwnershipManager;

    @Autowired
    protected TrackedEntityCommentService commentService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected I18nManager i18nManager;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Autowired
    protected TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected TrackedEntityTypeService defaultTrackedEntityTypeService;

    @Autowired
    public AclService aclService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    protected RelationshipService relationshipService;

    protected boolean validateAttributes( ValidationHookErrorReporter errorReporter, TrackerPreheat preheat,
        TrackedEntity te, TrackedEntityInstance tei,
        OrganisationUnit trackedEntityOu )
    {
        ValidationHookErrorReporter attrErrorReporter = new ValidationHookErrorReporter( errorReporter.isFailFast(),
            errorReporter.getMainKlass() );

        // For looking up existing tei attr. ie. if it is an update. Could/should this be done in the preheater instead?
        Map<String, TrackedEntityAttributeValue> teiAttributeValueMap = getTeiAttributeValueMap(
            trackedEntityAttributeValueService.getTrackedEntityAttributeValues( tei ) );

        List<Attribute> attributes = te.getAttributes();
        if ( !attributes.isEmpty() )
        {
            for ( Attribute attr : attributes )
            {
                if ( StringUtils.isNotEmpty( attr.getValue() ) )
                {
                    TrackedEntityAttribute tea = preheat
                        .get( TrackerIdentifier.UID, TrackedEntityAttribute.class, attr.getAttribute() );
                    if ( tea == null )
                    {
                        attrErrorReporter.raiseError( TrackerErrorCode.E1006, attr.getAttribute() );
                        continue;
                    }

                    // Existing attr value IF we do a update or else this will be NULL
                    TrackedEntityAttributeValue teiAttributeValue = teiAttributeValueMap.get( tea.getUid() );

                    validateTextPattern( attrErrorReporter, attr, tea, teiAttributeValue );

                    validateAttrValueType( attrErrorReporter, attr, tea );

                    // This is a super expensive check, needs better optimization
                    validateAttrUnique( attrErrorReporter, attr, tea, te, trackedEntityOu );

                    validateFileNotAlreadyAssigned( attrErrorReporter, attr, tei );
                }
                else
                {
                    // no/empty value? should this qualify as an error?
                }
            }
        }

        errorReporter.getReportList().addAll( attrErrorReporter.getReportList() );
        return attrErrorReporter.getReportList().isEmpty();
    }

    protected boolean checkCanCreateGeo( ValidationHookErrorReporter errorReporter, TrackedEntity te )
    {
        if ( !FeatureType.NONE.equals( te.getFeatureType() ) && te.getCoordinates() != null )
        {
            try
            {
                GeoUtils.getGeometryFromCoordinatesAndType( te.getFeatureType(), te.getCoordinates() );
            }
            catch ( IOException e )
            {
                errorReporter.raiseError( TrackerErrorCode.E1013 );// Add exception message?
                return false;
            }

        }

        return true;
    }

    protected OrganisationUnit getOrganisationUnit( ValidationHookErrorReporter errorReporter, TrackerPreheat preheat,
        TrackedEntity te )
    {
        if ( StringUtils.isEmpty( te.getOrgUnit() ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1010 );
            return null;
        }

        OrganisationUnit ou = preheat.get( TrackerIdentifier.UID, OrganisationUnit.class, te.getOrgUnit() );
        if ( ou == null )
        {
            errorReporter.raiseError( TrackerErrorCode.E1011, te.getOrgUnit() ).setErrorProperties( "orgUnit" );
            return null;
        }

        return ou;
    }

    protected boolean textPatternValueIsValid( TrackedEntityAttribute attribute, String value, String oldValue )
    {
        return Objects.equals( value, oldValue ) ||
            TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value ) ||
            reservedValueService.isReserved( attribute.getTextPattern(), value );
    }

    private boolean validateTextPattern( ValidationHookErrorReporter errorReporter,
        Attribute attr, TrackedEntityAttribute teAttr,
        TrackedEntityAttributeValue teiAttributeValue )
    {
        if ( teAttr.isGenerated() && teAttr.getTextPattern() != null )
        //&& ??? !importOptions.isSkipPatternValidation()
        // MortenO: How should we deal with this in the new importer?
        {
            String oldValue = teiAttributeValue != null ? teiAttributeValue.getValue() : null;
            if ( !textPatternValueIsValid( teAttr, attr.getValue(), oldValue ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1008, attr.getAttribute() );
                return false;
            }
        }

        return true;
    }

    private boolean validateFileNotAlreadyAssigned( ValidationHookErrorReporter errorReporter, Attribute attr,
        TrackedEntityInstance tei )
    {
        boolean attrIsFile = attr.getValueType() != null && attr.getValueType().isFile();

        if ( attrIsFile )
        {
            List<String> existingValues = new ArrayList<>();

            tei.getTrackedEntityAttributeValues().stream()
                .filter( attrVal -> attrVal.getAttribute().getValueType().isFile() )
                .forEach( attrVal -> existingValues.add( attrVal.getValue() ) );

            FileResource fileResource = fileResourceService.getFileResource( attr.getValue() );
            if ( fileResource != null && fileResource.isAssigned() && !existingValues.contains( attr.getValue() ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1009, attr.getValue() );
                return false;
            }
        }

        return true;
    }

    private boolean validateAttrValueType( ValidationHookErrorReporter errorReporter,
        Attribute attr, TrackedEntityAttribute teAttr )
    {
        String error = teAttrService.validateValueType( teAttr, attr.getValue() );
        if ( error != null )
        {
            errorReporter.raiseError( TrackerErrorCode.E1007, error );
            return false;
        }

        return true;
    }

    private boolean validateAttrUnique( ValidationHookErrorReporter errorReporter,
        Attribute attr, TrackedEntityAttribute teAttr,
        TrackedEntity te, OrganisationUnit trackedEntityOu )
    {
        if ( teAttr.isUnique() )
        {
            String error = teAttrService
                .validateAttributeUniquenessWithinScope(
                    teAttr,
                    attr.getValue(),
                    te.getTrackedEntity(),
                    trackedEntityOu );

            if ( error != null )
            {
                errorReporter.raiseError( TrackerErrorCode.E1007, error );
                return false;
            }
        }

        return true;
    }

    protected Map<String, TrackedEntityAttributeValue> getTeiAttributeValueMap(
        List<TrackedEntityAttributeValue> values )
    {
        return values.stream().collect( Collectors.toMap( v -> v.getAttribute().getUid(), v -> v ) );
    }

    protected boolean isInUserSearchHierarchy( ValidationHookErrorReporter errorReporter, User user,
        OrganisationUnit ou )
    {
        if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1000, ou.getUid() );
            return false;
        }

        return true;
    }

    protected boolean userHasWriteAccess( ValidationHookErrorReporter errorReporter, User user,
        TrackedEntityType teType )
    {
        if ( !aclService.canDataWrite( user, teType ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1000, teType.getUid() );
            return false;
        }

        return true;
    }

    protected boolean validateGeo( ValidationHookErrorReporter errorReporter, TrackedEntity te,
        FeatureType featureType )
    {
        if ( te.getGeometry() != null )
        {
            FeatureType typeFromName = FeatureType.getTypeFromName( te.getGeometry().getGeometryType() );

            if ( featureType.equals( FeatureType.NONE ) || !featureType.equals( typeFromName ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1012, featureType );
                return false;
            }
        }
        else
        {
            return checkCanCreateGeo( errorReporter, te );
        }

        return true;
    }
}
