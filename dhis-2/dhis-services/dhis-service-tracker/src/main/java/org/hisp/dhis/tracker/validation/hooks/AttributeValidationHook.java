package org.hisp.dhis.tracker.validation.hooks;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.UniqueAttributeValue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;

/**
 * @author Luciano Fiandesio
 */
public abstract class AttributeValidationHook extends AbstractTrackerDtoValidationHook
{

    private final TrackedEntityAttributeService teAttrService;

    public AttributeValidationHook( TrackedEntityAttributeService teAttrService )
    {
        this.teAttrService = teAttrService;
    }

    public <T extends TrackerDto> AttributeValidationHook( TrackerImportStrategy strategy,
        TrackedEntityAttributeService teAttrService )
    {
        super( strategy );
        this.teAttrService = teAttrService;
    }

    protected void validateAttrValueType( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        ValueType valueType = teAttr.getValueType();

        TrackerImportValidationContext context = errorReporter.getValidationContext();

        String error;

        if ( valueType.equals( ValueType.ORGANISATION_UNIT ) )
        {
            error = context.getOrganisationUnit( attr.getValue() ) == null
                ? " Value " + attr.getValue() + " is not a valid org unit value"
                : null;
        }
        else if ( valueType.equals( ValueType.USERNAME ) )
        {
            error = context.usernameExists( attr.getValue() ) ? null
                : " Value " + attr.getValue() + " is not a valid username value";
        }
        else
        {
            // We need to do try/catch here since validateValueType() since
            // validateValueType can cast IllegalArgumentException e.g.
            // on at
            // org.joda.time.format.DateTimeFormatter.parseDateTime(DateTimeFormatter.java:945)
            try
            {
                error = teAttrService.validateValueType( teAttr, attr.getValue() );
            }
            catch ( Exception e )
            {
                error = e.getMessage();
            }
        }

        if ( error != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1007 )
                .addArg( valueType.toString() )
                .addArg( error ) );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter errorReporter,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstance,
        OrganisationUnit organisationUnit )
    {
        checkNotNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.FALSE.equals( trackedEntityAttribute.isUnique() ) )
            return;

        List<UniqueAttributeValue> uniqueAttributeValues = errorReporter
            .getValidationContext().getBundle().getPreheat().getUniqueAttributeValues();

        for ( UniqueAttributeValue uniqueAttributeValue : uniqueAttributeValues )
        {
            boolean isTeaUniqueInOrgUnitScope = !trackedEntityAttribute.getOrgunitScope()
                || Objects.equals( organisationUnit.getUid(), uniqueAttributeValue.getOrgUnitId() );

            boolean isTheSameTea = Objects.equals( uniqueAttributeValue.getAttributeUid(),
                trackedEntityAttribute.getUid() );
            boolean hasTheSameValue = Objects.equals( uniqueAttributeValue.getValue(), value );
            boolean isNotSameTei = trackedEntityInstance == null
                || !Objects.equals( trackedEntityInstance.getUid(),
                    uniqueAttributeValue.getTeiUid() );

            if ( isTeaUniqueInOrgUnitScope
                && isTheSameTea
                && hasTheSameValue
                && isNotSameTei )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1064 )
                    .addArg( value )
                    .addArg( trackedEntityAttribute.getUid() ) );
                return;
            }
        }
    }
}
