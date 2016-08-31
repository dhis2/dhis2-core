/* global dhis2, angular, selection, i18n_ajax_login_failed, _ */

dhis2.util.namespace('dhis2.tc');

// whether current user has any organisation units
dhis2.tc.emptyOrganisationUnits = false;

var i18n_no_orgunits = 'No organisation unit attached to current user, no data entry possible';
var i18n_offline_notification = 'You are offline';
var i18n_online_notification = 'You are online';
var i18n_ajax_login_failed = 'Login failed, check your username and password and try again';

var optionSetsInPromise = [];
var attributesInPromise = [];

dhis2.tc.store = null;
dhis2.tc.metaDataCached = false;
dhis2.tc.memoryOnly = $('html').hasClass('ie7') || $('html').hasClass('ie8');
var adapters = [];    
if( dhis2.tc.memoryOnly ) {
    adapters = [ dhis2.storage.InMemoryAdapter ];
} else {
    adapters = [ dhis2.storage.IndexedDBAdapter, dhis2.storage.DomLocalStorageAdapter, dhis2.storage.InMemoryAdapter ];
}

dhis2.tc.store = new dhis2.storage.Store({
    name: 'dhis2tc',
    adapters: [dhis2.storage.IndexedDBAdapter, dhis2.storage.DomSessionStorageAdapter, dhis2.storage.InMemoryAdapter],
    objectStores: ['programs', 'programStages', 'trackedEntities', 'attributes', 'attributeGroups','relationshipTypes', 'optionSets', 'programValidations', 'programIndicators', 'ouLevels', 'programRuleVariables', 'programRules','constants']      
});

(function($) {
    $.safeEach = function(arr, fn)
    {
        if (arr)
        {
            $.each(arr, fn);
        }
    };
})(jQuery);

/**
 * Page init. The order of events is:
 *
 * 1. Load ouwt 
 * 2. Load meta-data (and notify ouwt) 
 * 
 */
$(document).ready(function()
{
    $.ajaxSetup({
        type: 'POST',
        cache: false
    });

    $('#loaderSpan').show();
});

$(document).bind('dhis2.online', function(event, loggedIn)
{
    if (loggedIn)
    {
        if (dhis2.tc.emptyOrganisationUnits) {
            setHeaderMessage(i18n_no_orgunits);
        }
        else {
            setHeaderDelayMessage(i18n_online_notification);
        }
    }
    else
    {
        var form = [
            '<form style="display:inline;">',
            '<label for="username">Username</label>',
            '<input name="username" id="username" type="text" style="width: 70px; margin-left: 10px; margin-right: 10px" size="10"/>',
            '<label for="password">Password</label>',
            '<input name="password" id="password" type="password" style="width: 70px; margin-left: 10px; margin-right: 10px" size="10"/>',
            '<button id="login_button" type="button">Login</button>',
            '</form>'
        ].join('');

        setHeaderMessage(form);
        ajax_login();
    }
});

$(document).bind('dhis2.offline', function()
{
    if (dhis2.tc.emptyOrganisationUnits) {
        setHeaderMessage(i18n_no_orgunits);
    }
    else {
        setHeaderMessage(i18n_offline_notification);
    }
});

function ajax_login()
{
    $('#login_button').bind('click', function()
    {
        var username = $('#username').val();
        var password = $('#password').val();

        $.post('../dhis-web-commons-security/login.action', {
            'j_username': username,
            'j_password': password
        }).success(function()
        {
            var ret = dhis2.availability.syncCheckAvailability();

            if (!ret)
            {
                alert(i18n_ajax_login_failed);
            }
        });
    });
}

// -----------------------------------------------------------------------------
// Metadata downloading
// -----------------------------------------------------------------------------

function downloadMetaData()
{
    console.log('Loading required meta-data');
    var def = $.Deferred();
    var promise = def.promise();

    promise = promise.then( dhis2.tc.store.open );
    promise = promise.then( getUserRoles );
    promise = promise.then( getCalendarSetting );
    promise = promise.then( getConstants );
    promise = promise.then( getRelationships );       
    promise = promise.then( getTrackedEntities );
    promise = promise.then( getMetaPrograms );     
    promise = promise.then( getPrograms );     
    promise = promise.then( getProgramStages );    
    promise = promise.then( getOptionSetsForDataElements );
    promise = promise.then( getMetaTrackeEntityAttributes );
    promise = promise.then( getTrackedEntityAttributes );
    promise = promise.then( getOptionSetsForAttributes );
    promise = promise.then( getMetaProgramRuleVariables );
    promise = promise.then( getProgramRuleVariables );
    promise = promise.then( getMetaProgramRules );
    promise = promise.then( getProgramRules );
    promise = promise.then( getMetaProgramValidations );
    promise = promise.then( getProgramValidations );   
    promise = promise.then( getMetaProgramIndicators );
    promise = promise.then( getProgramIndicators );
    promise = promise.then( getOrgUnitLevels );
    promise = promise.then( getTrackedEntityAttributeGroups );
    promise.done(function() {        
        //Enable ou selection after meta-data has downloaded
        $( "#orgUnitTree" ).removeClass( "disable-clicks" );
        dhis2.tc.metaDataCached = true;
        dhis2.availability.startAvailabilityCheck();
        console.log( 'Finished loading meta-data' );        
        selection.responseReceived(); 
    });

    def.resolve();    
}

function getUserRoles()
{
    var SessionStorageService = angular.element('body').injector().get('SessionStorageService');    
    if( SessionStorageService.get('USER_ROLES') ){
       return; 
    }
    
    var def = $.Deferred();
    var promise = def.promise();
    promise = promise.then( getD2Object(null, 'USER_ROLES', '../api/me.json', 'fields=id,name,userCredentials[userRoles[id,authorities]]', 'sessionStorage') );
    promise = promise.done(function(){});    
    def.resolve();
}

function getCalendarSetting()
{   
    var def = $.Deferred();
    var promise = def.promise();
    promise = promise.then( getD2Object(null, 'CALENDAR_SETTING', '../api/systemSettings', 'key=keyCalendar&key=keyDateFormat', 'localStorage') );
    promise = promise.done(function(){});    
    def.resolve();    
}

function getConstants()
{
    dhis2.tc.store.getKeys( 'constants').done(function(res){        
        if(res.length > 0){
            return;
        }        
        return getD2Objects('constants', 'constants', '../api/constants.json', 'paging=false&fields=id,name,displayName,value', 'idb');        
    });    
}

function getRelationships()
{    
    dhis2.tc.store.getKeys( 'relationshipTypes').done(function(res){        
        if(res.length > 0){
            return;
        }
        return getD2Objects('relationshipTypes', 'relationshipTypes', '../api/relationshipTypes.json', 'paging=false&fields=id,name,aIsToB,bIsToA,displayName', 'idb');
    });    
}

function getTrackedEntities()
{
    dhis2.tc.store.getKeys('trackedEntities').done(function(res){
        if(res.length > 0){
            return;
        }        
        return getD2Objects('trackedEntities', 'trackedEntities', '../api/trackedEntities.json', 'paging=false&fields=id,name', 'idb');
    });    
}

function getMetaPrograms()
{
    return getD2Objects('programs', 'programs', '../api/programs.json', 'filter=programType:eq:WITH_REGISTRATION&paging=false&fields=id,version,programTrackedEntityAttributes[trackedEntityAttribute[id,optionSet[id,version]]],programStages[id,name,version,minDaysFromStart,standardInterval,periodType,generatedByEnrollmentDate,excecutionDateLabel,repeatable,autoGenerateEvent,openAfterEnrollment,reportDateToUse,programStageDataElements[dataElement[optionSet[id,version]]]]', 'temp');
}

function getPrograms( programs )
{
    if( !programs ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();

    _.each( _.values( programs ), function ( program ) {
        build = build.then(function() {
            var d = $.Deferred();
            var p = d.promise();
            dhis2.tc.store.get('programs', program.id).done(function(obj) {
                if(!obj || obj.version !== program.version) {
                    promise = promise.then( getProgram( program.id ) );
                }

                d.resolve();
            });

            return p;
        });
    });

    build.done(function() {
        def.resolve();

        promise = promise.done( function () {
            mainDef.resolve( programs );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;
}

function getProgram( id )
{
    return function() {
        return $.ajax( {
            url: '../api/programs/' + id + '.json',
            type: 'GET',
            data: 'fields=id,name,type,version,dataEntryMethod,enrollmentDateLabel,incidentDateLabel,displayIncidentDate,ignoreOverdueEvents,selectEnrollmentDatesInFuture,selectIncidentDatesInFuture,onlyEnrollOnce,externalAccess,displayOnAllOrgunit,registration,relationshipText,relationshipFromA,relatedProgram[id,name],relationshipType[id,name],trackedEntity[id,name,description],userRoles[id,name],organisationUnits[id,name],userRoles[id,name],programStages[id,name,version,minDaysFromStart,standardInterval,periodType,generatedByEnrollmentDate,excecutionDateLabel,repeatable,autoGenerateEvent,openAfterEnrollment,reportDateToUse],dataEntryForm[name,style,htmlCode,format],programTrackedEntityAttributes[displayInList,mandatory,allowFutureDate,trackedEntityAttribute[id,unique]]'
        }).done( function( program ){            
            var ou = {};
            if(program.organisationUnits){
                _.each(_.values( program.organisationUnits), function(o){
                    ou[o.id] = o.name;
                });
            }
            program.organisationUnits = ou;

            var ur = {};
            if(program.userRoles){
                _.each(_.values( program.userRoles), function(u){
                    ur[u.id] = u.name;
                });
            }
            program.userRoles = ur;            
            dhis2.tc.store.set( 'programs', program );  
        });
    };
}

function getProgramStages( programs )
{
    if( !programs ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();

    _.each( _.values( programs ), function ( program ) {
        
        if(program.programStages){
            _.each(_.values(program.programStages), function(programStage){
                build = build.then(function() {
                    var d = $.Deferred();
                    var p = d.promise();
                    dhis2.tc.store.get('programStages', programStage.id).done(function(obj) {
                        if(!obj || obj.version !== programStage.version) {
                            promise = promise.then( getD2Object( programStage.id, 'programStages', '../api/programStages', 'fields=id,name,sortOrder,version,dataEntryForm,captureCoordinates,blockEntryForm,autoGenerateEvent,allowGenerateNextVisit,generatedByEnrollmentDate,remindCompleted,excecutionDateLabel,minDaysFromStart,repeatable,openAfterEnrollment,standardInterval,periodType,reportDateToUse,programStageSections[id,name,programStageDataElements[dataElement[id]]],programStageDataElements[displayInReports,allowProvidedElsewhere,allowFutureDate,compulsory,dataElement[id,code,name,description,formName,valueType,optionSetValue,optionSet[id]]]', 'idb' ) );
                        }
                        d.resolve();
                    });
                    return p;
                });            
            });
        }                             
    });

    build.done(function() {
        def.resolve();

        promise = promise.done( function () {
            mainDef.resolve( programs );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;    
}

function getOptionSetsForDataElements( programs )
{
    if( !programs ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();    

    _.each( _.values( programs ), function ( program ) {
        
        if(program.programStages){
            _.each(_.values( program.programStages), function( programStage) {
                
                if(programStage.programStageDataElements){
                    _.each(_.values( programStage.programStageDataElements), function(prStDe){            
                        if( prStDe.dataElement.optionSet && prStDe.dataElement.optionSet.id ){
                            build = build.then(function() {
                                var d = $.Deferred();
                                var p = d.promise();
                                dhis2.tc.store.get('optionSets', prStDe.dataElement.optionSet.id).done(function(obj) {                                    
                                    if( (!obj || obj.version !== prStDe.dataElement.optionSet.version) && optionSetsInPromise.indexOf(prStDe.dataElement.optionSet.id) === -1) {                                
                                        optionSetsInPromise.push( prStDe.dataElement.optionSet.id );
                                        promise = promise.then( getD2Object( prStDe.dataElement.optionSet.id, 'optionSets', '../api/optionSets', 'fields=id,name,version,options[id,name,code]', 'idb' ) );
                                    }
                                    d.resolve();
                                });

                                return p;
                            });
                        }            
                    });
                }                
            });
        }                                      
    });

    build.done(function() {
        def.resolve();

        promise = promise.done( function () {
            mainDef.resolve( programs );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;    
}

function getMetaTrackeEntityAttributes( programs ){
    
    var def = $.Deferred();
    
    $.ajax({
        url: '../api/trackedEntityAttributes.json',
        type: 'GET',
        data:'paging=false&filter=displayInListNoProgram:eq:true&fields=id,optionSet[id,version]'
    }).done( function(response) {          
        var trackedEntityAttributes = [];
        _.each( _.values( response.trackedEntityAttributes ), function ( trackedEntityAttribute ) {             
            if( trackedEntityAttribute && trackedEntityAttribute.id ) {            
                trackedEntityAttributes.push( trackedEntityAttribute );
            }            
        });
        
        _.each( _.values( programs ), function ( program ) {        
            if(program.programTrackedEntityAttributes){
                _.each(_.values(program.programTrackedEntityAttributes), function(teAttribute){                    
                    trackedEntityAttributes.push(teAttribute.trackedEntityAttribute);
                });
            }
        });
        
        def.resolve( {trackedEntityAttributes: trackedEntityAttributes, programs: programs} );
        
    }).fail(function(){
        def.resolve( null );
    });
    
    return def.promise();
}

function getTrackedEntityAttributes( data )
{
    if( !data.trackedEntityAttributes ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();        

    _.each(_.values(data.trackedEntityAttributes), function(teAttribute){        
        build = build.then(function() {
            var d = $.Deferred();
            var p = d.promise();
            dhis2.tc.store.get('attributes', teAttribute.id).done(function(obj) {
                if((!obj || obj.version !== teAttribute.version) && attributesInPromise.indexOf(teAttribute.id) === -1) {
                    attributesInPromise.push( teAttribute.id );
                    promise = promise.then( getD2Object( teAttribute.id, 'attributes', '../api/trackedEntityAttributes', 'fields=id,name,code,version,description,valueType,optionSetValue,confidential,inherit,sortOrderInVisitSchedule,sortOrderInListNoProgram,displayOnVisitSchedule,displayInListNoProgram,unique,programScope,orgunitScope,confidential,optionSet[id,version],trackedEntity[id,name]', 'idb' ) );
                }
                d.resolve();
            });
            return p;
        });            
    });

    build.done(function() {
        def.resolve();

        promise = promise.done( function () {
            mainDef.resolve( {trackedEntityAttributes: data.trackedEntityAttributes, programs: data.programs} );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;    
}

function getOptionSetsForAttributes( data )
{
    if( !data.trackedEntityAttributes ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();

    _.each(_.values( data.trackedEntityAttributes), function( teAttribute) {           
        if( teAttribute.optionSet && teAttribute.optionSet.id ){
            build = build.then(function() {
                var d = $.Deferred();
                var p = d.promise();
                dhis2.tc.store.get('optionSets', teAttribute.optionSet.id).done(function(obj) {                            
                    if((!obj || obj.version !== teAttribute.optionSet.version) && optionSetsInPromise.indexOf(teAttribute.optionSet.id) === -1) {                                
                        optionSetsInPromise.push(teAttribute.optionSet.id);
                        promise = promise.then( getD2Object( teAttribute.optionSet.id, 'optionSets', '../api/optionSets', 'fields=id,name,version,options[id,name,code]', 'idb' ) );
                    }
                    d.resolve();
                });

                return p;
            });
        }            
    });

    build.done(function() {
        def.resolve();

        promise = promise.done( function () {
            mainDef.resolve( data.programs );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;    
}

function getOrgUnitLevels()
{
    dhis2.tc.store.getKeys( 'ouLevels').done(function(res){        
        if(res.length > 0){
            return;
        }        
        return getD2Objects('ouLevels', 'organisationUnitLevels', '../api/organisationUnitLevels.json', 'filter=level:gt:1&fields=id,name,level&paging=false', 'idb');        
    }); 
}

function getTrackedEntityAttributeGroups()
{
    dhis2.tc.store.getKeys( 'attributeGroups').done(function(res){        
        if(res.length > 0){
            return;
        }        
        return getD2Objects('attributeGroups', 'trackedEntityAttributeGroups', '../api/trackedEntityAttributeGroups.json', 'fields=id,name,trackedEntityAttributes[id]&paging=false', 'idb');        
    }); 
}

function getMetaProgramValidations( programs )
{    
    return getD2MetaObject(programs, 'programValidations', '../api/programValidations.json', 'paging=false&fields=id,program[id]');
}

function getProgramValidations( programValidations )
{
    return checkAndGetD2Objects( programValidations, 'programValidations', '../api/programValidations', 'fields=id,name,displayName,operator,rightSide[expression,description],leftSide[expression,description],program[id]');
}

function getMetaProgramIndicators( programs )
{    
    return getD2MetaObject(programs, 'programIndicators', '../api/programIndicators.json', 'paging=false&fields=id,program[id]');
}

function getProgramIndicators( programIndicators )
{
    return checkAndGetD2Objects( programIndicators, 'programIndicators', '../api/programIndicators', 'fields=id,name,code,shortName,displayInForm,expression,displayDescription,rootDate,description,valueType,DisplayName,filter,program[id]');
}

function getMetaProgramRules( programs )
{    
    return getD2MetaObject(programs, 'programRules', '../api/programRules.json', 'paging=false&fields=id,program[id]');
}

function getProgramRules( programRules )
{
    return checkAndGetD2Objects( programRules, 'programRules', '../api/programRules', 'fields=id,name,condition,description,program[id],programStage[id],priority,programRuleActions[id,content,location,data,programRuleActionType,programStageSection[id],dataElement[id],trackedEntityAttribute[id]]');
}

function getMetaProgramRuleVariables( programs )
{    
    return getD2MetaObject(programs, 'programRuleVariables', '../api/programRuleVariables.json', 'paging=false&fields=id,program[id]');
}

function getProgramRuleVariables( programRuleVariables )
{
    return checkAndGetD2Objects( programRuleVariables, 'programRuleVariables', '../api/programRuleVariables', 'fields=id,name,displayName,programRuleVariableSourceType,program[id],programStage[id],dataElement[id],trackedEntityAttribute[id]');
}

function getD2MetaObject( programs, objNames, url, filter )
{
    if( !programs ){
        return;
    }
    
    var def = $.Deferred();
    
    var programIds = [];
    _.each( _.values( programs ), function ( program ) { 
        if( program.id ) {
            programIds.push( program.id );
        }
    });
    
    $.ajax({
        url: url,
        type: 'GET',
        data:filter
    }).done( function(response) {          
        var objs = [];
        _.each( _.values( response[objNames]), function ( o ) { 
            if( o &&
                o.id &&
                o.program &&
                o.program.id &&
                programIds.indexOf( o.program.id ) !== -1) {
            
                objs.push( o );
            }  
            
        });
        
        def.resolve( {programs: programs, self: objs} );
        
    }).fail(function(){
        def.resolve( null );
    });
    
    return def.promise();    
}

function checkAndGetD2Objects( obj, store, url, filter )
{
    if( !obj || !obj.programs || !obj.self ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();

    _.each( _.values( obj.self ), function ( obj) {
        build = build.then(function() {
            var d = $.Deferred();
            var p = d.promise();
            dhis2.tc.store.get(store, obj.id).done(function(o) {
                if(!o) {
                    promise = promise.then( getD2Object( obj.id, store, url, filter, 'idb' ) );
                }
                d.resolve();
            });

            return p;
        });
    });

    build.done(function() {
        def.resolve();
        promise = promise.done( function () {
            mainDef.resolve( obj.programs );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;
}

function getD2Objects(store, objs, url, filter, storage)
{
    var def = $.Deferred();

    $.ajax({
        url: url,
        type: 'GET',
        data: filter
    }).done(function(response) {
        if(response[objs]){            
            if(storage === 'idb'){
                dhis2.tc.store.setAll( store, response[objs] );
            }
            if(storage === 'localStorage'){                
                localStorage[store] = JSON.stringify(response[objs]);
            }            
            if(storage === 'sessionStorage'){
                var SessionStorageService = angular.element('body').injector().get('SessionStorageService');
                SessionStorageService.set(store, response[objs]);
            }
        }
        
        if(storage === 'temp'){
            def.resolve(response[objs] ? response[objs] : []);
        }
        else{
            def.resolve();
        }    
    }).fail(function(){
        def.resolve();
    });

    return def.promise();
}

function getD2Object( id, store, url, filter, storage )
{
    return function() {
        if(id){
            url = url + '/' + id + '.json';
        }
        return $.ajax( {
            url: url,
            type: 'GET',            
            data: filter
        }).done( function( response ){
            if(storage === 'idb'){
                if( response && response.id) {
                    dhis2.tc.store.set( store, response );
                }
            }
            if(storage === 'localStorage'){
                localStorage[store] = JSON.stringify(response);
            }            
            if(storage === 'sessionStorage'){
                var SessionStorageService = angular.element('body').injector().get('SessionStorageService');
                SessionStorageService.set(store, response);
            }            
        });
    };
}