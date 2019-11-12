package org.hisp.dhis.dxf2.events.trackedentity;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class AbstractTrackedEntityInstanceService
    implements TrackedEntityInstanceService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    @Autowired
    protected org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    protected TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    protected EnrollmentService enrollmentService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected QueryService queryService;

    @Autowired
    protected ReservedValueService reservedValueService;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Autowired
    protected FileResourceService fileResourceService;

    @Autowired
    private I18nManager i18nManager;

    private final CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private final CachingMap<String, TrackedEntityType> trackedEntityCache = new CachingMap<>();

    private final CachingMap<String, TrackedEntityAttribute> trackedEntityAttributeCache = new CachingMap<>();

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams queryParams, TrackedEntityInstanceParams params )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> daoTEIs = teiService.getTrackedEntityInstances( queryParams );

        List<TrackedEntityInstance> dtoTEIItems = new ArrayList<>();
        User user = currentUserService.getCurrentUser();

        for ( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance : daoTEIs )
        {
            if ( trackerAccessManager.canRead( user, daoTrackedEntityInstance ).isEmpty() )
            {
                dtoTEIItems.add( getTrackedEntityInstance( daoTrackedEntityInstance, params, user ) );
            }
        }

        return dtoTEIItems;
    }

    @Override
    @Transactional(readOnly = true)
    public int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params, boolean sync )
    {
        return teiService.getTrackedEntityInstanceCount( params, sync );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( String uid )
    {
        return getTrackedEntityInstance( teiService.getTrackedEntityInstance( uid ) );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( String uid, TrackedEntityInstanceParams params )
    {
        return getTrackedEntityInstance( teiService.getTrackedEntityInstance( uid ), params );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance )
    {
        return getTrackedEntityInstance( daoTrackedEntityInstance, TrackedEntityInstanceParams.TRUE );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance,
        TrackedEntityInstanceParams params )
    {
        return getTrackedEntityInstance( daoTrackedEntityInstance, params, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance,
        TrackedEntityInstanceParams params, User user )
    {
        if ( daoTrackedEntityInstance == null )
        {
            return null;
        }

        List<String> errors = trackerAccessManager.canRead( user, daoTrackedEntityInstance );

        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }

        return getTei( daoTrackedEntityInstance, params, user );
    }

    private org.hisp.dhis.trackedentity.TrackedEntityInstance createDAOTrackedEntityInstance(TrackedEntityInstance dtoEntityInstance, ImportOptions importOptions, ImportSummary importSummary)
    {
        if ( StringUtils.isEmpty( dtoEntityInstance.getOrgUnit() ) )
        {
            importSummary.getConflicts().add( new ImportConflict( dtoEntityInstance.getTrackedEntityInstance(), "No org unit ID in tracked entity instance object." ) );
            return null;
        }

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = new org.hisp.dhis.trackedentity.TrackedEntityInstance();

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), dtoEntityInstance.getOrgUnit() );

        if ( organisationUnit == null )
        {
            importSummary.getConflicts().add( new ImportConflict( dtoEntityInstance.getTrackedEntityInstance(), "Invalid org unit ID: " + dtoEntityInstance.getOrgUnit() ) );
            return null;
        }

        daoEntityInstance.setOrganisationUnit( organisationUnit );

        TrackedEntityType trackedEntityType = getTrackedEntityType( importOptions.getIdSchemes(), dtoEntityInstance.getTrackedEntityType() );

        if ( trackedEntityType == null )
        {
            importSummary.getConflicts().add( new ImportConflict( dtoEntityInstance.getTrackedEntityInstance(), "Invalid tracked entity ID: " + dtoEntityInstance.getTrackedEntityType() ) );
            return null;
        }

        daoEntityInstance.setTrackedEntityType( trackedEntityType );
        daoEntityInstance.setUid( CodeGenerator.isValidUid( dtoEntityInstance.getTrackedEntityInstance() ) ?
            dtoEntityInstance.getTrackedEntityInstance() : CodeGenerator.generateUid() );

        return daoEntityInstance;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummaries addTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions )
    {
        User user = currentUserService.getCurrentUser();
        List<List<TrackedEntityInstance>> partitions = Lists.partition( trackedEntityInstances, FLUSH_FREQUENCY );

        ImportSummaries importSummaries = new ImportSummaries();

        for ( List<TrackedEntityInstance> _trackedEntityInstances : partitions )
        {
            prepareCaches( _trackedEntityInstances, user );

            for ( TrackedEntityInstance trackedEntityInstance : _trackedEntityInstances )
            {
                importSummaries.addImportSummary( addTrackedEntityInstance( trackedEntityInstance, user, importOptions ) );
            }

            clearSession();
        }

        return importSummaries;
    }

    @Override
    @Transactional
    public ImportSummary addTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions )
    {
        return addTrackedEntityInstance( trackedEntityInstance, currentUserService.getCurrentUser(), importOptions );
    }

    private ImportSummary addTrackedEntityInstance( TrackedEntityInstance dtoEntityInstance, User user, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummary importSummary = new ImportSummary( dtoEntityInstance.getTrackedEntityInstance() );

        dtoEntityInstance.trimValuesToNull();

        Set<ImportConflict> importConflicts = new HashSet<>();
        importConflicts.addAll( checkTrackedEntityType( dtoEntityInstance, importOptions ) );
        importConflicts.addAll( checkAttributes( dtoEntityInstance, importOptions ) );

        if ( !importConflicts.isEmpty() )
        {
            importSummary.setConflicts( importConflicts );
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();
            return importSummary;
        }

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = createDAOTrackedEntityInstance( dtoEntityInstance, importOptions, importSummary );

        if ( daoEntityInstance == null )
        {
            return importSummary;
        }

        List<String> errors = trackerAccessManager.canWrite( user, daoEntityInstance );

        if ( !errors.isEmpty() )
        {
            return new ImportSummary( ImportStatus.ERROR, errors.toString() );
        }

        teiService.addTrackedEntityInstance( daoEntityInstance );

        updateRelationships( dtoEntityInstance );
        updateAttributeValues( dtoEntityInstance, daoEntityInstance, user );
        updateDateFields( dtoEntityInstance, daoEntityInstance );

        daoEntityInstance.setFeatureType( dtoEntityInstance.getFeatureType() );
        daoEntityInstance.setCoordinates( dtoEntityInstance.getCoordinates() );

        teiService.updateTrackedEntityInstance( daoEntityInstance );

        importSummary.setReference( daoEntityInstance.getUid() );
        importSummary.getImportCount().incrementImported();

        importOptions.setStrategy( ImportStrategy.CREATE_AND_UPDATE );
        importSummary.setEnrollments( handleEnrollments( dtoEntityInstance, daoEntityInstance, importOptions ) );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummaries updateTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions )
    {
        User user = currentUserService.getCurrentUser();
        List<List<TrackedEntityInstance>> partitions = Lists.partition( trackedEntityInstances, FLUSH_FREQUENCY );

        ImportSummaries importSummaries = new ImportSummaries();

        for ( List<TrackedEntityInstance> _trackedEntityInstances : partitions )
        {
            prepareCaches( _trackedEntityInstances, user );

            for ( TrackedEntityInstance trackedEntityInstance : _trackedEntityInstances )
            {
                importSummaries.addImportSummary( updateTrackedEntityInstance( trackedEntityInstance, user, importOptions ) );
            }

            clearSession();
        }

        return importSummaries;
    }

    @Override
    @Transactional
    public ImportSummary updateTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions )
    {
        return updateTrackedEntityInstance( trackedEntityInstance, currentUserService.getCurrentUser(), importOptions );
    }

    private ImportSummary updateTrackedEntityInstance( TrackedEntityInstance dtoEntityInstance, User user, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummary importSummary = new ImportSummary( dtoEntityInstance.getTrackedEntityInstance() );

        dtoEntityInstance.trimValuesToNull();

        Set<ImportConflict> importConflicts = new HashSet<>();
        importConflicts.addAll( checkRelationships( dtoEntityInstance ) );
        importConflicts.addAll( checkAttributes( dtoEntityInstance, importOptions ) );

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = teiService.getTrackedEntityInstance( dtoEntityInstance.getTrackedEntityInstance() );

        if ( daoEntityInstance == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance", "trackedEntityInstance " + dtoEntityInstance.getTrackedEntityInstance()
                + " does not point to valid trackedEntityInstance" ) );
        }

        List<String> errors = trackerAccessManager.canWrite( user, daoEntityInstance );

        if ( !errors.isEmpty() )
        {
            return new ImportSummary( ImportStatus.ERROR, errors.toString() );
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( new IdSchemes(), dtoEntityInstance.getOrgUnit() );

        if ( organisationUnit == null )
        {
            importConflicts.add( new ImportConflict( "OrganisationUnit", "orgUnit " + dtoEntityInstance.getOrgUnit()
                + " does not point to valid organisation unit" ) );
        }
        else
        {
            daoEntityInstance.setOrganisationUnit( organisationUnit );
        }

        if ( !importConflicts.isEmpty() )
        {
            importSummary.setConflicts( importConflicts );
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();

            return importSummary;
        }

        daoEntityInstance.setInactive( dtoEntityInstance.isInactive() );
        daoEntityInstance.setFeatureType( dtoEntityInstance.getFeatureType() );
        daoEntityInstance.setCoordinates( dtoEntityInstance.getCoordinates() );

        removeRelationships( daoEntityInstance );
        removeAttributeValues( daoEntityInstance );
        teiService.updateTrackedEntityInstance( daoEntityInstance );

        updateRelationships( dtoEntityInstance );
        updateAttributeValues( dtoEntityInstance, daoEntityInstance, user );
        updateDateFields( dtoEntityInstance, daoEntityInstance );

        teiService.updateTrackedEntityInstance( daoEntityInstance );

        importSummary.setReference( daoEntityInstance.getUid() );
        importSummary.getImportCount().incrementUpdated();

        importOptions.setStrategy( ImportStrategy.CREATE_AND_UPDATE );
        importSummary.setEnrollments( handleEnrollments( dtoEntityInstance, daoEntityInstance, importOptions ) );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummary deleteTrackedEntityInstance( String uid )
    {
        org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = teiService.getTrackedEntityInstance( uid );

        User user = currentUserService.getCurrentUser();

        if ( entityInstance != null )
        {
            if( !entityInstance.getProgramInstances().isEmpty() && user != null && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                return new ImportSummary( ImportStatus.ERROR, "The " + entityInstance.getTrackedEntityType().getName() + " to be deleted has associated enrollments. Deletion requires special authority: " + i18nManager.getI18n().getString( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) ).incrementIgnored();
            }

            teiService.deleteTrackedEntityInstance( entityInstance );
            return new ImportSummary( ImportStatus.SUCCESS, "Deletion of tracked entity instance " + uid + " was successful" ).incrementDeleted();
        }

        return new ImportSummary( ImportStatus.ERROR, "ID " + uid + " does not point to a valid tracked entity instance" ).incrementIgnored();
    }

    @Override
    @Transactional
    public ImportSummaries deleteTrackedEntityInstances( List<String> uids )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        for ( String uid : uids )
        {
            importSummaries.addImportSummary( deleteTrackedEntityInstance( uid ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private ImportSummaries handleEnrollments( TrackedEntityInstance dtoEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance, ImportOptions importOptions )
    {
        List<Enrollment> create = new ArrayList<>();
        List<Enrollment> update = new ArrayList<>();

        for ( Enrollment enrollment : dtoEntityInstance.getEnrollments() )
        {
            enrollment.setTrackedEntityType( dtoEntityInstance.getTrackedEntityType() );
            enrollment.setTrackedEntityInstance( daoEntityInstance.getUid() );

            if ( !programInstanceService.programInstanceExists( enrollment.getEnrollment() ) )
            {
                create.add( enrollment );
            }
            else
            {
                update.add( enrollment );
            }
        }

        ImportSummaries importSummaries = new ImportSummaries();

        importSummaries.addImportSummaries( enrollmentService.addEnrollments( create, importOptions, daoEntityInstance, false ) );
        importSummaries.addImportSummaries( enrollmentService.updateEnrollments( update, importOptions, daoEntityInstance, false ) );

        return importSummaries;
    }

    private void prepareCaches( List<TrackedEntityInstance> trackedEntityInstances, User user )
    {
        Collection<String> orgUnits = trackedEntityInstances.stream().map( TrackedEntityInstance::getOrgUnit ).collect( Collectors.toSet() );

        if ( !orgUnits.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( OrganisationUnit.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", orgUnits ) );
            queryService.query( query ).forEach( ou -> organisationUnitCache.put( ou.getUid(), (OrganisationUnit) ou ) );
        }

        Collection<String> trackedEntityAttributes = new HashSet<>();
        trackedEntityInstances.forEach( e -> e.getAttributes().forEach( at -> trackedEntityAttributes.add( at.getAttribute() ) ) );

        if ( !trackedEntityAttributes.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( TrackedEntityAttribute.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", trackedEntityAttributes ) );
            queryService.query( query ).forEach( tea -> trackedEntityAttributeCache.put( tea.getUid(), (TrackedEntityAttribute) tea ) );
        }
    }

    private void updateAttributeValues( TrackedEntityInstance dtoEntityInstance,
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance, User user )
    {
        for ( Attribute dtoAttribute : dtoEntityInstance.getAttributes() )
        {
            TrackedEntityAttribute daoEntityAttribute = trackedEntityAttributeService.getTrackedEntityAttribute( dtoAttribute.getAttribute() );

            if ( daoEntityAttribute != null )
            {
                TrackedEntityAttributeValue daoAttributeValue = new TrackedEntityAttributeValue();
                daoAttributeValue.setEntityInstance( daoEntityInstance );
                daoAttributeValue.setValue( dtoAttribute.getValue() );
                daoAttributeValue.setAttribute( daoEntityAttribute );

                daoEntityInstance.addAttributeValue( daoAttributeValue );

                String storedBy = getStoredBy( daoAttributeValue, new ImportSummary(), user );
                daoAttributeValue.setStoredBy( storedBy );

                trackedEntityAttributeValueService.addTrackedEntityAttributeValue( daoAttributeValue );
            }
        }
    }

    private void updateRelationships( TrackedEntityInstance dtoEntityInstance )
    {
        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship dtoRelationship : dtoEntityInstance.getRelationships() )
        {
            org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstanceA = teiService.getTrackedEntityInstance( dtoRelationship.getTrackedEntityInstanceA() );
            org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstanceB = teiService.getTrackedEntityInstance( dtoRelationship.getTrackedEntityInstanceB() );

            RelationshipType relationshipType = manager.get( RelationshipType.class, dtoRelationship.getRelationship() );

            Relationship daoRelationship = new Relationship();
            daoRelationship.setEntityInstanceA( daoEntityInstanceA );
            daoRelationship.setEntityInstanceB( daoEntityInstanceB );
            daoRelationship.setRelationshipType( relationshipType );

            relationshipService.addRelationship( daoRelationship );
        }
    }

    private void removeRelationships( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance )
    {
        Collection<Relationship> relationships = relationshipService.getRelationshipsForTrackedEntityInstance( entityInstance );
        relationships.forEach( relationshipService::deleteRelationship );
    }

    private void removeAttributeValues( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance )
    {
        entityInstance.getTrackedEntityAttributeValues().forEach( trackedEntityAttributeValueService::deleteTrackedEntityAttributeValue );
        teiService.updateTrackedEntityInstance( entityInstance );
    }

    private OrganisationUnit getOrganisationUnit( IdSchemes idSchemes, String id )
    {
        return organisationUnitCache.get( id, () -> manager.getObject( OrganisationUnit.class, idSchemes.getOrgUnitIdScheme(), id ) );
    }

    private TrackedEntityType getTrackedEntityType( IdSchemes idSchemes, String id )
    {
        return trackedEntityCache.get( id, () -> manager.getObject( TrackedEntityType.class, idSchemes.getTrackedEntityIdScheme(), id ) );
    }

    private TrackedEntityAttribute getTrackedEntityAttribute( IdSchemes idSchemes, String id )
    {
        return trackedEntityAttributeCache.get( id, () -> manager.getObject( TrackedEntityAttribute.class, idSchemes.getTrackedEntityAttributeIdScheme(), id ) );
    }

    private Map<String, TrackedEntityAttributeValue> getTeiAttributeValueMap(
        List<TrackedEntityAttributeValue> teiAttributeValues )
    {
        return teiAttributeValues.stream()
            .collect( Collectors.toMap( tav -> tav.getAttribute().getUid(), tav -> tav ) );
    }

    //--------------------------------------------------------------------------
    // VALIDATION
    //--------------------------------------------------------------------------

    private List<ImportConflict> validateAttributeType( Attribute attribute, ImportOptions importOptions )
    {
        List<ImportConflict> importConflicts = Lists.newArrayList();

        if ( attribute == null || attribute.getValue() == null )
        {
            return importConflicts;
        }

        TrackedEntityAttribute daoTrackedEntityAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(), attribute.getAttribute() );

        if ( daoTrackedEntityAttribute == null )
        {
            importConflicts.add( new ImportConflict( "Attribute.attribute", "Does not point to a valid attribute." ) );
            return importConflicts;
        }

        String errorMessage = trackedEntityAttributeService.validateValueType( daoTrackedEntityAttribute, attribute.getValue() );

        if ( errorMessage != null )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", errorMessage ) );
        }

        return importConflicts;
    }

    private List<ImportConflict> checkRelationships( TrackedEntityInstance dtoEntityInstance )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship dtoRelationship : dtoEntityInstance.getRelationships() )
        {
            RelationshipType daoRelationshipType = manager.get( RelationshipType.class, dtoRelationship.getRelationship() );

            if ( daoRelationshipType == null )
            {
                importConflicts.add( new ImportConflict( "Relationship.type", "Invalid type " + dtoRelationship.getRelationship() ) );
            }

            org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstanceA = teiService.getTrackedEntityInstance( dtoRelationship.getTrackedEntityInstanceA() );

            if ( daoEntityInstanceA == null )
            {
                importConflicts.add( new ImportConflict( "Relationship.trackedEntityInstance", "Invalid trackedEntityInstance "
                    + dtoRelationship.getTrackedEntityInstanceA() ) );
            }

            org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstanceB = teiService.getTrackedEntityInstance( dtoRelationship.getTrackedEntityInstanceB() );

            if ( daoEntityInstanceB == null )
            {
                importConflicts.add( new ImportConflict( "Relationship.trackedEntityInstance", "Invalid trackedEntityInstance "
                    + dtoRelationship.getTrackedEntityInstanceB() ) );
            }
        }
        return importConflicts;
    }

    private List<ImportConflict> validateTextPatternValue( TrackedEntityAttribute attribute, String value,
        String oldValue )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        if ( !TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value )
            && !reservedValueService.isReserved( attribute.getTextPattern(), value )
            && !Objects.equals( value, oldValue ) )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", "Value does not match the attribute pattern." ) );
        }

        return importConflicts;
    }

    private List<ImportConflict> checkScope( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance,
        TrackedEntityAttribute trackedEntityAttribute, String value, OrganisationUnit organisationUnit )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        if ( trackedEntityAttribute == null || value == null )
        {
            return importConflicts;
        }

        String errorMessage = trackedEntityAttributeService.validateScope( trackedEntityAttribute, value, entityInstance,
            organisationUnit, null );

        if ( errorMessage != null )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", errorMessage ) );
        }

        return importConflicts;
    }

    private List<ImportConflict> checkAttributes( TrackedEntityInstance dtoEntityInstance, ImportOptions importOptions )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();
        List<String> fileValues = new ArrayList<>();

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstanceTemp = teiService.getTrackedEntityInstance( dtoEntityInstance.getTrackedEntityInstance() );

        if ( daoEntityInstanceTemp != null )
        {
            daoEntityInstanceTemp.getTrackedEntityAttributeValues().stream()
                .filter( attrVal -> attrVal.getAttribute().getValueType().isFile() )
                .forEach( attrVal -> fileValues.add( attrVal.getValue() ) );
        }

        Map<String, TrackedEntityAttributeValue> teiAttributeValueMap = getTeiAttributeValueMap(
            trackedEntityAttributeValueService.getTrackedEntityAttributeValues( daoEntityInstanceTemp ) );

        for ( Attribute attribute : dtoEntityInstance.getAttributes() )
        {
            TrackedEntityAttribute daoEntityAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(), attribute.getAttribute() );
            TrackedEntityAttributeValue trackedEntityAttributeValue = teiAttributeValueMap
                .get( daoEntityAttribute.getUid() );

            if ( daoEntityAttribute == null )
            {
                importConflicts.add( new ImportConflict( "Attribute.attribute", "Invalid attribute " + attribute.getAttribute() ) );
                continue;
            }

            if ( daoEntityAttribute.isGenerated() && daoEntityAttribute.getTextPattern() != null && !importOptions.isSkipPatternValidation() )
            {
                importConflicts.addAll( validateTextPatternValue( daoEntityAttribute, attribute.getValue(),
                    trackedEntityAttributeValue != null ? trackedEntityAttributeValue.getValue() : null ) );
            }

            if ( daoEntityAttribute.isUnique() )
            {
                OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), dtoEntityInstance.getOrgUnit() );
                org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = teiService.getTrackedEntityInstance( dtoEntityInstance.getTrackedEntityInstance() );
                importConflicts.addAll( checkScope( daoEntityInstance, daoEntityAttribute, attribute.getValue(), organisationUnit ) );
            }

            importConflicts.addAll( validateAttributeType( attribute, importOptions ) );

            if ( daoEntityAttribute.getValueType().isFile() && checkAssigned( attribute, fileValues ) )
            {
                importConflicts.add( new ImportConflict( "Attribute.value",
                    String.format( " File Resource with uid '%s' has already been assigned to a different object", attribute.getValue() ) ) );
            }
        }

        return importConflicts;
    }

    private List<ImportConflict> checkTrackedEntityType( TrackedEntityInstance entityInstance, ImportOptions importOptions )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        if ( entityInstance.getTrackedEntityType() == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance.trackedEntityType", "Missing required property trackedEntityType" ) );
            return importConflicts;
        }

        TrackedEntityType daoTrackedEntityType = getTrackedEntityType( importOptions.getIdSchemes(), entityInstance.getTrackedEntityType() );

        if ( daoTrackedEntityType == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance.trackedEntityType", "Invalid trackedEntityType" +
                entityInstance.getTrackedEntityType() ) );
        }

        return importConflicts;
    }

    private void clearSession()
    {
        organisationUnitCache.clear();
        trackedEntityCache.clear();
        trackedEntityAttributeCache.clear();

        dbmsManager.clearSession();
    }

    private void updateDateFields( TrackedEntityInstance dtoEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance )
    {
        daoEntityInstance.setAutoFields();

        Date createdAtClient = DateUtils.parseDate( dtoEntityInstance.getCreatedAtClient() );

        if ( createdAtClient != null )
        {
            daoEntityInstance.setCreatedAtClient( createdAtClient );
        }

        String lastUpdatedAtClient = dtoEntityInstance.getLastUpdatedAtClient();

        if ( lastUpdatedAtClient != null )
        {
            daoEntityInstance.setLastUpdatedAtClient( DateUtils.parseDate( lastUpdatedAtClient ) );
        }
    }

    private String getStoredBy( TrackedEntityAttributeValue attributeValue, ImportSummary importSummary, User fallbackUser )
    {
        String storedBy = attributeValue.getStoredBy();

        if ( StringUtils.isEmpty( storedBy ) )
        {
            storedBy = User.getSafeUsername( fallbackUser );
        }
        else if ( storedBy.length() >= 31 )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add( new ImportConflict( "stored by",
                    storedBy + " is more than 31 characters, using current username instead" ) );
            }

            storedBy = User.getSafeUsername( fallbackUser );
        }

        return storedBy;
    }

    private boolean checkAssigned( Attribute attribute, List<String> oldFileValues )
    {
        FileResource fileResource = fileResourceService.getFileResource( attribute.getValue() );
        return fileResource != null && fileResource.isAssigned() && !oldFileValues.contains( attribute.getValue() );
    }

    private TrackedEntityInstance getTei( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance,
        TrackedEntityInstanceParams params, User user )
    {
        if ( daoTrackedEntityInstance == null )
        {
            return null;
        }

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setTrackedEntityInstance( daoTrackedEntityInstance.getUid() );
        trackedEntityInstance.setOrgUnit( daoTrackedEntityInstance.getOrganisationUnit().getUid() );
        trackedEntityInstance.setTrackedEntityType( daoTrackedEntityInstance.getTrackedEntityType().getUid() );
        trackedEntityInstance.setCreated( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getCreated() ) );
        trackedEntityInstance.setCreatedAtClient( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getLastUpdatedAtClient() ) );
        trackedEntityInstance.setLastUpdated( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getLastUpdated() ) );
        trackedEntityInstance.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getLastUpdatedAtClient() ) );
        trackedEntityInstance.setInactive( daoTrackedEntityInstance.isInactive() );
        trackedEntityInstance.setFeatureType( daoTrackedEntityInstance.getFeatureType() );
        trackedEntityInstance.setCoordinates( daoTrackedEntityInstance.getCoordinates() );

        if ( params.isIncludeRelationships() )
        {
            //TODO include relationships in data model and void transactional query in for-loop

            Collection<Relationship> daoRelationships = relationshipService.getRelationshipsForTrackedEntityInstance( daoTrackedEntityInstance );

            for ( Relationship daoEntityRelationship : daoRelationships )
            {
                org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship = new org.hisp.dhis.dxf2.events.trackedentity.Relationship();
                relationship.setDisplayName( daoEntityRelationship.getRelationshipType().getDisplayName() );
                relationship.setTrackedEntityInstanceA( daoEntityRelationship.getEntityInstanceA().getUid() );
                relationship.setTrackedEntityInstanceB( daoEntityRelationship.getEntityInstanceB().getUid() );

                relationship.setRelationship( daoEntityRelationship.getRelationshipType().getUid() );

                // we might have cases where A <=> A, so we only include the relative if the UIDs do not match
                if ( !daoEntityRelationship.getEntityInstanceA().getUid().equals( daoTrackedEntityInstance.getUid() ) )
                {
                    relationship.setRelative( getTei( daoEntityRelationship.getEntityInstanceA(), TrackedEntityInstanceParams.FALSE, user ) );
                }
                else if ( !daoEntityRelationship.getEntityInstanceB().getUid().equals( daoTrackedEntityInstance.getUid() ) )
                {
                    relationship.setRelative( getTei( daoEntityRelationship.getEntityInstanceB(), TrackedEntityInstanceParams.FALSE, user ) );
                }

                trackedEntityInstance.getRelationships().add( relationship );
            }
        }

        if ( params.isIncludeEnrollments() )
        {
            for ( ProgramInstance programInstance : daoTrackedEntityInstance.getProgramInstances() )
            {
                if ( trackerAccessManager.canRead( user, programInstance ).isEmpty() )
                {
                    trackedEntityInstance.getEnrollments().add( enrollmentService.getEnrollment( user, programInstance, params ) );
                }
            }
        }

        Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes();

        for ( TrackedEntityAttributeValue attributeValue : daoTrackedEntityInstance.getTrackedEntityAttributeValues() )
        {
            if ( readableAttributes.contains( attributeValue.getAttribute() ) )
            {
                Attribute attribute = new Attribute();

                attribute.setCreated( DateUtils.getIso8601NoTz( attributeValue.getCreated() ) );
                attribute.setLastUpdated( DateUtils.getIso8601NoTz( attributeValue.getLastUpdated() ) );
                attribute.setDisplayName( attributeValue.getAttribute().getDisplayName() );
                attribute.setAttribute( attributeValue.getAttribute().getUid() );
                attribute.setValueType( attributeValue.getAttribute().getValueType() );
                attribute.setCode( attributeValue.getAttribute().getCode() );
                attribute.setValue( attributeValue.getValue() );
                attribute.setStoredBy( attributeValue.getStoredBy() );

                trackedEntityInstance.getAttributes().add( attribute );
            }
        }

        return trackedEntityInstance;
    }
}

