package org.hisp.dhis.webapi.controller.user;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import com.google.common.collect.Sets;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.controller.exception.FilterTooShortException;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.FormUtils;
import org.hisp.dhis.webapi.webdomain.FormDataSet;
import org.hisp.dhis.webapi.webdomain.FormOrganisationUnit;
import org.hisp.dhis.webapi.webdomain.FormProgram;
import org.hisp.dhis.webapi.webdomain.Forms;
import org.hisp.dhis.webapi.webdomain.user.Dashboard;
import org.hisp.dhis.webapi.webdomain.user.Inbox;
import org.hisp.dhis.webapi.webdomain.user.Recipients;
import org.hisp.dhis.webapi.webdomain.user.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = { CurrentUserController.RESOURCE_PATH, "/me" }, method = RequestMethod.GET )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.V23 } )
public class CurrentUserController
{
    public static final String RESOURCE_PATH = "/currentUser";

    private static final int MAX_OBJECTS = 50;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    protected AclService aclService;

    @Autowired
    private DataApprovalLevelService approvalLevelService;

    @Autowired
    private RenderService renderService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected FieldFilterService fieldFilterService;

    @RequestMapping
    public @ResponseBody RootNode getCurrentUser( HttpServletResponse response ) throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.filter( User.class, Collections.singletonList( currentUser ), fields );

        RootNode rootNode = new RootNode( collectionNode.getChildren().get( 0 ) );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    @RequestMapping( value = "/dashboards", produces = { "application/json", "text/*" } )
    public void getDashboards( HttpServletResponse response ) throws NotAuthenticatedException, IOException
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new NotAuthenticatedException();
        }

        List<org.hisp.dhis.dashboard.Dashboard> dashboards = Lists.newArrayList( manager.getAll( org.hisp.dhis.dashboard.Dashboard.class ) );

        for ( org.hisp.dhis.dashboard.Dashboard dashboard : dashboards )
        {
            dashboard.setAccess( aclService.getAccess( dashboard, user ) );

            for ( DashboardItem dashboardItem : dashboard.getItems() )
            {
                dashboardItem.setAccess( aclService.getAccess( dashboardItem, user ) );
            }
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), dashboards );
    }

    @RequestMapping( value = "/inbox", produces = { "application/json", "text/*" } )
    public void getInbox( HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new NotAuthenticatedException();
        }

        Inbox inbox = new Inbox();
        inbox.setMessageConversations( new ArrayList<>( messageService.getMessageConversations( 0, MAX_OBJECTS ) ) );
        inbox.setInterpretations( new ArrayList<>( interpretationService.getInterpretations( 0, MAX_OBJECTS ) ) );

        for ( org.hisp.dhis.message.MessageConversation messageConversation : inbox.getMessageConversations() )
        {
            messageConversation.setAccess( aclService.getAccess( messageConversation, user ) );
        }

        for ( Interpretation interpretation : inbox.getInterpretations() )
        {
            interpretation.setAccess( aclService.getAccess( interpretation, user ) );
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), inbox );
    }

    @RequestMapping( value = "/inbox/messageConversations", produces = { "application/json", "text/*" } )
    public void getInboxMessageConversations( HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new NotAuthenticatedException();
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        List<MessageConversation> messageConversations = new ArrayList<>( messageService.getMessageConversations( 0, MAX_OBJECTS ) );

        for ( org.hisp.dhis.message.MessageConversation messageConversation : messageConversations )
        {
            messageConversation.setAccess( aclService.getAccess( messageConversation, user ) );
        }

        renderService.toJson( response.getOutputStream(), messageConversations );
    }

    @RequestMapping( value = "/inbox/interpretations", produces = { "application/json", "text/*" } )
    public void getInboxInterpretations( HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new NotAuthenticatedException();
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        List<Interpretation> interpretations = new ArrayList<>( interpretationService.getInterpretations( 0, MAX_OBJECTS ) );

        for ( Interpretation interpretation : interpretations )
        {
            interpretation.setAccess( aclService.getAccess( interpretation, user ) );
        }

        renderService.toJson( response.getOutputStream(), interpretations );
    }

    @RequestMapping( value = "/dashboard", produces = { "application/json", "text/*" } )
    public void getDashboard( HttpServletResponse response ) throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setUnreadMessageConversations( messageService.getUnreadMessageConversationCount() );
        dashboard.setUnreadInterpretations( interpretationService.getNewInterpretationCount() );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), dashboard );
    }

    @RequestMapping( value = { "/profile", "/user-account" }, produces = { "application/json", "text/html" } )
    public void getUserAccountJson( HttpServletResponse response ) throws Exception
    {
        UserAccount userAccount = getUserAccount();

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), userAccount );
    }

    @RequestMapping( value = { "/profile", "/user-account" }, produces = { "application/javascript" } )
    public void getUserAccountJsonP( @RequestParam( defaultValue = "callback" ) String callback, HttpServletResponse response, HttpServletRequest request ) throws Exception
    {
        UserAccount userAccount = getUserAccount();

        response.setContentType( "application/javascript" );
        renderService.toJsonP( response.getOutputStream(), userAccount, callback );
    }

    private UserAccount getUserAccount() throws NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        UserAccount userAccount = new UserAccount();

        // user account
        userAccount.setId( currentUser.getUid() );
        userAccount.setUsername( currentUser.getUsername() );
        userAccount.setFirstName( currentUser.getFirstName() );
        userAccount.setSurname( currentUser.getSurname() );
        userAccount.setEmail( currentUser.getEmail() );
        userAccount.setPhoneNumber( currentUser.getPhoneNumber() );

        // profile
        userAccount.setIntroduction( currentUser.getIntroduction() );
        userAccount.setJobTitle( currentUser.getJobTitle() );
        userAccount.setGender( currentUser.getGender() );

        if ( currentUser.getBirthday() != null )
        {
            userAccount.setBirthday( DateUtils.getMediumDateString( currentUser.getBirthday() ) );
        }

        userAccount.setNationality( currentUser.getNationality() );
        userAccount.setEmployer( currentUser.getEmployer() );
        userAccount.setEducation( currentUser.getEducation() );
        userAccount.setInterests( currentUser.getInterests() );
        userAccount.setLanguages( currentUser.getLanguages() );

        userAccount.getSettings().put( UserSettingKey.UI_LOCALE.getName(), TextUtils.toString( userSettingService.getUserSetting( UserSettingKey.UI_LOCALE ) ) );
        userAccount.getSettings().put( UserSettingKey.DB_LOCALE.getName(), TextUtils.toString( userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ) ) );
        userAccount.getSettings().put( UserSettingKey.MESSAGE_EMAIL_NOTIFICATION.getName(), TextUtils.toString( userSettingService.getUserSetting( UserSettingKey.MESSAGE_EMAIL_NOTIFICATION ) ) );
        userAccount.getSettings().put( UserSettingKey.MESSAGE_SMS_NOTIFICATION.getName(), TextUtils.toString( userSettingService.getUserSetting( UserSettingKey.MESSAGE_SMS_NOTIFICATION ) ) );
        userAccount.getSettings().put( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY.getName(), TextUtils.toString( userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY ) ) );
        return userAccount;
    }

    @RequestMapping( value = { "/profile", "/user-account" }, method = RequestMethod.POST, consumes = "application/json" )
    public void postUserAccountJson( HttpServletResponse response, HttpServletRequest request ) throws Exception
    {
        UserAccount userAccount = renderService.fromJson( request.getInputStream(), UserAccount.class );
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        // basic user account
        currentUser.setFirstName( userAccount.getFirstName() );
        currentUser.setSurname( userAccount.getSurname() );
        currentUser.setEmail( userAccount.getEmail() );
        currentUser.setPhoneNumber( userAccount.getPhoneNumber() );

        // profile
        currentUser.setIntroduction( userAccount.getIntroduction() );
        currentUser.setJobTitle( userAccount.getJobTitle() );
        currentUser.setGender( userAccount.getGender() );

        if ( userAccount.getBirthday() != null && !userAccount.getBirthday().isEmpty() )
        {
            currentUser.setBirthday( DateUtils.getMediumDate( userAccount.getBirthday() ) );
        }

        currentUser.setNationality( userAccount.getNationality() );
        currentUser.setEmployer( userAccount.getEmployer() );
        currentUser.setEducation( userAccount.getEducation() );
        currentUser.setInterests( userAccount.getInterests() );
        currentUser.setLanguages( userAccount.getLanguages() );

        userService.updateUser( currentUser );
    }

    @RequestMapping( value = "/authorization", produces = { "application/json", "text/*" } )
    public void getAuthorization( HttpServletResponse response ) throws IOException
    {
        User currentUser = currentUserService.getCurrentUser();

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), currentUser.getUserCredentials().getAllAuthorities() );
    }

    @RequestMapping( value = "/authorization/{auth}", produces = { "application/json", "text/*" } )
    public void hasAuthorization( @PathVariable String auth, HttpServletResponse response ) throws IOException
    {
        User currentUser = currentUserService.getCurrentUser();

        boolean hasAuth = currentUser != null && currentUser.getUserCredentials().isAuthorized( auth );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), hasAuth );
    }

    @RequestMapping( value = "/recipients", produces = { "application/json", "text/*" } )
    public void recipientsJson( HttpServletResponse response,
        @RequestParam( value = "filter" ) String filter ) throws IOException, NotAuthenticatedException, FilterTooShortException
    {
        User currentUser = currentUserService.getCurrentUser();

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.CACHE_1_HOUR );

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        if ( 3 > filter.length() )
        {
            throw new FilterTooShortException();
        }

        Recipients recipients = new Recipients();
        recipients.setOrganisationUnits( new HashSet<>( organisationUnitService.getOrganisationUnitsBetweenByName( filter, 0, MAX_OBJECTS ) ) );

        recipients.setUsers( new HashSet<>( userService.getAllUsersBetweenByName( filter, 0, MAX_OBJECTS ) ) );
        recipients.setUserGroups( new HashSet<>( userGroupService.getUserGroupsBetweenByName( filter, 0, MAX_OBJECTS ) ) );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), recipients );
    }

    @RequestMapping( value = { "/assignedOrganisationUnits", "/organisationUnits" }, produces = { "application/json", "text/*" } )
    public void getAssignedOrganisationUnits( HttpServletResponse response, @RequestParam Map<String, String> parameters ) throws IOException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Set<OrganisationUnit> userOrganisationUnits = new HashSet<>();
        userOrganisationUnits.add( currentUser.getOrganisationUnit() );

        if ( parameters.containsKey( "includeChildren" ) && Boolean.parseBoolean( parameters.get( "includeChildren" ) ) )
        {
            List<OrganisationUnit> children = new ArrayList<>();

            for ( OrganisationUnit organisationUnit : userOrganisationUnits )
            {
                children.addAll( organisationUnit.getChildren() );
            }

            userOrganisationUnits.addAll( children );
        }
        else if ( parameters.containsKey( "includeDescendants" ) && Boolean.parseBoolean( parameters.get( "includeDescendants" ) ) )
        {
            List<OrganisationUnit> children = new ArrayList<>();

            for ( OrganisationUnit organisationUnit : userOrganisationUnits )
            {
                children.addAll( organisationUnitService.getOrganisationUnitWithChildren( organisationUnit.getUid() ) );
            }

            userOrganisationUnits.addAll( children );
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), userOrganisationUnits );
    }

    @RequestMapping( value = { "/assignedPrograms", "/programs" }, produces = { "application/json", "text/*" } )
    public void getPrograms( HttpServletResponse response, @RequestParam Map<String, String> parameters,
        @RequestParam( required = false ) String type )
        throws IOException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Set<OrganisationUnit> userOrganisationUnits = new HashSet<>();
        Set<OrganisationUnit> organisationUnits = new HashSet<>();
        Set<Program> programs = new HashSet<>();
        Map<String, List<Program>> programAssociations = new HashMap<>();
        Set<Program> userPrograms;

        if ( type == null )
        {
            userPrograms = programService.getUserPrograms();
        }
        else
        {
            userPrograms = programService.getUserPrograms( ProgramType.fromValue( type ) );
        }

        if ( currentUserService.currentUserIsSuper() && currentUser.getOrganisationUnits().isEmpty() )
        {
            userOrganisationUnits.addAll( organisationUnitService.getRootOrganisationUnits() );
        }
        else
        {
            userOrganisationUnits.addAll( currentUser.getOrganisationUnits() );
        }

        if ( parameters.containsKey( "includeDescendants" ) && Boolean.parseBoolean( parameters.get( "includeDescendants" ) ) )
        {
            List<OrganisationUnit> children = new ArrayList<>();

            for ( OrganisationUnit organisationUnit : userOrganisationUnits )
            {
                children.addAll( organisationUnitService.getOrganisationUnitWithChildren( organisationUnit.getUid() ) );
            }

            userOrganisationUnits.addAll( children );
        }
        else
        {
            List<OrganisationUnit> children = new ArrayList<>();

            for ( OrganisationUnit organisationUnit : userOrganisationUnits )
            {
                children.addAll( organisationUnit.getChildren() );
            }

            userOrganisationUnits.addAll( children );
        }

        for ( OrganisationUnit organisationUnit : userOrganisationUnits )
        {
            List<Program> ouPrograms = new ArrayList<>( programService.getPrograms( organisationUnit ) );

            if ( !ouPrograms.isEmpty() )
            {
                for ( Program program : ouPrograms )
                {
                    if ( userPrograms.contains( program ) )
                    {
                        organisationUnits.add( organisationUnit );
                        programs.add( program );

                        programAssociations.putIfAbsent( organisationUnit.getUid(), new ArrayList<>() );
                        programAssociations.get( organisationUnit.getUid() ).add( program );
                    }
                }
            }
        }

        Forms forms = new Forms();

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            FormOrganisationUnit formOrganisationUnit = new FormOrganisationUnit();
            formOrganisationUnit.setId( organisationUnit.getUid() );
            formOrganisationUnit.setLabel( organisationUnit.getDisplayName() );
            formOrganisationUnit.setLevel( organisationUnit.getLevel() );

            if ( organisationUnit.getParent() != null )
            {
                formOrganisationUnit.setParent( organisationUnit.getParent().getUid() );
            }

            for ( Program program : programAssociations.get( organisationUnit.getUid() ) )
            {
                FormProgram formProgram = new FormProgram();
                formProgram.setId( program.getUid() );
                formProgram.setLabel( program.getDisplayName() );

                formOrganisationUnit.getPrograms().add( formProgram );
            }

            forms.getOrganisationUnits().put( formOrganisationUnit.getId(), formOrganisationUnit );
        }

        for ( Program program : programs )
        {
            forms.getForms().put( program.getUid(), FormUtils.fromProgram( program ) );
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), forms );
    }

    @RequestMapping( value = { "/assignedDataSets", "/dataSets" }, produces = { "application/json", "text/*" } )
    public void getDataSets( @RequestParam( defaultValue = "false" ) boolean optionSets, @RequestParam( defaultValue = "50" ) int maxOptions,
        HttpServletResponse response, @RequestParam Map<String, String> parameters ) throws IOException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Forms forms = new Forms();

        Set<OrganisationUnit> organisationUnits = new HashSet<>();
        Set<DataSet> userDataSets;
        Set<OrganisationUnit> userOrganisationUnits = new HashSet<>( currentUser.getOrganisationUnits() );

        if ( currentUser.getUserCredentials().getAllAuthorities().contains( "ALL" ) )
        {
            userDataSets = new HashSet<>( dataSetService.getAllDataSets() );

            if ( userOrganisationUnits.isEmpty() )
            {
                userOrganisationUnits = new HashSet<>( organisationUnitService.getRootOrganisationUnits() );
            }
        }
        else
        {
            userDataSets = currentUser.getUserCredentials().getAllDataSets();
        }

        if ( parameters.containsKey( "includeDescendants" ) && Boolean.parseBoolean( parameters.get( "includeDescendants" ) ) )
        {
            List<OrganisationUnit> children = new ArrayList<>();

            for ( OrganisationUnit organisationUnit : userOrganisationUnits )
            {
                children.addAll( organisationUnitService.getOrganisationUnitWithChildren( organisationUnit.getUid() ) );
            }

            userOrganisationUnits.addAll( children );
        }
        else
        {
            List<OrganisationUnit> children = new ArrayList<>();

            for ( OrganisationUnit organisationUnit : userOrganisationUnits )
            {
                children.addAll( organisationUnit.getChildren() );
            }

            userOrganisationUnits.addAll( children );
        }

        for ( OrganisationUnit ou : userOrganisationUnits )
        {
            Set<DataSet> dataSets = new HashSet<>( Sets.intersection( ou.getDataSets(), userDataSets ) );

            if ( dataSets.size() > 0 )
            {
                organisationUnits.add( ou );
            }
        }

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            FormOrganisationUnit formOrganisationUnit = new FormOrganisationUnit();
            formOrganisationUnit.setId( organisationUnit.getUid() );
            formOrganisationUnit.setLabel( organisationUnit.getDisplayName() );
            formOrganisationUnit.setLevel( organisationUnit.getLevel() );

            if ( organisationUnit.getParent() != null )
            {
                formOrganisationUnit.setParent( organisationUnit.getParent().getUid() );
            }

            Set<DataSet> dataSets = new HashSet<>( Sets.intersection( organisationUnit.getDataSets(), userDataSets ) );

            for ( DataSet dataSet : dataSets )
            {
                String uid = dataSet.getUid();

                FormDataSet formDataSet = new FormDataSet();
                formDataSet.setId( uid );
                formDataSet.setLabel( dataSet.getDisplayName() );

                dataSet.getCategoryCombo().getCategories().forEach( cat -> {
                    cat.setAccess( aclService.getAccess( cat, currentUser ) );
                    cat.getCategoryOptions().forEach( catOpts -> catOpts.setAccess( aclService.getAccess( catOpts, currentUser ) ) );
                });

                forms.getForms().put( uid, FormUtils.fromDataSet( dataSet, false, userOrganisationUnits ) );
                formOrganisationUnit.getDataSets().add( formDataSet );

                if ( optionSets )
                {
                    for ( DataElement dataElement : dataSet.getDataElements() )
                    {
                        if ( dataElement.hasOptionSet() )
                        {
                            int size = maxOptions;

                            if ( size >= dataElement.getOptionSet().getOptions().size() )
                            {
                                size = dataElement.getOptionSet().getOptions().size();
                            }

                            forms.getOptionSets().put( dataElement.getOptionSet().getUid(), dataElement.getOptionSet().getOptionValues().subList( 0, size - 1 ) );
                        }
                    }
                }
            }

            forms.getOrganisationUnits().put( formOrganisationUnit.getId(), formOrganisationUnit );
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), forms );
    }

    @RequestMapping( value = "/dataApprovalLevels", produces = { "application/json", "text/*" } )
    public void getApprovalLevels( HttpServletResponse response ) throws IOException
    {
        List<DataApprovalLevel> approvalLevels = approvalLevelService.getUserDataApprovalLevels();
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), approvalLevels );
    }

    @RequestMapping( value = "/readDataApprovalLevels", produces = { "application/json", "text/*" } )
    public void getReadApprovalLevels( HttpServletResponse response ) throws IOException
    {
        Map<OrganisationUnit, Integer> orgUnitApprovalLevelMap = approvalLevelService.getUserReadApprovalLevels();
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), orgUnitApprovalLevelMap );
    }
}
