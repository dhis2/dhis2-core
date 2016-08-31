package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.hisp.dhis.i18n.I18nUtils.i18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.validation.ValidationCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultProgramService
    implements ProgramService
{
    private static final String TAG_OPEN = "<";

    private static final String TAG_CLOSE = "/>";

    private static final String PROGRAM_INCIDENT_DATE = "incidentDate";

    private static final String PROGRAM_ENROLLMENT_DATE = "enrollmentDate";

    private static final String DOB_FIELD = "@DOB_FIELD";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStore programStore;

    public void setProgramStore( ProgramStore programStore )
    {
        this.programStore = programStore;
    }

    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }
    
    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addProgram( Program program )
    {
        return programStore.save( program );
    }

    @Override
    public void updateProgram( Program program )
    {
        programStore.update( program );
    }

    @Override
    public void deleteProgram( Program program )
    {
        programStore.delete( program );
    }

    @Override
    public List<Program> getAllPrograms()
    {
        return i18n( i18nService, programStore.getAll() );
    }

    @Override
    public Program getProgram( int id )
    {
        return i18n( i18nService, programStore.get( id ) );
    }

    @Override
    public Program getProgramByName( String name )
    {
        return i18n( i18nService, programStore.getByName( name ) );
    }

    @Override
    public List<Program> getPrograms( OrganisationUnit organisationUnit )
    {
        return i18n( i18nService, programStore.get( organisationUnit ) );
    }

    @Override
    public List<Program> getPrograms( ValidationCriteria validationCriteria )
    {
        List<Program> programs = new ArrayList<>();

        for ( Program program : getAllPrograms() )
        {
            if ( program.getValidationCriteria().contains( validationCriteria ) )
            {
                programs.add( program );
            }
        }

        return i18n( i18nService, programs );
    }

    @Override
    public List<Program> getPrograms( ProgramType type )
    {
        return i18n( i18nService, programStore.getByType( type ) );
    }

    @Override
    public List<Program> getPrograms( ProgramType type, OrganisationUnit orgunit )
    {
        return i18n( i18nService, programStore.get( type, orgunit ) );
    }

    @Override
    public List<Program> getProgramsByCurrentUser()
    {
        return i18n( i18nService, getByCurrentUser() );
    }

    @Override
    public List<Program> getProgramsByUser( User user )
    {
        return i18n( i18nService, getByUser( user ) );
    }

    @Override
    public List<Program> getProgramsByCurrentUser( ProgramType type )
    {
        return i18n( i18nService, getByCurrentUser( type ) );
    }

    @Override
    public Program getProgram( String uid )
    {
        return i18n( i18nService, programStore.getByUid( uid ) );
    }

    @Override
    public List<Program> getProgramsByCurrentUser( OrganisationUnit organisationUnit )
    {
        List<Program> programs = new ArrayList<>( getPrograms( organisationUnit ) );
        programs.retainAll( getProgramsByCurrentUser() );

        return programs;
    }

    @Override
    public List<Program> getProgramsByTrackedEntity( TrackedEntity trackedEntity )
    {
        return i18n( i18nService, programStore.getByTrackedEntity( trackedEntity ) );
    }

    @Override
    public Integer getProgramCountByName( String name )
    {
        return programStore.getCountLikeName( name );
    }

    @Override
    public List<Program> getProgramBetweenByName( String name, int min, int max )
    {
        return i18n( i18nService, programStore.getAllLikeName( name, min, max ) );
    }

    @Override
    public Integer getProgramCount()
    {
        return programStore.getCount();
    }

    @Override
    public List<Program> getProgramsBetween( int min, int max )
    {
        return i18n( i18nService, programStore.getAllOrderedName( min, max ) );
    }

    @Override
    public List<Program> getByCurrentUser()
    {
        return getByUser( currentUserService.getCurrentUser() );
    }

    public List<Program> getByUser( User user )
    {
        List<Program> programs = new ArrayList<>();

        if ( user != null && !user.isSuper() )
        {
            Set<UserAuthorityGroup> userRoles = userService.getUserCredentials( currentUserService.getCurrentUser() )
                .getUserAuthorityGroups();

            for ( Program program : programStore.getAll() )
            {
                if ( Sets.intersection( program.getUserRoles(), userRoles ).size() > 0 )
                {
                    programs.add( program );
                }
            }
        }
        else
        {
            programs = programStore.getAll();
        }

        return programs;
    }

    @Override
    public List<Program> getByCurrentUser( ProgramType type )
    {
        List<Program> programs = new ArrayList<>();

        if ( currentUserService.getCurrentUser() != null && !currentUserService.currentUserIsSuper() )
        {
            Set<UserAuthorityGroup> userRoles = userService.getUserCredentials( currentUserService.getCurrentUser() )
                .getUserAuthorityGroups();

            for ( Program program : programStore.getByType( type ) )
            {
                if ( Sets.intersection( program.getUserRoles(), userRoles ).size() > 0 )
                {
                    programs.add( program );
                }
            }
        }
        else
        {
            programs = programStore.getByType( type );
        }

        return programs;
    }

    @Override
    public void mergeWithCurrentUserOrganisationUnits( Program program, Collection<OrganisationUnit> mergeOrganisationUnits )
    {
        Set<OrganisationUnit> selectedOrgUnits = Sets.newHashSet( program.getOrganisationUnits() );
        
        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setParents( currentUserService.getCurrentUser().getOrganisationUnits() );

        Set<OrganisationUnit> userOrganisationUnits = Sets.newHashSet( organisationUnitService.getOrganisationUnitsByQuery( params ) );

        selectedOrgUnits.removeAll( userOrganisationUnits );
        selectedOrgUnits.addAll( mergeOrganisationUnits );

        program.updateOrganisationUnits( selectedOrgUnits );

        updateProgram( program );
    }

    @Override
    public String prepareDataEntryFormForAdd( String htmlCode, Program program, Collection<User> healthWorkers,
        TrackedEntityInstance instance, ProgramInstance programInstance, I18n i18n, I18nFormat format )
    {
        int index = 1;

        StringBuffer sb = new StringBuffer();

        Matcher inputMatcher = INPUT_PATTERN.matcher( htmlCode );

        boolean hasBirthdate = false;
        boolean hasAge = false;

        while ( inputMatcher.find() )
        {
            // -----------------------------------------------------------------
            // Get HTML input field code
            // -----------------------------------------------------------------

            String inputHtml = inputMatcher.group();
            Matcher dynamicAttrMatcher = DYNAMIC_ATTRIBUTE_PATTERN.matcher( inputHtml );
            Matcher programMatcher = PROGRAM_PATTERN.matcher( inputHtml );

            index++;

            String hidden = "";
            String style = "";
            Matcher classMarcher = CLASS_PATTERN.matcher( inputHtml );
            if ( classMarcher.find() )
            {
                hidden = classMarcher.group( 2 );
            }

            Matcher styleMarcher = STYLE_PATTERN.matcher( inputHtml );
            if ( styleMarcher.find() )
            {
                style = styleMarcher.group( 2 );
            }

            if ( dynamicAttrMatcher.find() && dynamicAttrMatcher.groupCount() > 0 )
            {
                String uid = dynamicAttrMatcher.group( 1 );
                TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( uid );

                if ( attribute == null )
                {
                    inputHtml = "<input value='[" + i18n.getString( "missing_instance_attribute" ) + " " + uid
                        + "]' title='[" + i18n.getString( "missing_instance_attribute" ) + " " + uid + "]'>/";
                }
                else
                {
                    // Get value
                    String value = "";
                    if ( instance != null )
                    {
                        TrackedEntityAttributeValue attributeValue = attributeValueService
                            .getTrackedEntityAttributeValue( instance, attribute );
                        if ( attributeValue != null )
                        {
                            value = attributeValue.getValue();
                        }
                    }

                    inputHtml = getAttributeField( inputHtml, attribute, program, value, i18n, index, hidden, style );

                }

            }
            else if ( programMatcher.find() && programMatcher.groupCount() > 0 )
            {
                String property = programMatcher.group( 1 );

                // Get value
                String value = "";
                if ( programInstance != null )
                {
                    value = format.formatDate( ((Date) getValueFromProgram( StringUtils.capitalize( property ),
                        programInstance )) );
                }

                inputHtml = "<input id=\"" + property + "\" name=\"" + property + "\" tabindex=\"" + index
                    + "\" value=\"" + value + "\" " + TAG_CLOSE;
                if ( property.equals( PROGRAM_ENROLLMENT_DATE ) )
                {
                    if ( program != null && program.getSelectEnrollmentDatesInFuture() )
                    {
                        inputHtml += "<script>datePicker(\"" + property + "\", true);</script>";
                    }
                    else
                    {
                        inputHtml += "<script>datePickerValid(\"" + property + "\", true);</script>";
                    }
                }
                else if ( property.equals( PROGRAM_INCIDENT_DATE ) )
                {
                    if ( program != null && program.getSelectIncidentDatesInFuture() )
                    {
                        inputHtml += "<script>datePicker(\"" + property + "\", true);</script>";
                    }
                    else
                    {
                        inputHtml += "<script>datePickerValid(\"" + property + "\", true);</script>";
                    }
                }
            }

            inputMatcher.appendReplacement( sb, inputHtml );
        }

        inputMatcher.appendTail( sb );

        String entryForm = sb.toString();
        String dobType = "";
        if ( hasBirthdate && hasAge )
        {
            dobType = "<select id=\'dobType\' name=\"dobType\" style=\'width:120px\' onchange=\'dobTypeOnChange(\"instanceForm\")\' >";
            dobType += "<option value=\"V\" >" + i18n.getString( "verified" ) + "</option>";
            dobType += "<option value=\"D\" >" + i18n.getString( "declared" ) + "</option>";
            dobType += "<option value=\"A\" >" + i18n.getString( "approximated" ) + "</option>";
            dobType += "</select>";
        }
        else if ( hasBirthdate )
        {
            dobType = "<input type=\'hidden\' id=\'dobType\' name=\"dobType\" value=\'V\'>";
        }
        else if ( hasAge )
        {
            dobType = "<input type=\'hidden\' id=\'dobType\' name=\"dobType\" value=\'A\'>";
        }

        entryForm = entryForm.replaceFirst( DOB_FIELD, dobType );
        entryForm = entryForm.replaceAll( DOB_FIELD, "" );

        return entryForm;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getAttributeField( String inputHtml, TrackedEntityAttribute attribute, Program program,
        String value, I18n i18n, int index, String hidden, String style )
    {
        boolean mandatory = false;
        boolean allowDateInFuture = false;

        if ( program != null && program.getAttribute( attribute ) != null )
        {
            ProgramTrackedEntityAttribute programAttribute = program.getAttribute( attribute );
            mandatory = programAttribute.isMandatory();
            allowDateInFuture = programAttribute.getAllowFutureDate();
        }

        inputHtml = TAG_OPEN + "input id=\"attr" + attribute.getId() + "\" name=\"attr" + attribute.getId()
            + "\" tabindex=\"" + index + "\" style=\"" + style + "\"";

        inputHtml += "\" class=\"" + hidden + " {validate:{required:" + mandatory;

        if ( ValueType.NUMBER == attribute.getValueType() )
        {
            inputHtml += ",number:true";
        }
        else if ( ValueType.PHONE_NUMBER == attribute.getValueType() )
        {
            inputHtml += ",phone:true";
        }

        inputHtml += "}}\" ";

        if ( ValueType.PHONE_NUMBER == attribute.getValueType() )
        {
            inputHtml += " phoneNumber value=\"" + value + "\"" + TAG_CLOSE;
        }
        else if ( ValueType.TRUE_ONLY == attribute.getValueType() )
        {
            inputHtml += " type='checkbox' value='true' ";
            if ( value.equals( "true" ) )
            {
                inputHtml += " checked ";
            }
        }
        else if ( ValueType.BOOLEAN == attribute.getValueType() )
        {
            inputHtml = inputHtml.replaceFirst( "input", "select" ) + ">";

            if ( value.equals( "" ) )
            {
                inputHtml += "<option value=\"\" selected>" + i18n.getString( "no_value" ) + "</option>";
                inputHtml += "<option value=\"true\">" + i18n.getString( "yes" ) + "</option>";
                inputHtml += "<option value=\"false\">" + i18n.getString( "no" ) + "</option>";
            }
            else if ( value.equals( "true" ) )
            {
                inputHtml += "<option value=\"\">" + i18n.getString( "no_value" ) + "</option>";
                inputHtml += "<option value=\"true\" selected >" + i18n.getString( "yes" ) + "</option>";
                inputHtml += "<option value=\"false\">" + i18n.getString( "no" ) + "</option>";
            }
            else if ( value.equals( "false" ) )
            {
                inputHtml += "<option value=\"\">" + i18n.getString( "no_value" ) + "</option>";
                inputHtml += "<option value=\"true\">" + i18n.getString( "yes" ) + "</option>";
                inputHtml += "<option value=\"false\" selected >" + i18n.getString( "no" ) + "</option>";
            }

            inputHtml += "</select>";
        }
        else if ( attribute.hasOptionSet() )
        {
            inputHtml = inputHtml.replaceFirst( "input", "select" ) + ">";
            inputHtml += "<option value=\"\" selected>" + i18n.getString( "no_value" ) + "</option>";
            for ( Option option : attribute.getOptionSet().getOptions() )
            {
                String optionValue = option.getName();
                inputHtml += "<option value=\"" + optionValue + "\" ";
                if ( optionValue.equals( value ) )
                {
                    inputHtml += " selected ";
                }
                inputHtml += ">" + optionValue + "</option>";
            }
            inputHtml += "</select>";
        }
        else if ( ValueType.DATE == attribute.getValueType() )
        {
            String jQueryCalendar = "<script>";
            if ( allowDateInFuture )
            {
                jQueryCalendar += "datePicker";
            }
            else
            {
                jQueryCalendar += "datePickerValid";
            }
            jQueryCalendar += "(\"attr" + attribute.getId() + "\", false, false);</script>";

            inputHtml += " value=\"" + value + "\"" + TAG_CLOSE;
            inputHtml += jQueryCalendar;
        }
        else
        {
            inputHtml += " value=\"" + value + "\"" + TAG_CLOSE;
        }

        return inputHtml;
    }

    private Object getValueFromProgram( String property, ProgramInstance programInstance )
    {
        try
        {
            return ProgramInstance.class.getMethod( "get" + property ).invoke( programInstance );
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }
}
