package org.hisp.dhis.dxf2.events.trackedentity;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    protected org.hisp.dhis.trackedentity.TrackedEntityInstanceService entityInstanceService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    protected EnrollmentService enrollmentService;

    private final CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private final CachingMap<String, TrackedEntity> trackedEntityCache = new CachingMap<>();

    private final CachingMap<String, TrackedEntityAttribute> trackedEntityAttributeCache = new CachingMap<>();

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> teis = entityInstanceService.getTrackedEntityInstances( params );

        List<TrackedEntityInstance> teiItems = new ArrayList<>();

        for ( org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance : teis )
        {
            teiItems.add( getTrackedEntityInstance( trackedEntityInstance, false ) );
        }

        return teiItems;
    }

    @Override
    public int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params )
    {
        return entityInstanceService.getTrackedEntityInstanceCount( params );
    }

    @Override
    public TrackedEntityInstance getTrackedEntityInstance( String uid )
    {
        return getTrackedEntityInstance( teiService.getTrackedEntityInstance( uid ) );
    }

    @Override
    public TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance )
    {
        return getTrackedEntityInstance( entityInstance, true );
    }

    @Override
    public TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance, boolean includeRelationships )
    {
        if ( entityInstance == null )
        {
            return null;
        }

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setTrackedEntityInstance( entityInstance.getUid() );
        trackedEntityInstance.setOrgUnit( entityInstance.getOrganisationUnit().getUid() );
        trackedEntityInstance.setTrackedEntity( entityInstance.getTrackedEntity().getUid() );
        trackedEntityInstance.setCreated( entityInstance.getCreated().toString() );
        trackedEntityInstance.setLastUpdated( DateUtils.getLongGmtDateString( entityInstance.getLastUpdated() ) );
        trackedEntityInstance.setInactive( entityInstance.isInactive() );

        if ( includeRelationships )
        {
            //TODO include relationships in data model and void transactional query in for-loop

            Collection<Relationship> relationships = relationshipService.getRelationshipsForTrackedEntityInstance( entityInstance );

            for ( Relationship entityRelationship : relationships )
            {
                org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship = new org.hisp.dhis.dxf2.events.trackedentity.Relationship();
                relationship.setDisplayName( entityRelationship.getRelationshipType().getDisplayName() );
                relationship.setTrackedEntityInstanceA( entityRelationship.getEntityInstanceA().getUid() );
                relationship.setTrackedEntityInstanceB( entityRelationship.getEntityInstanceB().getUid() );

                relationship.setRelationship( entityRelationship.getRelationshipType().getUid() );

                // we might have cases where A <=> A, so we only include the relative if the UIDs do not match
                if ( !entityRelationship.getEntityInstanceA().getUid().equals( entityInstance.getUid() ) )
                {
                    relationship.setRelative( getTrackedEntityInstance( entityRelationship.getEntityInstanceA(), false ) );
                }
                else if ( !entityRelationship.getEntityInstanceB().getUid().equals( entityInstance.getUid() ) )
                {
                    relationship.setRelative( getTrackedEntityInstance( entityRelationship.getEntityInstanceB(), false ) );
                }

                trackedEntityInstance.getRelationships().add( relationship );
            }
        }

        for ( TrackedEntityAttributeValue attributeValue : entityInstance.getTrackedEntityAttributeValues() )
        {
            Attribute attribute = new Attribute();

            attribute.setCreated( DateUtils.getLongGmtDateString( attributeValue.getCreated() ) );
            attribute.setLastUpdated( DateUtils.getLongGmtDateString( attributeValue.getLastUpdated() ) );
            attribute.setDisplayName( attributeValue.getAttribute().getDisplayName() );
            attribute.setAttribute( attributeValue.getAttribute().getUid() );
            attribute.setValueType( attributeValue.getAttribute().getValueType() );
            attribute.setCode( attributeValue.getAttribute().getCode() );
            attribute.setValue( attributeValue.getValue() );

            trackedEntityInstance.getAttributes().add( attribute );
        }

        return trackedEntityInstance;
    }

    public org.hisp.dhis.trackedentity.TrackedEntityInstance getTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions, ImportSummary importSummary )
    {
        if ( StringUtils.isEmpty( trackedEntityInstance.getOrgUnit() ) )
        {
            importSummary.getConflicts().add( new ImportConflict( trackedEntityInstance.getTrackedEntityInstance(), "No org unit ID in tracked entity instance object." ) );
            return null;
        }

        org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = new org.hisp.dhis.trackedentity.TrackedEntityInstance();

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), trackedEntityInstance.getOrgUnit() );

        if ( organisationUnit == null )
        {
            importSummary.getConflicts().add( new ImportConflict( trackedEntityInstance.getTrackedEntityInstance(), "Invalid org unit ID: " + trackedEntityInstance.getOrgUnit() ) );
            return null;
        }

        entityInstance.setOrganisationUnit( organisationUnit );

        TrackedEntity trackedEntity = getTrackedEntity( importOptions.getIdSchemes(), trackedEntityInstance.getTrackedEntity() );

        if ( trackedEntity == null )
        {
            importSummary.getConflicts().add( new ImportConflict( trackedEntityInstance.getTrackedEntityInstance(), "Invalid tracked entity ID: " + trackedEntityInstance.getTrackedEntity() ) );
            return null;
        }

        entityInstance.setTrackedEntity( trackedEntity );
        entityInstance.setUid( CodeGenerator.isValidCode( trackedEntityInstance.getTrackedEntityInstance() ) ?
            trackedEntityInstance.getTrackedEntityInstance() : CodeGenerator.generateCode() );

        return entityInstance;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries addTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        for ( TrackedEntityInstance trackedEntityInstance : trackedEntityInstances )
        {
            importSummaries.addImportSummary( addTrackedEntityInstance( trackedEntityInstance, importOptions ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    @Override
    public ImportSummary addTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummary importSummary = new ImportSummary();

        trackedEntityInstance.trimValuesToNull();

        Set<ImportConflict> importConflicts = new HashSet<>();
        importConflicts.addAll( checkTrackedEntity( trackedEntityInstance, importOptions ) );
        importConflicts.addAll( checkAttributes( trackedEntityInstance, importOptions ) );

        importSummary.setConflicts( importConflicts );

        if ( !importConflicts.isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();
            return importSummary;
        }

        org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = getTrackedEntityInstance( trackedEntityInstance, importOptions, importSummary );

        if ( entityInstance == null )
        {
            return importSummary;
        }

        teiService.addTrackedEntityInstance( entityInstance );

        updateRelationships( trackedEntityInstance, entityInstance );
        updateAttributeValues( trackedEntityInstance, entityInstance );
        teiService.updateTrackedEntityInstance( entityInstance );

        importSummary.setReference( entityInstance.getUid() );
        importSummary.getImportCount().incrementImported();

        for ( Enrollment enrollment : trackedEntityInstance.getEnrollments() )
        {
            enrollment.setTrackedEntity( trackedEntityInstance.getTrackedEntity() );
            enrollment.setTrackedEntityInstance( entityInstance.getUid() );
        }

        ImportSummaries importSummaries = enrollmentService.addEnrollments( trackedEntityInstance.getEnrollments(), importOptions );
        importSummary.setEnrollments( importSummaries );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries updateTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        for ( TrackedEntityInstance trackedEntityInstance : trackedEntityInstances )
        {
            importSummaries.addImportSummary( updateTrackedEntityInstance( trackedEntityInstance, importOptions ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    @Override
    public ImportSummary updateTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummary importSummary = new ImportSummary();

        trackedEntityInstance.trimValuesToNull();

        Set<ImportConflict> importConflicts = new HashSet<>();
        importConflicts.addAll( checkRelationships( trackedEntityInstance ) );
        importConflicts.addAll( checkAttributes( trackedEntityInstance, importOptions ) );

        org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = manager.get( org.hisp.dhis.trackedentity.TrackedEntityInstance.class,
            trackedEntityInstance.getTrackedEntityInstance() );

        if ( entityInstance == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance", "trackedEntityInstance " + trackedEntityInstance.getTrackedEntityInstance()
                + " does not point to valid trackedEntityInstance" ) );
        }

        OrganisationUnit organisationUnit = manager.get( OrganisationUnit.class, trackedEntityInstance.getOrgUnit() );

        if ( organisationUnit == null )
        {
            importConflicts.add( new ImportConflict( "OrganisationUnit", "orgUnit " + trackedEntityInstance.getOrgUnit()
                + " does not point to valid organisation unit" ) );
        }

        importSummary.setConflicts( importConflicts );

        if ( !importConflicts.isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();

            return importSummary;
        }

        entityInstance.setInactive( trackedEntityInstance.isInactive() );

        removeRelationships( entityInstance );
        removeAttributeValues( entityInstance );
        teiService.updateTrackedEntityInstance( entityInstance );

        updateRelationships( trackedEntityInstance, entityInstance );
        updateAttributeValues( trackedEntityInstance, entityInstance );
        teiService.updateTrackedEntityInstance( entityInstance );

        importSummary.setStatus( ImportStatus.SUCCESS );
        importSummary.setReference( entityInstance.getUid() );
        importSummary.getImportCount().incrementUpdated();

        for ( Enrollment enrollment : trackedEntityInstance.getEnrollments() )
        {
            enrollment.setTrackedEntity( trackedEntityInstance.getTrackedEntity() );
            enrollment.setTrackedEntityInstance( entityInstance.getUid() );
        }

        importOptions.setStrategy( ImportStrategy.CREATE_AND_UPDATE );
        ImportSummaries importSummaries = enrollmentService.addEnrollments( trackedEntityInstance.getEnrollments(), importOptions );
        importSummary.setEnrollments( importSummaries );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary deleteTrackedEntityInstance( String uid )
    {
        org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = teiService.getTrackedEntityInstance( uid );

        if ( entityInstance != null )
        {
            teiService.deleteTrackedEntityInstance( entityInstance );
            return new ImportSummary( ImportStatus.SUCCESS, "Deletion of tracked entity instance " + uid + " was successful" ).incrementDeleted();
        }

        return new ImportSummary( ImportStatus.ERROR, "ID " + uid + " does not point to a valid tracked entity instance" ).incrementIgnored();
    }

    @Override
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

    private void updateAttributeValues( TrackedEntityInstance trackedEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance )
    {
        for ( Attribute attribute : trackedEntityInstance.getAttributes() )
        {
            TrackedEntityAttribute entityAttribute = manager.get( TrackedEntityAttribute.class,
                attribute.getAttribute() );

            if ( entityAttribute != null )
            {
                TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
                attributeValue.setEntityInstance( entityInstance );
                attributeValue.setValue( attribute.getValue() );
                attributeValue.setAttribute( entityAttribute );

                trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValue );
            }
        }
    }

    private void updateRelationships( TrackedEntityInstance trackedEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance )
    {
        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship : trackedEntityInstance.getRelationships() )
        {
            org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstanceA = manager.get( org.hisp.dhis.trackedentity.TrackedEntityInstance.class, relationship.getTrackedEntityInstanceA() );
            org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstanceB = manager.get( org.hisp.dhis.trackedentity.TrackedEntityInstance.class, relationship.getTrackedEntityInstanceB() );

            RelationshipType relationshipType = manager.get( RelationshipType.class, relationship.getRelationship() );

            Relationship entityRelationship = new Relationship();
            entityRelationship.setEntityInstanceA( entityInstanceA );
            entityRelationship.setEntityInstanceB( entityInstanceB );
            entityRelationship.setRelationshipType( relationshipType );

            relationshipService.addRelationship( entityRelationship );
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

    private TrackedEntity getTrackedEntity( IdSchemes idSchemes, String id )
    {
        return trackedEntityCache.get( id, () -> manager.getObject( TrackedEntity.class, idSchemes.getTrackedEntityIdScheme(), id ) );
    }

    private TrackedEntityAttribute getTrackedEntityAttribute( IdSchemes idSchemes, String id )
    {
        return trackedEntityAttributeCache.get( id, () -> manager.getObject( TrackedEntityAttribute.class, idSchemes.getTrackedEntityAttributeIdScheme(), id ) );
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

        TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(), attribute.getAttribute() );

        if ( trackedEntityAttribute == null )
        {
            importConflicts.add( new ImportConflict( "Attribute.attribute", "Does not point to a valid attribute." ) );
            return importConflicts;
        }

        String errorMessage = trackedEntityAttributeService.validateValueType( trackedEntityAttribute, attribute.getValue() );

        if ( errorMessage != null )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", errorMessage ) );
        }

        return importConflicts;
    }

    private List<ImportConflict> checkRelationships( TrackedEntityInstance trackedEntityInstance )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        for ( org.hisp.dhis.dxf2.events.trackedentity.Relationship relationship : trackedEntityInstance.getRelationships() )
        {
            RelationshipType relationshipType = manager.get( RelationshipType.class, relationship.getRelationship() );

            if ( relationshipType == null )
            {
                importConflicts.add( new ImportConflict( "Relationship.type", "Invalid type " + relationship.getRelationship() ) );
            }

            org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstanceA = manager.get( org.hisp.dhis.trackedentity.TrackedEntityInstance.class, relationship.getTrackedEntityInstanceA() );

            if ( entityInstanceA == null )
            {
                importConflicts.add( new ImportConflict( "Relationship.trackedEntityInstance", "Invalid trackedEntityInstance "
                    + relationship.getTrackedEntityInstanceA() ) );
            }

            org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstanceB = manager.get( org.hisp.dhis.trackedentity.TrackedEntityInstance.class, relationship.getTrackedEntityInstanceB() );

            if ( entityInstanceB == null )
            {
                importConflicts.add( new ImportConflict( "Relationship.trackedEntityInstance", "Invalid trackedEntityInstance "
                    + relationship.getTrackedEntityInstanceB() ) );
            }
        }

        return importConflicts;
    }

    private List<ImportConflict> checkScope( org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance,
        TrackedEntityAttribute trackedEntityAttribute, String value, OrganisationUnit organisationUnit )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        if ( trackedEntityAttribute == null || value == null )
        {
            return importConflicts;
        }

        String errorMessage = trackedEntityAttributeService.validateScope( trackedEntityAttribute, value, trackedEntityInstance,
            organisationUnit, null );

        if ( errorMessage != null )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", errorMessage ) );
        }

        return importConflicts;
    }

    private List<ImportConflict> checkAttributes( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        for ( Attribute attribute : trackedEntityInstance.getAttributes() )
        {
            TrackedEntityAttribute entityAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(), attribute.getAttribute() );

            if ( entityAttribute == null )
            {
                importConflicts.add( new ImportConflict( "Attribute.attribute", "Invalid attribute " + attribute.getAttribute() ) );
                continue;
            }

            if ( entityAttribute.isUnique() )
            {
                OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), trackedEntityInstance.getOrgUnit() );
                org.hisp.dhis.trackedentity.TrackedEntityInstance tei = teiService.getTrackedEntityInstance( trackedEntityInstance.getTrackedEntityInstance() );
                importConflicts.addAll( checkScope( tei, entityAttribute, attribute.getValue(), organisationUnit ) );
            }

            importConflicts.addAll( validateAttributeType( attribute, importOptions ) );
        }

        return importConflicts;
    }

    private List<ImportConflict> checkTrackedEntity( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        if ( trackedEntityInstance.getTrackedEntity() == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance.trackedEntity", "Missing required property trackedEntity" ) );
            return importConflicts;
        }

        TrackedEntity trackedEntity = getTrackedEntity( importOptions.getIdSchemes(), trackedEntityInstance.getTrackedEntity() );

        if ( trackedEntity == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance.trackedEntity", "Invalid trackedEntity" +
                trackedEntityInstance.getTrackedEntity() ) );
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
}
