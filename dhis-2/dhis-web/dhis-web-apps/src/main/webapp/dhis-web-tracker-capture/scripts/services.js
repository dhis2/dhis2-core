/* global angular, moment, dhis2 */

'use strict';

/* Services */

var trackerCaptureServices = angular.module('trackerCaptureServices', ['ngResource'])

.factory('TCStorageService', function(){
    var store = new dhis2.storage.Store({
        name: "dhis2tc",
        adapters: [dhis2.storage.IndexedDBAdapter, dhis2.storage.DomSessionStorageAdapter, dhis2.storage.InMemoryAdapter],        
        objectStores: ['programs', 'programStages', 'trackedEntities', 'attributes', 'relationshipTypes', 'optionSets', 'programValidations', 'programIndicators', 'ouLevels', 'programRuleVariables', 'programRules','constants', 'dataElements']
    });
    return{
        currentStore: store
    };
})

/* Service to fetch/store dasboard widgets */
.service('DashboardLayoutService', function($http) {
    
    var ButtonIds = { Complete: "Complete", Incomplete: "Incomplete", Validate: "Validate", Delete: "Delete", Skip: "Skip", Unskip: "Unskip", Note: "Note" };
      
    var w = {};
    w.enrollmentWidget = {title: 'enrollment', view: "components/enrollment/enrollment.html", show: true, expand: true, parent: 'biggerWidget', order: 0};
    w.indicatorWidget = {title: 'indicators', view: "components/rulebound/rulebound.html", show: true, expand: true, parent: 'biggerWidget', order: 1};
    w.dataentryWidget = {title: 'dataentry', view: "components/dataentry/dataentry.html", show: true, expand: true, parent: 'biggerWidget', order: 2};
    w.dataentryTabularWidget = {title: 'dataentryTabular', view: "components/dataentry/dataentry-tabular-layout.html", show: true, expand: true, parent: 'biggerWidget', order: 3};
    w.reportWidget = {title: 'report', view: "components/report/tei-report.html", show: true, expand: true, parent: 'biggerWidget', order: 4};
    w.selectedWidget = {title: 'current_selections', view: "components/selected/selected.html", show: false, expand: true, parent: 'smallerWidget', order: 0};
    w.feedbackWidget = {title: 'feedback', view: "components/rulebound/rulebound.html", show: true, expand: true, parent: 'smallerWidget', order: 1};
    w.profileWidget = {title: 'profile', view: "components/profile/profile.html", show: true, expand: true, parent: 'smallerWidget', order: 2};    
    w.relationshipWidget = {title: 'relationships', view: "components/relationship/relationship.html", show: true, expand: true, parent: 'smallerWidget', order: 3};
    w.notesWidget = {title: 'notes', view: "components/notes/notes.html", show: true, expand: true, parent: 'smallerWidget', order: 4};
    w.messagingWidget = {title: 'messaging', view: "components/messaging/messaging.html", show: false, expand: true, parent: 'smallerWidget', order: 5};
    var defaultLayout = new Object();
    
    defaultLayout['DEFAULT'] = {widgets: w, program: 'DEFAULT'};
    
    var getDefaultLayout = function(customLayout){
        var dashboardLayout = {customLayout: customLayout, defaultLayout: defaultLayout};        
        var promise = $http.get(  '../api/systemSettings/keyTrackerDashboardDefaultLayout' ).then(function(response){
            angular.extend(dashboardLayout.defaultLayout, response.data);
            return dashboardLayout;
        }, function(){
            return dashboardLayout;
        });
        return promise;        
    };
    
    return {
        saveLayout: function(dashboardLayout, saveAsDefault){
            var url = saveAsDefault ? '../api/systemSettings/keyTrackerDashboardDefaultLayout' : '../api/userSettings/keyTrackerDashboardLayout';
            var promise = $http({
                method: "post",
                url: url,
                data: dashboardLayout,
                headers: {'Content-Type': 'text/plain;charset=utf-8'}
            }).then(function(response){
                return response.data;
            });
            return promise;            
        },
        get: function(){
            var promise = $http.get(  '../api/userSettings/keyTrackerDashboardLayout' ).then(function(response){
                return getDefaultLayout(response.data);
            }, function(){
                return getDefaultLayout(null);
            });
            return promise;
        }
    };
})

/* current selections */
.service('PeriodService', function(DateUtils, CalendarService, $filter){
    
    var calendarSetting = CalendarService.getSetting();    
    
    var splitDate = function(dateValue){
        if(!dateValue){
            return;
        }
        var calendarSetting = CalendarService.getSetting();            

        return {year: moment(dateValue, calendarSetting.momentFormat).year(), month: moment(dateValue, calendarSetting.momentFormat).month(), week: moment(dateValue, calendarSetting.momentFormat).week(), day: moment(dateValue, calendarSetting.momentFormat).day()};
    };
    
    function processPeriodsForEvent(periods,event){
        var index = -1;
        var occupied = null;
        for(var i=0; i<periods.length && index === -1; i++){
            if(moment(periods[i].endDate).isSame(event.sortingDate) ||
                    moment(periods[i].startDate).isSame(event.sortingDate) ||
                    moment(periods[i].endDate).isAfter(event.sortingDate) && moment(event.sortingDate).isAfter(periods[i].endDate)){
                index = i;
                occupied = angular.copy(periods[i]);
            }
        }
        
        if(index !== -1){
            periods.splice(index,1);
        }
        
        return {available: periods, occupied: occupied};
    };
    
    this.getPeriods = function(events, stage, enrollment){
     
        if(!stage){
            return;
        }
        
        var referenceDate = enrollment.incidentDate ? enrollment.incidentDate : enrollment.enrollmentDate;
        var offset = stage.minDaysFromStart;
        
        if(stage.generatedByEnrollmentDate){
            referenceDate = enrollment.enrollmentDate;
        }        
               
        var occupiedPeriods = [];
        var availablePeriods = [];
        if(!stage.periodType){
            angular.forEach(events, function(event){
                occupiedPeriods.push({event: event.event, name: event.sortingDate, stage: stage.id});
            });            
            
        }
        else{

            var startDate = DateUtils.format( moment(referenceDate, calendarSetting.momentFormat).add(offset, 'days') );
            var periodOffset = splitDate(startDate).year - splitDate(DateUtils.getToday()).year;
            var eventDateOffSet = moment(referenceDate, calendarSetting.momentFormat).add('d', offset)._d;
            eventDateOffSet = $filter('date')(eventDateOffSet, calendarSetting.keyDateFormat);        
            
            //generate availablePeriods
            var pt = new PeriodType();
            var d2Periods = pt.get(stage.periodType).generatePeriods({offset: periodOffset, filterFuturePeriods: false, reversePeriods: false});
            angular.forEach(d2Periods, function(p){
                p.endDate = DateUtils.formatFromApiToUser(p.endDate);
                p.startDate = DateUtils.formatFromApiToUser(p.startDate);
                
                if(moment(p.endDate, calendarSetting.momentFormat).isAfter(moment(eventDateOffSet,calendarSetting.momentFormat))){
                    availablePeriods.push( p );
                }
            });                

            //get occupied periods
            angular.forEach(events, function(event){
                var ps = processPeriodsForEvent(availablePeriods, event);
                availablePeriods = ps.available;
                if(ps.occupied){
                    occupiedPeriods.push(ps.occupied);
                }
            });
        }
        return {occupiedPeriods: occupiedPeriods, availablePeriods: availablePeriods};
    };
})

/* Factory to fetch optionSets */
.factory('OptionSetService', function($q, $rootScope, TCStorageService) { 
    return {
        getAll: function(){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('optionSets').done(function(optionSets){
                    $rootScope.$apply(function(){
                        def.resolve(optionSets);
                    });                    
                });
            });            
            
            return def.promise;            
        },
        get: function(uid){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get('optionSets', uid).done(function(optionSet){                    
                    $rootScope.$apply(function(){
                        def.resolve(optionSet);
                    });
                });
            });                        
            return def.promise;            
        },        
        getCode: function(options, key){
            if(options){
                for(var i=0; i<options.length; i++){
                    if( key === options[i].displayName){
                        return options[i].code;
                    }
                }
            }            
            return key;
        },        
        getName: function(options, key){
            if(options){
                for(var i=0; i<options.length; i++){                    
                    if( key === options[i].code){
                        return options[i].displayName;
                    }
                }
            }            
            return key;
        }
    };
})

/* Factory to fetch relationships */
.factory('RelationshipFactory', function($q, $rootScope, TCStorageService) { 
    return {
        getAll: function(){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('relationshipTypes').done(function(relationshipTypes){
                    $rootScope.$apply(function(){
                        def.resolve(relationshipTypes);
                    });                    
                });
            });            
            
            return def.promise;            
        },
        get: function(uid){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get('relationshipTypes', uid).done(function(relationshipType){                    
                    $rootScope.$apply(function(){
                        def.resolve(relationshipType);
                    });
                });
            });                        
            return def.promise;            
        }
    };
})

/* Factory to fetch programs */
.factory('ProgramFactory', function($q, $rootScope, SessionStorageService, TCStorageService, orderByFilter, CommonUtils) { 

    return {        
        
        getAll: function(){
            
            var roles = SessionStorageService.get('USER_ROLES');
            var userRoles = roles && roles.userCredentials && roles.userCredentials.userRoles ? roles.userCredentials.userRoles : [];
            var ou = SessionStorageService.get('SELECTED_OU');
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('programs').done(function(prs){
                    var programs = [];
                    angular.forEach(prs, function(pr){
                        if(pr.organisationUnits.hasOwnProperty( ou.id ) && CommonUtils.userHasValidRole(pr, 'programs', userRoles)){
                            programs.push(pr);
                        }
                    });
                    $rootScope.$apply(function(){
                        def.resolve(programs);
                    });                      
                });
            });
            
            return def.promise;            
        },
        getAllForUser: function(selectedProgram){
            var roles = SessionStorageService.get('USER_ROLES');
            var userRoles = roles && roles.userCredentials && roles.userCredentials.userRoles ? roles.userCredentials.userRoles : [];
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('programs').done(function(prs){
                    var programs = [];
                    angular.forEach(prs, function(pr){                            
                        if(CommonUtils.userHasValidRole(pr, 'programs', userRoles)){
                            programs.push(pr);
                        }
                    });
                    
                    programs = orderByFilter(programs, '-displayName').reverse();
                    
                    if(programs.length === 0){
                        selectedProgram = null;
                    }
                    else if(programs.length === 1){
                        selectedProgram = programs[0];
                    } 
                    else{
                        if(selectedProgram){
                            var continueLoop = true;
                            for(var i=0; i<programs.length && continueLoop; i++){
                                if(programs[i].id === selectedProgram.id){                                
                                    selectedProgram = programs[i];
                                    continueLoop = false;
                                }
                            }
                            if(continueLoop){
                                selectedProgram = null;
                            }
                        }
                    }
                        
                    if(!selectedProgram || angular.isUndefined(selectedProgram) && programs.legth > 0){
                        selectedProgram = programs[0];
                    }
                    
                    $rootScope.$apply(function(){
                        def.resolve({programs: programs, selectedProgram: selectedProgram});
                    });                      
                });
            });
            
            return def.promise;
        },
        get: function(uid){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get('programs', uid).done(function(pr){                    
                    $rootScope.$apply(function(){
                        def.resolve(pr);
                    });
                });
            });                        
            return def.promise;            
        },
        getProgramsByOu: function(ou, selectedProgram){
            var roles = SessionStorageService.get('USER_ROLES');
            var userRoles = roles && roles.userCredentials && roles.userCredentials.userRoles ? roles.userCredentials.userRoles : [];
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('programs').done(function(prs){
                    var programs = [];
                    angular.forEach(prs, function(pr){                            
                        if(pr.organisationUnits.hasOwnProperty( ou.id ) && CommonUtils.userHasValidRole(pr, 'programs', userRoles)){
                            programs.push(pr);
                        }
                    });
                    
                    programs = orderByFilter(programs, '-displayName').reverse();
                    
                    if(programs.length === 0){
                        selectedProgram = null;
                    }
                    else if(programs.length === 1){
                        selectedProgram = programs[0];
                    } 
                    else{
                        if(selectedProgram){
                            var continueLoop = true;
                            for(var i=0; i<programs.length && continueLoop; i++){
                                if(programs[i].id === selectedProgram.id){                                
                                    selectedProgram = programs[i];
                                    continueLoop = false;
                                }
                            }
                            if(continueLoop){
                                selectedProgram = null;
                            }
                        }
                    }
                                        
                    if(!selectedProgram || angular.isUndefined(selectedProgram) && programs.legth > 0){
                        selectedProgram = programs[0];
                    }
                    
                    $rootScope.$apply(function(){
                        def.resolve({programs: programs, selectedProgram: selectedProgram});
                    });                      
                });
            });
            
            return def.promise;
        }
    };
})

/* Factory to fetch programStages */
.factory('ProgramStageFactory', function($q, $rootScope, TCStorageService) {  
    
    return {        
        get: function(uid){            
            var def = $q.defer();
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get('programStages', uid).done(function(pst){                    
                    $rootScope.$apply(function(){
                        def.resolve(pst);
                    });
                });
            });            
            return def.promise;
        },
        getByProgram: function(program){
            var def = $q.defer();
            var stageIds = [];
            var programStages = [];
            angular.forEach(program.programStages, function(stage){
                stageIds.push(stage.id);
            });
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('programStages').done(function(stages){   
                    angular.forEach(stages, function(stage){
                        if(stageIds.indexOf(stage.id) !== -1){                            
                            programStages.push(stage);                               
                        }                        
                    });
                    $rootScope.$apply(function(){
                        def.resolve(programStages);
                    });
                });                
            });            
            return def.promise;
        }
    };    
})

/* Factory to fetch programValidations */
.factory('ProgramValidationFactory', function($q, $rootScope, TCStorageService) {  
    
    return {        
        get: function(uid){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get('programValidations', uid).done(function(pv){                    
                    $rootScope.$apply(function(){
                        def.resolve(pv);
                    });
                });
            });                        
            return def.promise;
        },
        getByProgram: function(program){
            var def = $q.defer();
            var programValidations = [];
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('programValidations').done(function(pvs){   
                    angular.forEach(pvs, function(pv){
                        if(pv.program.id === program){                            
                            programValidations.push(pv);                               
                        }                        
                    });
                    $rootScope.$apply(function(){
                        def.resolve(programValidations);
                    });
                });                
            });            
            return def.promise;
        }
    };        
})

/* Factory for fetching OrgUnit */
.factory('OrgUnitFactory', function($http) {    
    var orgUnit, orgUnitPromise, rootOrgUnitPromise,orgUnitTreePromise;
    return {
        get: function(uid){            
            if( orgUnit !== uid ){
                orgUnitPromise = $http.get( '../api/organisationUnits.json?filter=id:eq:' + uid + '&fields=id,displayName,level,children[id,displayName,level,children[id,displayName,level]]&paging=false' ).then(function(response){
                    orgUnit = response.data.id;
                    return response.data;
                });
            }
            return orgUnitPromise;
        },
        getSearchTreeRoot: function(){
            if(!rootOrgUnitPromise){
                var url = '../api/me.json?fields=teiSearchOrganisationUnits[id,displayName,level,children[id,displayName,level,children[id,displayName,level]]],organisationUnits[id,displayName,level,children[id,displayName,level,children[id,displayName,level]]]&paging=false';                
                rootOrgUnitPromise = $http.get( url ).then(function(response){                    
                    response.data.organisationUnits = response.data.teiSearchOrganisationUnits && response.data.teiSearchOrganisationUnits.length > 0 ? response.data.teiSearchOrganisationUnits : response.data.organisationUnits;
                    delete response.data.teiSearchOrganisationUnits;                    
                    return response.data;
                });
            }
            return rootOrgUnitPromise;
        },
        getOrgUnits: function(uid,fieldUrl){
            var url = '../api/organisationUnits.json?filter=id:eq:'+uid+'&'+fieldUrl+'&paging=false';
            orgUnitTreePromise = $http.get(url).then(function(response){
              return response.data; 
            });               
            return orgUnitTreePromise;
        }
    }; 
})

/* service to deal with TEI registration and update */
.service('RegistrationService', function(TEIService, $q, $translate){
    return {
        registerOrUpdate: function(tei, optionSets, attributesById){
            if(tei){
                var def = $q.defer();
                if(tei.trackedEntityInstance){
                    TEIService.update(tei, optionSets, attributesById).then(function(response){
                        def.resolve(response); 
                    });
                }
                else{
                    TEIService.register(tei, optionSets, attributesById).then(function(response){
                        def.resolve(response); 
                    });
                }
                return def.promise;
            }            
        },
        processForm: function(existingTei, formTei, originalTei, attributesById){
            var tei = angular.copy(existingTei);            
            tei.attributes = [];
            var formEmpty = true;            
            for(var k in attributesById){
                if(formTei[k] !== originalTei[k] && !formTei[k] && !originalTei[k]){
                    formChanged = true;
                }
                if( formTei[k] ){
                    var att = attributesById[k];
                    tei.attributes.push({attribute: att.id, value: formTei[k], displayName: att.displayName, valueType: att.valueType});
                    formEmpty = false;              
                }
                delete tei[k];
            }
            formTei.attributes = tei.attributes;
            
            var formChanged = false;
            for(var k in attributesById){                
                if(formTei[k] !== originalTei[k]){
                    if(!formEmpty){
                        formChanged = true;
                        break;
                    }
                    if(formEmpty && (formTei[k] || originalTei[k]) ){
                        formChanged = true;
                        break;
                    }                    
                }
            }
            
            return {tei: tei, formEmpty: formEmpty, formChanged: formChanged};
        }
    };
})

/* Service to deal with enrollment */
.service('EnrollmentService', function($http, DateUtils, DialogService, $translate) {
    
    var convertFromApiToUser = function(enrollment){
        if(enrollment.enrollments){
            angular.forEach(enrollment.enrollments, function(enrollment){
                enrollment.incidentDate = DateUtils.formatFromApiToUser(enrollment.incidentDate);
                enrollment.enrollmentDate = DateUtils.formatFromApiToUser(enrollment.enrollmentDate);                
            });
        }
        else{
            enrollment.incidentDate = DateUtils.formatFromApiToUser(enrollment.incidentDate);
            enrollment.enrollmentDate = DateUtils.formatFromApiToUser(enrollment.enrollmentDate);
        }
        
        return enrollment;
    };
    var convertFromUserToApi = function(enrollment){
        enrollment.incidentDate = DateUtils.formatFromUserToApi(enrollment.incidentDate);
        enrollment.enrollmentDate = DateUtils.formatFromUserToApi(enrollment.enrollmentDate);
        delete enrollment.orgUnitName;
        return enrollment;
    };
    return {        
        get: function( enrollmentUid ){
            var promise = $http.get(  '../api/enrollments/' + enrollmentUid ).then(function(response){
                return convertFromApiToUser(response.data);
            });
            return promise;
        },
        getByEntity: function( entity ){
            var promise = $http.get(  '../api/enrollments.json?ouMode=ACCESSIBLE&trackedEntityInstance=' + entity + '&paging=false').then(function(response){
                return convertFromApiToUser(response.data);
            });
            return promise;
        },
        getByEntityAndProgram: function( entity, program ){
            var promise = $http.get(  '../api/enrollments.json?ouMode=ACCESSIBLE&trackedEntityInstance=' + entity + '&program=' + program + '&paging=false').then(function(response){
                return convertFromApiToUser(response.data);
            }, function(response){
                if( response && response.data && response.data.status === 'ERROR'){
                    var dialogOptions = {
                        headerText: response.data.status,
                        bodyText: response.data.message ? response.data.message : $translate.instant('unable_to_fetch_data_from_server')
                    };		
                    DialogService.showDialog({}, dialogOptions);
                }                
            });
            return promise;
        },
        getByStartAndEndDate: function( program, orgUnit, ouMode, startDate, endDate ){
            var promise = $http.get(  '../api/enrollments.json?ouMode=ACCESSIBLE&program=' + program + '&orgUnit=' + orgUnit + '&ouMode='+ ouMode + '&startDate=' + startDate + '&endDate=' + endDate + '&paging=false').then(function(response){
                return convertFromApiToUser(response.data);
            });
            return promise;
        },
        enroll: function( enrollment ){
            var en = convertFromUserToApi(angular.copy(enrollment));
            var promise = $http.post(  '../api/enrollments', en ).then(function(response){
                return response.data;
            });
            return promise;
        },
        update: function( enrollment ){
            var en = convertFromUserToApi(angular.copy(enrollment));
            var promise = $http.put( '../api/enrollments/' + en.enrollment , en ).then(function(response){
                return response.data;
            });
            return promise;
        },
        updateForNote: function( enrollment ){
            var promise = $http.post('../api/enrollments/' + enrollment.enrollment + '/note', enrollment).then(function(response){
                return response.data;         
            });
            return promise;
        },
        cancel: function(enrollment){
            var promise = $http.put('../api/enrollments/' + enrollment.enrollment + '/cancelled').then(function(response){
                return response.data;               
            });
            return promise;           
        },
        completeIncomplete: function(enrollment, status){
            var promise = $http.put('../api/enrollments/' + enrollment.enrollment + '/' + status).then(function(response){
                return response.data;               
            });
            return promise; 
        }
    };   
})

/* Service for getting tracked entity */
.factory('TEService', function(TCStorageService, $q, $rootScope) {

    return {        
        getAll: function(){            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('trackedEntities').done(function(entities){
                    $rootScope.$apply(function(){
                        def.resolve(entities);
                    });                    
                });
            });            
            return def.promise;
        },
        get: function(uid){            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get('trackedEntities', uid).done(function(te){                    
                    $rootScope.$apply(function(){
                        def.resolve(te);
                    });
                });
            });                        
            return def.promise;            
        }
    };
})

/* Service for getting tracked entity instances */
.factory('TEIService', function($http, $q, AttributesFactory, DialogService, CommonUtils, CurrentSelection, DateUtils ) {
    
    return {
        get: function(entityUid, optionSets, attributesById){
            var promise = $http.get( '../api/trackedEntityInstances/' +  entityUid + '.json').then(function(response){
                var tei = response.data;
                angular.forEach(tei.attributes, function(att){                    
                    if(attributesById[att.attribute]){
                        att.displayName = attributesById[att.attribute].displayName;                        
                        att.value = CommonUtils.formatDataValue(null, att.value, attributesById[att.attribute], optionSets, 'USER');                
                    }
                });
                return tei;
            }, function(error){
                if(error){
                    var dialogOptions = {
                        headerText: 'error',
                        bodyText: 'access_denied'
                    };
                    if(error.statusText) {
                        dialogOptions.headerText = error.statusText;
                    }
                    if(error.data && error.data.message) {
                        dialogOptions.bodyText = error.data.message;
                    }
                    
                    DialogService.showDialog({}, dialogOptions);
                }
            });
            
            return promise;
        },
        search: function(ouId, ouMode, queryUrl, programUrl, attributeUrl, pager, paging, format, attributesList, attrNamesIdMap, optionSets) {
            var url;
            var deferred = $q.defer();

            if (format === "csv") {
                url = '../api/trackedEntityInstances/query.csv?ou=' + ouId + '&ouMode=' + ouMode;
            } else if (format === "xml") {
                url = '../api/trackedEntityInstances/query.json?ou=' + ouId + '&ouMode=' + ouMode;
            }else {
                url = '../api/trackedEntityInstances/query.json?ou=' + ouId + '&ouMode=' + ouMode;
            }
            
            if(queryUrl){
                url = url + '&'+ queryUrl;
            }
            if(programUrl){
                url = url + '&' + programUrl;
            }
            if(attributeUrl){
                url = url + '&' + attributeUrl;
            }
            if(paging){
                var pgSize = pager ? pager.pageSize : 50;
                var pg = pager ? pager.page : 1;
                pgSize = pgSize > 1 ? pgSize  : 1;
                pg = pg > 1 ? pg : 1;
                url = url + '&pageSize=' + pgSize + '&page=' + pg + '&totalPages=true';
            }
            else{
                url = url + '&paging=false';
            }
            
            $http.get( url ).then(function(response){
                var xmlData, rows, headers, index, itemName, value, jsonData;
                var trackedEntityInstance, attributesById;
                if (format) {
                    attributesById = CurrentSelection.getAttributesById();
                    if (format === "json") {
                        jsonData = {"trackedEntityInstances": []};
                        rows = response.data.rows;
                        headers = response.data.headers;
                        for (var i = 0; i < rows.length; i++) {
                            trackedEntityInstance = null;
                            for (var j = 0; j < rows[i].length; j++) {
                                index = attributesList.indexOf(headers[j].name);
                                itemName = headers[j].column;
                                value = rows[i][j].replace(/&/g, "&amp;");
                                if (attributesById[headers[j].name]) {
                                    value = CommonUtils.formatDataValue(null, value, attributesById[headers[j].name], optionSets, 'USER');
                                } else if ((headers[j].name === "created") || ((headers[j].name === "lastupdated"))) {
                                    value = DateUtils.formatFromApiToUser(value);
                                }

                                if (trackedEntityInstance === null) {
                                    trackedEntityInstance = {};
                                }

                                if (index > -1) {
                                    if (!trackedEntityInstance["attributes"]) {
                                        trackedEntityInstance["attributes"] = [];
                                    }
                                    trackedEntityInstance["attributes"].push({
                                        id: attrNamesIdMap[itemName], name: itemName,
                                        value: value
                                    });
                                } else {
                                    trackedEntityInstance[headers[j].name] = value;
                                }
                            }
                            if (trackedEntityInstance !== null) {
                                jsonData["trackedEntityInstances"].push(trackedEntityInstance);
                            }
                        }
                        if (jsonData) {
                            deferred.resolve(JSON.stringify(jsonData, null, 2));
                        }
                    } else if (format === "xml") {
                        xmlData = "";
                        if (response.data && response.data.rows) {
                            xmlData += "<trackedEntityInstances>";
                            rows = response.data.rows;
                            headers = response.data.headers;
                            for (var i = 0; i < rows.length; i++) {
                                xmlData += "<trackedEntityInstance>";
                                for (var j = 0; j < rows[i].length; j++) {
                                    index = attributesList.indexOf(headers[j].name);
                                    itemName = headers[j].column;
                                    value = rows[i][j].replace(/&/g, "&amp;");
                                    if (attributesById[headers[j].name]) {
                                        value = CommonUtils.formatDataValue(null, value, attributesById[headers[j].name], optionSets, 'USER');
                                    } else if ((headers[j].name === "created") || ((headers[j].name === "lastupdated"))) {
                                        value = DateUtils.formatFromApiToUser(value);
                                    }
                                    if (index > -1) {
                                        xmlData += '<attribute id="' + attrNamesIdMap[itemName] + '" ' +
                                            'name="' + itemName + '" value="' + value + '"></attribute>';
                                    } else {
                                        xmlData += '<' + headers[j].name + ' value="' + value + '"></' + headers[j].name + '>';
                                    }

                                }
                                xmlData += "</trackedEntityInstance>";

                            }
                            xmlData += "</trackedEntityInstances>";
                            deferred.resolve(xmlData);
                        }
                    } else if (format === "csv") {
                        deferred.resolve(response.data);
                    }
                } else {
                    deferred.resolve(response.data);
                }
            }, function(error){
                if(error && error.status === 403){
                    var dialogOptions = {
                        headerText: 'error',
                        bodyText: 'access_denied'
                    };		
                    DialogService.showDialog({}, dialogOptions);
                }
                deferred.resolve(null);
            });            
            return deferred.promise;
        },                
        update: function(tei, optionSets, attributesById){
            var formattedTei = angular.copy(tei);
            angular.forEach(formattedTei.attributes, function(att){
                att.value = CommonUtils.formatDataValue(null, att.value, attributesById[att.attribute], optionSets, 'API');
            });
            var promise = $http.put( '../api/trackedEntityInstances/' + formattedTei.trackedEntityInstance , formattedTei ).then(function(response){                    
                return response.data;
            });
            
            return promise;
        },
        register: function(tei, optionSets, attributesById){
            var formattedTei = angular.copy(tei);
            var attributes = [];
            angular.forEach(formattedTei.attributes, function(att){ 
                attributes.push({attribute: att.attribute, value: CommonUtils.formatDataValue(null, att.value, attributesById[att.attribute], optionSets, 'API')});
            });
            
            formattedTei.attributes = attributes;
            var promise = $http.post( '../api/trackedEntityInstances' , formattedTei ).then(function(response){                    
                return response.data;
            }, function(response) {
                //Necessary now that import errors gives a 409 response from the server.
                //The 409 response is treated as an error response.
                return response.data;
            });                    
            return promise;            
        },
        processAttributes: function(selectedTei, selectedProgram, selectedEnrollment){
            var def = $q.defer();            
            if(selectedTei.attributes){
                if(selectedProgram && selectedEnrollment){
                    //show attribute for selected program and enrollment
                    AttributesFactory.getByProgram(selectedProgram).then(function(atts){
                        selectedTei.attributes = AttributesFactory.showRequiredAttributes(atts,selectedTei.attributes, true);
                        def.resolve(selectedTei);
                    }); 
                }
                if(selectedProgram && !selectedEnrollment){
                    //show attributes for selected program            
                    AttributesFactory.getByProgram(selectedProgram).then(function(atts){    
                        selectedTei.attributes = AttributesFactory.showRequiredAttributes(atts,selectedTei.attributes, false);
                        def.resolve(selectedTei);
                    }); 
                }
                if(!selectedProgram && !selectedEnrollment){
                    //show attributes in no program            
                    AttributesFactory.getWithoutProgram().then(function(atts){                
                        selectedTei.attributes = AttributesFactory.showRequiredAttributes(atts,selectedTei.attributes, false);     
                        def.resolve(selectedTei);
                    });
                }
            }       
            return def.promise;
        }
    };
})

/* Factory for getting tracked entity attributes */
.factory('AttributesFactory', function($q, $rootScope, TCStorageService, orderByFilter, DateUtils, OptionSetService, OperatorFactory) {      

    return {
        getAll: function(){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll('attributes').done(function(attributes){                    
                    $rootScope.$apply(function(){
                        def.resolve(attributes);
                    });
                });
            });            
            return def.promise;            
        }, 
        getByProgram: function(program){
            var def = $q.defer();
            this.getAll().then(function(atts){                
                
                if(program && program.id){
                    var attributes = [];
                    var programAttributes = [];
                    angular.forEach(atts, function(attribute){
                        attributes[attribute.id] = attribute;
                    });

                    angular.forEach(program.programTrackedEntityAttributes, function(pAttribute){
                        var att = attributes[pAttribute.trackedEntityAttribute.id];
                        att.mandatory = pAttribute.mandatory;
                        if(pAttribute.displayInList){
                            att.displayInListNoProgram = true;
                        }
                        programAttributes.push(att);                
                    });
                    
                    def.resolve(programAttributes);
                }                
                else{
                    var attributes = [];
                    angular.forEach(atts, function(attribute){
                        if (attribute.displayInListNoProgram) {
                            attributes.push(attribute);
                        }
                    });     
                    
                    attributes = orderByFilter(attributes, '-sortOrderInListNoProgram').reverse();
                    def.resolve(attributes);
                }                
            });
            return def.promise;    
        },
        getWithoutProgram: function(){   
            
            var def = $q.defer();
            this.getAll().then(function(atts){
                var attributes = [];
                angular.forEach(atts, function(attribute){
                    if (attribute.displayInListNoProgram) {
                        attributes.push(attribute);
                    }
                });     
                def.resolve(attributes);             
            });     
            return def.promise;
        },        
        getMissingAttributesForEnrollment: function(tei, program){
            var def = $q.defer();
            this.getByProgram(program).then(function(atts){
                var programAttributes = atts;
                var existingAttributes = tei.attributes;
                var missingAttributes = [];
                
                for(var i=0; i<programAttributes.length; i++){
                    var exists = false;
                    for(var j=0; j<existingAttributes.length && !exists; j++){
                        if(programAttributes[i].id === existingAttributes[j].attribute){
                            exists = true;
                        }
                    }
                    if(!exists){
                        missingAttributes.push(programAttributes[i]);
                    }
                }
                def.resolve(missingAttributes);
            });            
            return def.promise();            
        },
        showRequiredAttributes: function(requiredAttributes, teiAttributes, fromEnrollment){        
            
            //first reset teiAttributes
            for(var j=0; j<teiAttributes.length; j++){
                teiAttributes[j].show = false;
            }

            //identify which ones to show
            for(var i=0; i<requiredAttributes.length; i++){
                var processed = false;
                for(var j=0; j<teiAttributes.length && !processed; j++){
                    if(requiredAttributes[i].id === teiAttributes[j].attribute){                    
                        processed = true;
                        teiAttributes[j].show = true;
                        teiAttributes[j].order = i;
                        teiAttributes[j].mandatory = requiredAttributes[i].mandatory ? requiredAttributes[i].mandatory : false;
                        teiAttributes[j].allowFutureDate = requiredAttributes[i].allowFutureDate ? requiredAttributes[i].allowFutureDate : false;
                        teiAttributes[j].displayName = requiredAttributes[i].displayName;
                    }
                }

                if(!processed && fromEnrollment){//attribute was empty, so a chance to put some value
                    teiAttributes.push({show: true, order: i, allowFutureDate: requiredAttributes[i].allowFutureDate ? requiredAttributes[i].allowFutureDate : false, mandatory: requiredAttributes[i].mandatory ? requiredAttributes[i].mandatory : false, attribute: requiredAttributes[i].id, displayName: requiredAttributes[i].displayName, type: requiredAttributes[i].valueType, value: ''});
                }                   
            }

            teiAttributes = orderByFilter(teiAttributes, '-order');
            teiAttributes.reverse();
            return teiAttributes;
        },
        generateAttributeFilters: function(attributes){
            angular.forEach(attributes, function(attribute){
                if(attribute.valueType === 'NUMBER' || attribute.valueType === 'DATE'){
                    attribute.operator = OperatorFactory.defaultOperators[0];
                }
            });            
            return attributes;
        }
    };
})

/* factory for handling events */
.factory('DHIS2EventFactory', function($http, DialogService, $translate) {   
    
    var skipPaging = "&skipPaging=true";
    return {     
        
        getEventsByStatus: function(entity, orgUnit, program, programStatus){   
            var promise = $http.get( '../api/events.json?ouMode=ACCESSIBLE&' + 'trackedEntityInstance=' + entity + '&orgUnit=' + orgUnit + '&program=' + program + '&programStatus=' + programStatus  + skipPaging).then(function(response){
                return response.data.events;
            });            
            return promise;
        },
        getEventsByProgram: function(entity, program, attributeCategory){            
            var url = '../api/events.json?ouMode=ACCESSIBLE&' + 'trackedEntityInstance=' + entity + skipPaging;            
            
            if(program){
                url = url + '&program=' + program;
            }
            
            if( attributeCategory && !attributeCategory.default){
                url = url + '&attributeCc=' + attributeCategory.cc + '&attributeCos=' + attributeCategory.cp;
            }
            
            var promise = $http.get( url ).then(function(response){
                return response.data.events;
            });            
            return promise;
        },
        getEventsByProgramStage: function(entity, programStage){
          var url = '../api/events.json?ouMode=ACCESSIBLE&' + 'trackedEntityInstance=' + entity + skipPaging; 
          if(programStage){
              url += '&programStage='+programStage;
          }
          var promise = $http.get(url).then(function(response){
             return response.data.events;
          });
          return promise;
        },
        getByOrgUnitAndProgram: function(orgUnit, ouMode, program, startDate, endDate){
            var url;
            if(startDate && endDate){
                url = '../api/events.json?' + 'orgUnit=' + orgUnit + '&ouMode='+ ouMode + '&program=' + program + '&startDate=' + startDate + '&endDate=' + endDate + skipPaging;
            }
            else{
                url = '../api/events.json?' + 'orgUnit=' + orgUnit + '&ouMode='+ ouMode + '&program=' + program + skipPaging;
            }
            var promise = $http.get( url ).then(function(response){
                return response.data.events;
            }, function(response){
                if( response && response.data && response.data.status === 'ERROR'){
                    var dialogOptions = {
                        headerText: response.data.status,
                        bodyText: response.data.message ? response.data.message : $translate.instant('unable_to_fetch_data_from_server')
                    };		
                    DialogService.showDialog({}, dialogOptions);
                }
            });            
            return promise;
        },
        get: function(eventUid){            
            var promise = $http.get('../api/events/' + eventUid + '.json').then(function(response){               
                return response.data;
            });            
            return promise;
        },        
        create: function(dhis2Event){    
            var promise = $http.post('../api/events.json', dhis2Event).then(function(response){
                return response.data;           
            });
            return promise;            
        },
        delete: function(dhis2Event){
            var promise = $http.delete('../api/events/' + dhis2Event.event).then(function(response){
                return response.data;               
            });
            return promise;           
        },
        update: function(dhis2Event){   
            var promise = $http.put('../api/events/' + dhis2Event.event, dhis2Event).then(function(response){
                return response.data;         
            });
            return promise;
        },        
        updateForSingleValue: function(singleValue){   
            var promise = $http.put('../api/events/' + singleValue.event + '/' + singleValue.dataValues[0].dataElement, singleValue ).then(function(response){
                return response.data;
            });
            return promise;
        },
        updateForNote: function(dhis2Event){   
            var promise = $http.post('../api/events/' + dhis2Event.event + '/note', dhis2Event).then(function(response){
                return response.data;         
            });
            return promise;
        },
        updateForEventDate: function(dhis2Event){
            var promise = $http.put('../api/events/' + dhis2Event.event + '/eventDate', dhis2Event).then(function(response){
                return response.data;         
            });
            return promise;
        }
    };    
})

/* factory for handling event reports */
.factory('EventReportService', function($http, DialogService, $translate) {   
    
    return {        
        getEventReport: function(orgUnit, ouMode, program, startDate, endDate, programStatus, eventStatus, pager){
            
            var url = '../api/events/eventRows.json?' + 'orgUnit=' + orgUnit + '&ouMode='+ ouMode + '&program=' + program;
            
            if( programStatus ){
                url = url + '&programStatus=' + programStatus;
            }
            
            if( eventStatus ){
                url = url + '&eventStatus=' + eventStatus;
            }
            
            if(startDate && endDate){
                url = url + '&startDate=' + startDate + '&endDate=' + endDate ;
            }
            
            if( pager ){
                var pgSize = pager ? pager.pageSize : 50;
                var pg = pager ? pager.page : 1;
                pgSize = pgSize > 1 ? pgSize  : 1;
                pg = pg > 1 ? pg : 1;
                url = url + '&pageSize=' + pgSize + '&page=' + pg + '&totalPages=true';
            } 
            
            var promise = $http.get( url ).then(function(response){
                return response.data;
            }, function(response){
                if( response && response.data && response.data.status === 'ERROR'){
                    var dialogOptions = {
                        headerText: response.data.status,
                        bodyText: response.data.message ? response.data.message : $translate.instant('unable_to_fetch_data_from_server')
                    };		
                    DialogService.showDialog({}, dialogOptions);
                }                
            });            
            return promise;
        }
    };    
})

.factory('OperatorFactory', function($translate){
    
    var defaultOperators = [$translate.instant('IS'), $translate.instant('RANGE') ];
    var boolOperators = [$translate.instant('yes'), $translate.instant('no')];
    return{
        defaultOperators: defaultOperators,
        boolOperators: boolOperators
    };  
})

/* factory to fetch and process programValidations */
.factory('MetaDataFactory', function($q, $rootScope, TCStorageService) {  
    
    return {        
        get: function(store, uid){
            
            var def = $q.defer();
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.get(store, uid).done(function(pv){                    
                    $rootScope.$apply(function(){
                        def.resolve(pv);
                    });
                });
            });                        
            return def.promise;
        },
        getByProgram: function(store, program){
            var def = $q.defer();
            var obj = [];
            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll(store, program).done(function(pvs){   
                    angular.forEach(pvs, function(pv){
                        if(pv.program.id === program){                            
                            obj.push(pv);                               
                        }                        
                    });
                    $rootScope.$apply(function(){
                        def.resolve(obj);
                    });
                });                
            });            
            return def.promise;
        },
        getAll: function(store){
            var def = $q.defer();            
            TCStorageService.currentStore.open().done(function(){
                TCStorageService.currentStore.getAll(store).done(function(pvs){                       
                    $rootScope.$apply(function(){
                        def.resolve(pvs);
                    });
                });                
            });            
            return def.promise;
        }
    };        
})

/* Returns a function for getting rules for a specific program */
.factory('TrackerRulesFactory', function($q,MetaDataFactory,$filter){
    var staticReplacements = 
                        [{regExp:new RegExp("([^\w\d])(and)([^\w\d])","gi"), replacement:"$1&&$3"},
                        {regExp:new RegExp("([^\w\d])(or)([^\w\d])","gi"), replacement:"$1||$3"},
                        {regExp:new RegExp("V{execution_date}","g"), replacement:"V{event_date}"}];
                    
    var performStaticReplacements = function(expression) {
        angular.forEach(staticReplacements, function(staticReplacement) {
            expression = expression.replace(staticReplacement.regExp, staticReplacement.replacement);
        });
        
        return expression;
    };
    
    return{                
        getRules : function(programUid){            
            var def = $q.defer();            
            MetaDataFactory.getAll('constants').then(function(constants) {
                MetaDataFactory.getByProgram('programIndicators',programUid).then(function(pis){                    
                    var variables = [];
                    var programRules = [];
                    angular.forEach(pis, function(pi){
                        if(pi.displayInForm){
                            var newAction = {
                                    id:pi.id,
                                    content:pi.displayDescription ? pi.displayDescription : pi.displayName,
                                    data:pi.expression,
                                    programRuleActionType:'DISPLAYKEYVALUEPAIR',
                                    location:'indicators'
                                };
                            var newRule = {
                                    name:pi.displayName,
                                    id: pi.id,
                                    shortname:pi.shortname,
                                    code:pi.code,
                                    program:pi.program,
                                    description:pi.description,
                                    condition:pi.filter ? pi.filter : 'true',
                                    programRuleActions: [newAction]
                                };

                            programRules.push(newRule);

                            var variablesInCondition = newRule.condition.match(/[A#]{\w+.?\w*}/g);
                            var variablesInData = newAction.data.match(/[A#]{\w+.?\w*}/g);
                            var valueCountPresent = newRule.condition.indexOf("V{value_count}") >= 0 
                                                            || newAction.data.indexOf("V{value_count}") >= 0;
                            var positiveValueCountPresent = newRule.condition.indexOf("V{zero_pos_value_count}") >= 0
                                                            || newAction.data.indexOf("V{zero_pos_value_count}") >= 0;
                            var variableObjectsCurrentExpression = [];
                            
                            var pushDirectAddressedVariable = function(variableWithCurls) {
                                var variableName = $filter('trimvariablequalifiers')(variableWithCurls);
                                var variableNameParts = variableName.split('.');

                                var newVariableObject;

                                if(variableNameParts.length === 2) {
                                    //this is a programstage and dataelement specification. translate to program variable:
                                    newVariableObject = {
                                        displayName:variableName,
                                        programRuleVariableSourceType:'DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE',
                                        dataElement:variableNameParts[1],
                                        programStage:variableNameParts[0],
                                        program:programUid
                                    };
                                }
                                else if(variableNameParts.length === 1)
                                {
                                    //This is an attribute - let us translate to program variable:
                                    newVariableObject = {
                                        displayName:variableName,
                                        programRuleVariableSourceType:'TEI_ATTRIBUTE',
                                        trackedEntityAttribute:variableNameParts[0],
                                        program:programUid
                                    };
                                }
                                variables.push(newVariableObject);
                                
                                return newVariableObject;
                                
                            };
                            
                            angular.forEach(variablesInCondition, function(variableInCondition) {
                                var pushed = pushDirectAddressedVariable(variableInCondition);
                            });

                            angular.forEach(variablesInData, function(variableInData) {
                                var pushed = pushDirectAddressedVariable(variableInData);
                                
                                //We only count the number of values in the data part of the rule
                                //(Called expression in program indicators)
                                variableObjectsCurrentExpression.push(pushed);
                            });
                            
                            //Change expression or data part of the rule to match the program rules execution model
                            
                            if(valueCountPresent) {
                                var valueCountText;
                                angular.forEach(variableObjectsCurrentExpression, function(variableCurrentRule) {
                                   if(valueCountText) {
                                       //This is not the first value in the value count part of the expression. 
                                       valueCountText +=  ' + d2:count(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                   else
                                   {
                                       //This is the first part value in the value count expression:
                                       valueCountText = '(d2:count(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                });
                                //To finish the value count expression we need to close the paranthesis:
                                valueCountText += ')';

                                //Replace all occurrences of value counts in both the data and expression:
                                newRule.condition = newRule.condition.replace(new RegExp("V{value_count}", 'g'),valueCountText);
                                newAction.data = newAction.data.replace(new RegExp("V{value_count}", 'g'),valueCountText);
                            }
                            if(positiveValueCountPresent) {
                                var zeroPosValueCountText;
                                angular.forEach(variableObjectsCurrentExpression, function(variableCurrentRule) {
                                   if(zeroPosValueCountText) {
                                       //This is not the first value in the value count part of the expression. 
                                       zeroPosValueCountText +=  '+ d2:countifzeropos(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                   else
                                   {
                                       //This is the first part value in the value count expression:
                                       zeroPosValueCountText = '(d2:countifzeropos(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                });
                                //To finish the value count expression we need to close the paranthesis:
                                zeroPosValueCountText += ')';

                                //Replace all occurrences of value counts in both the data and expression:
                                newRule.condition = newRule.condition.replace(new RegExp("V{zero_pos_value_count}", 'g'),zeroPosValueCountText);
                                newAction.data = newAction.data.replace(new RegExp("V{zero_pos_value_count}", 'g'),zeroPosValueCountText);
                            }
                            
                            newAction.data = performStaticReplacements(newAction.data);
                            newRule.condition = performStaticReplacements(newRule.condition);
                        }
                    });

                    var programIndicators = {rules:programRules, variables:variables};
                    
                    MetaDataFactory.getByProgram('programValidations',programUid).then(function(programValidations){                    
                        MetaDataFactory.getByProgram('programRuleVariables',programUid).then(function(programVariables){                    
                            MetaDataFactory.getByProgram('programRules',programUid).then(function(prs){
                                var programRules = [];
                                angular.forEach(prs, function(rule){
                                    rule.actions = [];
                                    rule.programStageId = rule.programStage && rule.programStage.id ? rule.programStage.id : null;
                                    programRules.push(rule);
                                });                                
                                def.resolve({constants: constants, programIndicators: programIndicators, programValidations: programValidations, programVariables: programVariables, programRules: programRules});
                            });
                        });
                    });
                }); 
            });                        
            return def.promise;
        }
    };  
})

.service('EntityQueryFactory', function(OperatorFactory, DateUtils){  
    
    this.getAttributesQuery = function(attributes, enrollment){

        var query = {url: null, hasValue: false};
        
        angular.forEach(attributes, function(attribute){           

            if(attribute.valueType === 'DATE' || attribute.valueType === 'NUMBER'){
                var q = '';
                
                if(attribute.operator === OperatorFactory.defaultOperators[0]){
                    if(attribute.exactValue && attribute.exactValue !== ''){
                        query.hasValue = true;
                        if(attribute.valueType === 'DATE'){
                            attribute.exactValue = DateUtils.formatFromUserToApi(attribute.exactValue);
                        }
                        q += 'EQ:' + attribute.exactValue + ':';
                    }
                }                
                if(attribute.operator === OperatorFactory.defaultOperators[1]){
                    if(attribute.startValue && attribute.startValue !== ''){
                        query.hasValue = true;
                        if(attribute.valueType === 'DATE'){
                            attribute.startValue = DateUtils.formatFromUserToApi(attribute.startValue);
                        }
                        q += 'GT:' + attribute.startValue + ':';
                    }
                    if(attribute.endValue && attribute.endValue !== ''){
                        query.hasValue = true;
                        if(attribute.valueType === 'DATE'){
                            attribute.endValue = DateUtils.formatFromUserToApi(attribute.endValue);
                        }
                        q += 'LT:' + attribute.endValue + ':';
                    }
                }                
                if(query.url){
                    if(q){
                        q = q.substr(0,q.length-1);
                        query.url = query.url + '&filter=' + attribute.id + ':' + q;
                    }
                }
                else{
                    if(q){
                        q = q.substr(0,q.length-1);
                        query.url = 'filter=' + attribute.id + ':' + q;
                    }
                }
            }
            else{
                if(attribute.value && attribute.value !== ''){                    
                    query.hasValue = true;                

                    if(angular.isArray(attribute.value)){
                        var q = '';
                        angular.forEach(attribute.value, function(val){                        
                            q += val + ';';
                        });

                        q = q.substr(0,q.length-1);

                        if(query.url){
                            if(q){
                                query.url = query.url + '&filter=' + attribute.id + ':IN:' + q;
                            }
                        }
                        else{
                            if(q){
                                query.url = 'filter=' + attribute.id + ':IN:' + q;
                            }
                        }                    
                    }
                    else{                        
                        if(query.url){
                            query.url = query.url + '&filter=' + attribute.id + ':LIKE:' + attribute.value;
                        }
                        else{
                            query.url = 'filter=' + attribute.id + ':LIKE:' + attribute.value;
                        }
                    }
                }
            }            
        });
        
        if(enrollment){
            var q = '';
            if(enrollment.programEnrollmentStartDate && enrollment.programEnrollmentStartDate !== ''){                
                query.hasValue = true;
                q += '&programEnrollmentStartDate=' + DateUtils.formatFromUserToApi(enrollment.programEnrollmentStartDate);
            }
            if(enrollment.programEnrollmentEndDate && enrollment.programEnrollmentEndDate !== ''){
                query.hasValue = true;
                q += '&programEnrollmentEndDate=' + DateUtils.formatFromUserToApi(enrollment.programEnrollmentEndDate);
            }
            if(enrollment.programIncidentStartDate && enrollment.programIncidentStartDate !== ''){                
                query.hasValue = true;
                q += '&programIncidentStartDate=' + DateUtils.formatFromUserToApi(enrollment.programIncidentStartDate);
            }
            if(enrollment.programIncidentEndDate && enrollment.programIncidentEndDate !== ''){
                query.hasValue = true;
                q += '&programIncidentEndDate=' + DateUtils.formatFromUserToApi(enrollment.programIncidentEndDate);
            }
            if(q){
                if(query.url){
                    query.url = query.url + q;
                }
                else{
                    query.url = q;
                }
            }            
        }
        return query;
        
    };   
    
    this.resetAttributesQuery = function(attributes, enrollment){
        
        angular.forEach(attributes, function(attribute){
            attribute.exactValue = '';
            attribute.startValue = '';
            attribute.endValue = '';
            attribute.value = '';           
        });
        
        if(enrollment){
            enrollment.programStartDate = '';
            enrollment.programEndDate = '';          
        }        
        return attributes;        
    }; 
})

/*Orgunit service for local db */
.service('OuService', function($window, $q){
    
    var indexedDB = $window.indexedDB;
    var db = null;
    
    var open = function(){
        var deferred = $q.defer();
        
        var request = indexedDB.open("dhis2ou");
        
        request.onsuccess = function(e) {
          db = e.target.result;
          deferred.resolve();
        };

        request.onerror = function(){
          deferred.reject();
        };

        return deferred.promise;
    };
    
    var get = function(uid){
        
        var deferred = $q.defer();
        
        if( db === null){
            deferred.reject("DB not opened");
        }
        else{
            var tx = db.transaction(["ou"]);
            var store = tx.objectStore("ou");
            var query = store.get(uid);
                
            query.onsuccess = function(e){
                if(e.target.result){
                    deferred.resolve(e.target.result);
                }
                else{
                    var t = db.transaction(["ouPartial"]);
                    var s = t.objectStore("ouPartial");
                    var q = s.get(uid);
                    q.onsuccess = function(e){
                        deferred.resolve(e.target.result);
                    };
                }            
            };
        }
        return deferred.promise;
    };
    
    return {
        open: open,
        get: get
    };    
})

.service('TEIGridService', function(OptionSetService, CurrentSelection, DateUtils, $translate, $filter, SessionStorageService){
    
    return {
        format: function(grid, map, optionSets, invalidTeis, isFollowUp){
            
            var ou = SessionStorageService.get('SELECTED_OU');
            
            invalidTeis = !invalidTeis ? [] : invalidTeis;
            if(!grid || !grid.rows){
                return;
            }            
            
            //grid.headers[0-6] = Instance, Created, Last updated, OU ID, Ou Name, Tracked entity, Inactive
            //grid.headers[7..] = Attribute, Attribute,.... 
            var attributes = [];
            for(var i=6; i<grid.headers.length; i++){
                attributes.push({id: grid.headers[i].name, displayName: grid.headers[i].column, type: grid.headers[i].type});
            }

            var entityList = {own: [], other: []};
            
            var attributesById = CurrentSelection.getAttributesById();
            
            angular.forEach(grid.rows, function(row){
                if(invalidTeis.indexOf(row[0]) === -1 ){
                    var entity = {};
                    var isEmpty = true;

                    entity.id = row[0];
                    entity.created = DateUtils.formatFromApiToUser( row[1] );

                    entity.orgUnit = row[3];
                    entity.orgUnitName = row[4];
                    entity.type = row[5];
                    entity.inactive = row[6] !== "" ? row[6] : false;
                    entity.followUp = isFollowUp;
                    
                    for(var i=7; i<row.length; i++){
                        if(row[i] && row[i] !== ''){
                            isEmpty = false;
                            var val = row[i];

                            if(attributesById[grid.headers[i].name] && 
                                    attributesById[grid.headers[i].name].optionSetValue && 
                                    optionSets &&    
                                    attributesById[grid.headers[i].name].optionSet &&
                                    optionSets[attributesById[grid.headers[i].name].optionSet.id] ){
                                val = OptionSetService.getName(optionSets[attributesById[grid.headers[i].name].optionSet.id].options, val);
                            }
                            if(attributesById[grid.headers[i].name] && attributesById[grid.headers[i].name].valueType === 'date'){                                    
                                val = DateUtils.formatFromApiToUser( val );
                            }

                            entity[grid.headers[i].name] = val;
                        }
                    }

                    if(!isEmpty){
                        if(map){
                            entityList[entity.id] = entity;
                        }
                        else{
                            if(entity.orgUnit === ou.id){
                                entityList.own.push(entity);
                            }
                            else{
                                entityList.other.push(entity);
                            }
                        }
                    }
                }
            });
            
            var len = entityList.own.length + entityList.other.length;
            return {headers: attributes, rows: entityList, pager: grid.metaData.pager, length: len};
            
        },
        generateGridColumns: function(attributes, ouMode, nonConfidential){
            
            if( ouMode === null ){
                ouMode = 'SELECTED';
            }
            var filterTypes = {}, filterText = {};
            var columns = [];
            
            var returnAttributes = [];
            if(nonConfidential) {
                //Filter out attributes that is confidential, so they will not be part of any grid:
                returnAttributes = angular.copy($filter('nonConfidential')(attributes));
            }
            else
            {
                returnAttributes = angular.copy(attributes);
            }
       
            //also add extra columns which are not part of attributes (orgunit for example)
            columns.push({id: 'orgUnitName', displayName: $translate.instant('registering_unit'), valueType: 'TEXT', displayInListNoProgram: false, attribute: false});
            columns.push({id: 'created', displayName: $translate.instant('registration_date'), valueType: 'DATE', displayInListNoProgram: false, attribute: false});
            columns.push({id: 'inactive', displayName: $translate.instant('inactive'), valueType: 'BOOLEAN', displayInListNoProgram: false, attribute: false});
            columns = columns.concat(returnAttributes ? returnAttributes : []);
            
            //generate grid column for the selected program/attributes
            angular.forEach(columns, function(column){
                column.attribute = angular.isUndefined(column.attribute) ? true : false;
                column.show = false;

                if( (column.id === 'orgUnitName' && ouMode !== 'SELECTED') ||
                    column.displayInListNoProgram || 
                    column.displayInList){
                    column.show = true;
                }                
                column.showFilter = false;                
                filterTypes[column.id] = column.valueType;
                if(column.valueType === 'DATE' || column.valueType === 'NUMBER' ){
                    filterText[column.id]= {};
                }
            });
            return {columns: columns, filterTypes: filterTypes, filterText: filterText};
        },
        getData: function(rows, columns){
            var data = [];
            angular.forEach(rows, function(row){
                var d = {};
                angular.forEach(columns, function(col){
                    if(col.show){
                        d[col.displayName] = row[col.id];
                    }                
                });
                data.push(d);            
            });
            return data;
        },
        getHeader: function(columns){
            var header = []; 
            angular.forEach(columns, function(col){
                if(col.show){
                    header.push($translate.instant(col.displayName));
                }
            });        
            return header;
        }
    };
})

.service('EventUtils', function(DateUtils, CommonUtils, PeriodService, CalendarService, CurrentSelection, $rootScope, $translate, $filter, orderByFilter){
    
    var getEventDueDate = function(eventsByStage, programStage, enrollment){       
        
        var referenceDate = enrollment.incidentDate ? enrollment.incidentDate : enrollment.enrollmentDate,
            offset = programStage.minDaysFromStart,
            calendarSetting = CalendarService.getSetting(),
            dueDate;

        if(programStage.generatedByEnrollmentDate){
            referenceDate = enrollment.enrollmentDate;
        }

        if(programStage.repeatable){
            var evs = [];                
            angular.forEach(eventsByStage, function(ev){
                if(ev.eventDate){
                    evs.push(ev);
                }
            });

            if(evs.length > 0){
                evs = orderByFilter(evs, '-eventDate');                
                if(programStage.periodType){
                    
                }
                else{
                    referenceDate = evs[0].eventDate;
                    offset = programStage.standardInterval;
                }
            }                
        }
        dueDate = moment(referenceDate, calendarSetting.momentFormat).add('d', offset)._d;
        dueDate = $filter('date')(dueDate, calendarSetting.keyDateFormat);        
        return dueDate;
    };
    
    var getEventDuePeriod = function(eventsByStage, programStage, enrollment){ 
        
        var evs = [];                
        angular.forEach(eventsByStage, function(ev){
            if(ev.eventDate){
                evs.push(ev);
            }
        });

        if(evs.length > 0){
            evs = orderByFilter(evs, '-eventDate');
        }
        
        var availabelPeriods = PeriodService.getPeriods(evs,programStage, enrollment).availablePeriods;
        var periods = [];
        for(var k in availabelPeriods){
            if(availabelPeriods.hasOwnProperty(k)){
                periods.push( availabelPeriods[k] );
            }
        }        
        return periods;
    };
    
    var reconstructEvent = function(dhis2Event, programStage, optionSets){
        var e = {dataValues: [], 
                event: dhis2Event.event, 
                program: dhis2Event.program, 
                programStage: dhis2Event.programStage, 
                orgUnit: dhis2Event.orgUnit, 
                trackedEntityInstance: dhis2Event.trackedEntityInstance,
                status: dhis2Event.status,
                dueDate: DateUtils.formatFromUserToApi(dhis2Event.dueDate)
            };

        angular.forEach(programStage.programStageDataElements, function(prStDe){
            if(dhis2Event[prStDe.dataElement.id]){                    
                var value = CommonUtils.formatDataValue(dhis2Event.event, dhis2Event[prStDe.dataElement.id], prStDe.dataElement, optionSets, 'API');                    
                var val = {value: value, dataElement: prStDe.dataElement.id};
                if(dhis2Event.providedElsewhere[prStDe.dataElement.id]){
                    val.providedElsewhere = dhis2Event.providedElsewhere[prStDe.dataElement.id];
                }
                e.dataValues.push(val);
            }                                
        });

        if(programStage.captureCoordinates){
            e.coordinate = {latitude: dhis2Event.coordinate.latitude ? dhis2Event.coordinate.latitude : 0,
                            longitude: dhis2Event.coordinate.longitude ? dhis2Event.coordinate.longitude : 0};
        }

        if(dhis2Event.eventDate){
            e.eventDate = DateUtils.formatFromUserToApi(dhis2Event.eventDate);
        }

        return e;
    };
    
    return {
        createDummyEvent: function(eventsPerStage, tei, program, programStage, orgUnit, enrollment){
            var today = DateUtils.getToday();
            var dummyEvent = {trackedEntityInstance: tei.trackedEntityInstance, 
                              programStage: programStage.id, 
                              program: program.id,
                              orgUnit: orgUnit.id,
                              orgUnitName: orgUnit.displayName,
                              name: programStage.displayName,
                              excecutionDateLabel: programStage.excecutionDateLabel ? programStage.excecutionDateLabel : $translate.instant('report_date'),
                              enrollmentStatus: 'ACTIVE',
                              enrollment: enrollment.enrollment,
                              status: 'SCHEDULED'};
                          
            if(programStage.periodType){                
                var periods = getEventDuePeriod(eventsPerStage, programStage, enrollment);
                dummyEvent.dueDate = periods[0].endDate;
                dummyEvent.periodName = periods[0].displayName;
                dummyEvent.eventDate = dummyEvent.dueDate;
                dummyEvent.periods = periods;
            }
            else{
                dummyEvent.dueDate = getEventDueDate(eventsPerStage, programStage, enrollment);
            }
            
            dummyEvent.sortingDate = dummyEvent.dueDate;
            
            
            if(programStage.captureCoordinates){
                dummyEvent.coordinate = {};
            }
            
            dummyEvent.statusColor = 'alert-warning';//'stage-on-time';
            if(moment(today).isAfter(dummyEvent.dueDate)){
                dummyEvent.statusColor = 'alert-danger';//'stage-overdue';
            }
            return dummyEvent;        
        },
        getEventStatusColor: function(dhis2Event){    
            var eventDate = DateUtils.getToday();
            var calendarSetting = CalendarService.getSetting();
            
            if(dhis2Event.eventDate){
                eventDate = dhis2Event.eventDate;
            }
    
            if(dhis2Event.status === 'COMPLETED'){
                return 'custom-tracker-complete';//'stage-completed';
            }
            else if(dhis2Event.status === 'SKIPPED'){
                return 'alert-default'; //'stage-skipped';
            }
            else{                
                if(dhis2Event.eventDate){
                    return 'alert-warning'; //'stage-executed';
                }
                else{
                    if(moment(eventDate, calendarSetting.momentFormat).isAfter(dhis2Event.dueDate)){
                        return 'alert-danger';//'stage-overdue';
                    }                
                    return 'alert-success';//'stage-on-time';
                }               
            }            
        },
        autoGenerateEvents: function(teiId, program, orgUnit, enrollment, availableEvent){
            var dhis2Events = {events: []};
            if(teiId && program && orgUnit && enrollment){
                angular.forEach(program.programStages, function(stage){
                    if(availableEvent && availableEvent.programStage && availableEvent.programStage === stage.id){
                        var ev = availableEvent;
                        ev.dueDate = ev.dueDate ? ev.dueDate : ev.eventDate;
                        ev.trackedEntityInstance = teiId;
                        ev.enrollment = enrollment.enrollment;
                        delete ev.event;
                        ev = reconstructEvent(ev, stage, CurrentSelection.getOptionSets());
                        dhis2Events.events.push(ev);
                    }
                    
                    if(stage.autoGenerateEvent && (!availableEvent || availableEvent && availableEvent.programStage && availableEvent.programStage !== stage.id)){
                        var newEvent = {
                                trackedEntityInstance: teiId,
                                program: program.id,
                                programStage: stage.id,
                                orgUnit: orgUnit.id,
                                enrollment: enrollment.enrollment
                            };
                        if(stage.periodType){
                            var periods = getEventDuePeriod(null, stage, enrollment);
                            newEvent.dueDate = DateUtils.formatFromUserToApi(periods[0].endDate);
                            newEvent.eventDate = newEvent.dueDate;
                        }
                        else{
                            newEvent.dueDate = DateUtils.formatFromUserToApi(getEventDueDate(null,stage, enrollment));
                        }
                        
                        if(stage.openAfterEnrollment){
                            if(stage.reportDateToUse === 'incidentDate'){
                                newEvent.eventDate = DateUtils.formatFromUserToApi(enrollment.incidentDate);
                            }
                            else{
                                newEvent.eventDate = DateUtils.formatFromUserToApi(enrollment.enrollmentDate);
                            }
                        }

                        newEvent.status = newEvent.eventDate ? 'ACTIVE' : 'SCHEDULE';
                        
                        dhis2Events.events.push(newEvent);
                    }
                });
            }
            
           return dhis2Events;
        },
        reconstruct: function(dhis2Event, programStage, optionSets){            
            return reconstructEvent(dhis2Event, programStage, optionSets);            
        },
        processEvent: function(event, stage, optionSets, prStDes){
            event.providedElsewhere = {};
            angular.forEach(event.dataValues, function(dataValue){
                
                var prStDe = prStDes[dataValue.dataElement];

                if( prStDe ){                
                    var val = dataValue.value;
                    if(prStDe.dataElement){
                        val = CommonUtils.formatDataValue(event.event, val, prStDe.dataElement, optionSets, 'USER');                        
                    }    
                    event[dataValue.dataElement] = val;
                    if(dataValue.providedElsewhere){
                        event.providedElsewhere[dataValue.dataElement] = dataValue.providedElsewhere;
                    }
                }

            });        

            if(stage.captureCoordinates){
                event.coordinate = {latitude: event.coordinate.latitude ? event.coordinate.latitude : '',
                                         longitude: event.coordinate.longitude ? event.coordinate.longitude : ''};
            }        

            event.allowProvidedElsewhereExists = false;        
            for(var i=0; i<stage.programStageDataElements.length; i++){
                if(stage.programStageDataElements[i].allowProvidedElsewhere){
                    event.allowProvidedElsewhereExists = true;
                    break;
                }
            }
            return event;
        },
        getGridColumns: function(stage, prStDes){
            var partial = [], allColumns = [];
            partial.push({id: 'sortingDate', name: $translate.instant('date')});
            partial.push({id: 'orgUnitName', name: $translate.instant('org_unit')});            
            allColumns.push({id: 'sortingDate', name: $translate.instant('date')});
            allColumns.push({id: 'orgUnitName', name: $translate.instant('org_unit')});
            
            var displayInReports = $filter('filter')(stage.programStageDataElements, {displayInReports: true});            
            if( displayInReports.length > 0 ){
                angular.forEach(displayInReports, function(c){
                    if ( prStDes[c.dataElement.id] && prStDes[c.dataElement.id].dataElement) {
                        partial.push({id: c.dataElement.id, name: prStDes[c.dataElement.id].dataElement.displayFormName});
                    }
                });
            }
            for(var i=0; i<stage.programStageDataElements.length; i++){
                if( i < $rootScope.maxGridColumnSize && displayInReports.length === 0){
                    partial.push({id: stage.programStageDataElements[i].dataElement.id, name: prStDes[stage.programStageDataElements[i].dataElement.id].dataElement.displayFormName});
                }                        
                allColumns.push({id: stage.programStageDataElements[i].dataElement.id, name: prStDes[stage.programStageDataElements[i].dataElement.id].dataElement.displayFormName});
            }            
            return {partial: partial, all: allColumns};
        }
    };    
})

.service('EventCreationService', function($modal){
            
        this.showModal = function(eventsByStage, stage, availableStages,programStages,selectedEntity,selectedProgram,selectedOrgUnit,selectedEnrollment, autoCreate, eventCreationAction,allEventsSorted, suggestedStage, selectedCategories){
            var modalInstance = $modal.open({
                templateUrl: 'components/dataentry/new-event.html',
                controller: 'EventCreationController',
                resolve: {                    
                    eventsByStage: function () {
                        return eventsByStage;
                    },
                    stage: function () {
                        return stage;
                    },                
                    stages: function(){
                        return availableStages;
                    },
                    allStages: function(){
                        return programStages;
                    },
                    tei: function(){
                        return selectedEntity;
                    },
                    program: function(){
                        return selectedProgram;
                    },
                    orgUnit: function(){
                        return selectedOrgUnit;
                    },
                    enrollment: function(){
                        return selectedEnrollment;
                    },
                    autoCreate: function () {
                        return autoCreate;
                    },
                    eventCreationAction: function(){
                        return eventCreationAction;
                    },
                    events: function(){
                        return allEventsSorted;
                    },
                    suggestedStage: function(){
                        return suggestedStage;
                    },
                	selectedCategories: function () {
                    	return selectedCategories;
	                }
                }
            }).result;
            return modalInstance;
        };
        this.eventCreationActions = { add: 'ADD',  schedule: 'SCHEDULE', referral: 'REFERRAL'};
})

.service('MessagingService', function($http){
    return {
        sendSmsMessage: function(message){    
            var promise = $http.post('../api/sms/outbound', message).then(function(response){
                return response.data;           
            }, function(response){
                return response.data;
            });
            return promise;            
        }
    };
});

