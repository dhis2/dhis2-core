/* global angular */

'use strict';

/* Controllers */
var eventCaptureControllers = angular.module('eventCaptureControllers', [])

//Controller for settings page
.controller('MainController',
        function($rootScope,
                $scope,
                $modal,
                $translate,
                $anchorScroll,
                $window,
                orderByFilter,
                SessionStorageService,
                Paginator,
                MetaDataFactory,
                ProgramFactory,                               
                DHIS2EventFactory,
                DHIS2EventService,
                ContextMenuSelectedItem,                
                DateUtils,
                CalendarService,
                GridColumnService,
                CustomFormService,
                ECStorageService,
                CurrentSelection,
                ModalService,
                DialogService,
                CommonUtils,
                FileService,
                AuthorityService,
                TrackerRulesExecutionService,
                TrackerRulesFactory) {
    
    $scope.maxOptionSize = 30;
    //selected org unit
    $scope.selectedOrgUnit = '';
    $scope.treeLoaded = false;    
    $scope.selectedSection = {id: 'ALL'};    
    $rootScope.ruleeffects = {};
    $scope.hiddenFields = [];
    $scope.assignedFields = [];
    
    $scope.calendarSetting = CalendarService.getSetting();
    
    //Paging
    $scope.pager = {pageSize: 50, page: 1, toolBarDisplay: 5};   
    
    //Editing
    $scope.eventRegistration = false;
    $scope.editGridColumns = false;
    $scope.editingEventInFull = false;
    $scope.editingEventInGrid = false;   
    $scope.updateSuccess = false;
    $scope.currentGridColumnId = '';  
    $scope.dhis2Events = [];
    $scope.currentEvent = {};
    $scope.currentEventOriginialValue = {}; 
    $scope.displayCustomForm = false;
    $scope.currentElement = {id: '', update: false};
    $scope.optionSets = [];
    $scope.dataElementTranslations = [];
    $scope.proceedSelection = true;
    $scope.formUnsaved = false;
    $scope.fileNames = [];
    $scope.currentFileNames = [];
    
    //notes
    $scope.note = {};
    $scope.today = DateUtils.getToday();    
    
    var userProfile = SessionStorageService.get('USER_PROFILE');
    var storedBy = userProfile && userProfile.username ? userProfile.username : '';
    
    $scope.noteExists = false;
        
    //watch for selection of org unit from tree
    $scope.$watch('selectedOrgUnit', function() {
        
        if(angular.isObject($scope.selectedOrgUnit)){
            
            $scope.pleaseSelectLabel = $translate.instant('please_select');
            $scope.registeringUnitLabel = $translate.instant('registering_unit');
            $scope.eventCaptureLabel = $translate.instant('event_capture');
            $scope.programLabel = $translate.instant('program');
            $scope.searchLabel = $translate.instant('search');
            $scope.findLabel = $translate.instant('find');
            $scope.searchOusLabel = $translate.instant('locate_organisation_unit_by_name');
            $scope.yesLabel = $translate.instant('yes');
            $scope.noLabel = $translate.instant('no');
            
            SessionStorageService.set('SELECTED_OU', $scope.selectedOrgUnit);
            
            $scope.userAuthority = AuthorityService.getUserAuthorities(SessionStorageService.get('USER_ROLES'));
            
            //get ouLevels
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getAll('ouLevels').done(function(response){
                    var ouLevels = angular.isObject(response) ? orderByFilter(response, '-level').reverse() : [];
                    CurrentSelection.setOuLevels(orderByFilter(ouLevels, '-level').reverse());
                });
            });
            
            if($scope.optionSets.length < 1){
                $scope.optionSets = [];
                $scope.dataElementTranslations = [];
                MetaDataFactory.getAll('optionSets').then(function(optionSets){
                    angular.forEach(optionSets, function(optionSet){  
                        $scope.optionSets[optionSet.id] = optionSet;
                    });                    
                    MetaDataFactory.getAll('dataElements').then(function(des){
                        angular.forEach(des, function(de){  
                            $scope.dataElementTranslations[de.id] = de;
                        });
                        $scope.loadPrograms();
                    });
                });
            }
            else{
                $scope.loadPrograms();
            }
        }        
    });
    
    function checkSkipOffline() {
        if(dhis2.ec.isOffline && $scope.selectedProgram && $scope.selectedProgram.skipOffline){
            var dialogOptions = {
                headerText: 'offline',
                bodyText: 'program_is_skip_offline'
            };

            DialogService.showDialog({}, dialogOptions);
            return;
        }
    }
    
    //load programs associated with the selected org unit.
    $scope.loadPrograms = function() {
        
        $scope.resetOu = false;
        $scope.selectedProgramStage = null;
        $scope.currentStage = null;
        $scope.allProgramRules = [];
        $scope.dhis2Events = [];
        $scope.currentEvent = {};
        $scope.currentEventOriginialValue = {};
        $scope.fileNames = [];
        $scope.currentFileNames = [];

        $scope.eventRegistration = false;
        $scope.editGridColumns = false;
        $scope.editingEventInFull = false;
        $scope.editingEventInGrid = false;   
        $scope.updateSuccess = false;
        $scope.currentGridColumnId = '';           
        $scope.displayCustomForm = false;
        
        if (angular.isObject($scope.selectedOrgUnit)) {
            ProgramFactory.getProgramsByOu($scope.selectedOrgUnit, $scope.selectedProgram).then(function(response){
                $scope.programs = response.programs;
                $scope.selectedProgram = response.selectedProgram;
                $scope.getProgramDetails();
            });
        }    
    };
    
    $scope.getProgramDetails = function(){        
        
        $scope.selectedProgramStage = null;
        $scope.eventFetched = false;
        $scope.optionsReady = false;
        
        //Filtering
        $scope.reverse = false;
        $scope.sortHeader = {};
        $scope.filterText = {};
        
        checkSkipOffline();
        
        if( $scope.userAuthority && $scope.userAuthority.canAddOrUpdateEvent &&
                $scope.selectedProgram && 
                $scope.selectedProgram.programStages && 
                $scope.selectedProgram.programStages[0] && 
                $scope.selectedProgram.programStages[0].id){ 
                
            //because this is single event, take the first program stage
            
            $scope.selectedProgramStage = $scope.selectedProgram.programStages[0];   
            $scope.currentStage = $scope.selectedProgramStage;
            
                angular.forEach($scope.selectedProgramStage.programStageSections, function(section){
                    section.open = true;
                });

                $scope.prStDes = [];  
                $scope.eventGridColumns = [];
                $scope.filterTypes = {};                               
                $scope.newDhis2Event = {};

                $scope.eventGridColumns.push({name: 'form_id', id: 'uid', valueType: 'TEXT', compulsory: false, filterWithRange: false, showFilter: false, show: false});
                $scope.filterTypes['uid'] = 'TEXT';                

                $scope.eventGridColumns.push({name: $scope.selectedProgramStage.reportDateDescription ? $scope.selectedProgramStage.reportDateDescription : $translate.instant('incident_date'), id: 'eventDate', valueType: 'DATE', filterWithRange: true, compulsory: false, showFilter: false, show: true});
                $scope.filterTypes['eventDate'] = 'DATE';
                $scope.filterText['eventDate']= {};

                angular.forEach($scope.selectedProgramStage.programStageDataElements, function(prStDe){
                    var tx = $scope.dataElementTranslations[prStDe.dataElement.id];                    
                    prStDe.dataElement.displayFormName = tx.displayFormName && tx.displayFormName !== "" ? tx.displayFormName : tx.displayName;
                    $scope.prStDes[prStDe.dataElement.id] = prStDe;
                    $scope.newDhis2Event[prStDe.dataElement.id] = ''; 
                    
                    //generate grid headers using program stage data elements
                    //create a template for new event
                    //for date type dataelements, filtering is based on start and end dates
                    $scope.eventGridColumns.push({name: prStDe.dataElement.displayFormName,
                                                  id: prStDe.dataElement.id, 
                                                  valueType: prStDe.dataElement.valueType, 
                                                  compulsory: prStDe.compulsory, 
                                                  filterWithRange: prStDe.dataElement.valueType === 'DATE' || 
                                                                        prStDe.dataElement.valueType === 'NUMBER' || 
                                                                        prStDe.dataElement.valueType === 'INTEGER' || 
                                                                        prStDe.dataElement.valueType === 'INTEGER_POSITIVE' || 
                                                                        prStDe.dataElement.valueType === 'INTEGER_NEGATIVE' || 
                                                                        prStDe.dataElement.valueType === 'INTEGER_ZERO_OR_POSITIVE' ? true : false,  
                                                  showFilter: false, 
                                                  show: prStDe.displayInReports});

                    $scope.filterTypes[prStDe.dataElement.id] = prStDe.dataElement.valueType;

                    if(prStDe.dataElement.valueType === 'DATE' ||
                            prStDe.dataElement.valueType === 'NUMBER' ||
                            prStDe.dataElement.valueType === 'INTEGER' ||
                            prStDe.dataElement.valueType === 'INTEGER_POSITIVE' ||
                            prStDe.dataElement.valueType === 'INTEGER_NEGATIVE' ||
                            prStDe.dataElement.valueType === 'INTEGER_ZERO_OR_POSITIVE'){
                        $scope.filterText[prStDe.dataElement.id]= {};
                    }
                });
                
                $scope.customForm = CustomFormService.getForProgramStage($scope.selectedProgramStage, $scope.prStDes);

                if($scope.selectedProgramStage.captureCoordinates){
                    $scope.newDhis2Event.coordinate = {};
                }
                
                $scope.newDhis2Event.eventDate = '';
                
                $scope.selectedCategories = [];
                if($scope.selectedProgram.categoryCombo && 
                        !$scope.selectedProgram.categoryCombo.isDefault &&
                        $scope.selectedProgram.categoryCombo.categories){
                    $scope.selectedCategories = $scope.selectedProgram.categoryCombo.categories;                    
                }
                else{
                    $scope.optionsReady = true;
                }
                
                TrackerRulesFactory.getRules($scope.selectedProgram.id).then(function(rules){                    
                    $scope.allProgramRules = rules;
                    if($scope.selectedCategories.length === 0){
                        $scope.loadEvents();
                    }                        
                });
        }
    };
    
    $scope.getCategoryOptions = function(){
        $scope.eventFetched = false;
        $scope.optionsReady = false;
        $scope.selectedOptions = [];        
        for(var i=0; i<$scope.selectedCategories.length; i++){
            if($scope.selectedCategories[i].selectedOption && $scope.selectedCategories[i].selectedOption.id){
                $scope.optionsReady = true;
                $scope.selectedOptions.push($scope.selectedCategories[i].selectedOption.id);
            }
            else{
                $scope.optionsReady = false;
                break;
            }
        }        
        if($scope.optionsReady){
            $scope.loadEvents();
        }
    };
        
    //get events for the selected program (and org unit)
    $scope.loadEvents = function(){   
        
        checkSkipOffline();
        
        $scope.noteExists = false;            
        $scope.dhis2Events = [];
        $scope.eventLength = 0;
        $scope.eventFetched = false;
        
        var attributeCategoryUrl = {cc: $scope.selectedProgram.categoryCombo.id, default: $scope.selectedProgram.categoryCombo.isDefault, cp: ""};
        if(!$scope.selectedProgram.categoryCombo.isDefault){            
            if($scope.selectedOptions.length !== $scope.selectedCategories.length){
                var dialogOptions = {
                    headerText: 'error',
                    bodyText: 'fill_all_category_options'
                };

                DialogService.showDialog({}, dialogOptions);
                return;
            }            
            attributeCategoryUrl.cp = $scope.selectedOptions.join(';');
        }
               
        if( $scope.selectedProgram && $scope.selectedProgramStage && $scope.selectedProgramStage.id){
            
            //Load events for the selected program stage and orgunit
            DHIS2EventFactory.getByStage($scope.selectedOrgUnit.id, $scope.selectedProgramStage.id, attributeCategoryUrl, $scope.pager, true ).then(function(data){

                if(data.events){
                    $scope.eventLength = data.events.length;
                }                

                //$scope.dhis2Events = data.events; 

                if( data.pager ){
                    $scope.pager = data.pager;
                    $scope.pager.toolBarDisplay = 5;

                    Paginator.setPage($scope.pager.page);
                    Paginator.setPageCount($scope.pager.pageCount);
                    Paginator.setPageSize($scope.pager.pageSize);
                    Paginator.setItemCount($scope.pager.total);                    
                }

                //process event list for easier tabular sorting
                if( angular.isObject( data.events ) ) {

                    angular.forEach(data.events,function(event){

                        if(event.notes && !$scope.noteExists){
                            $scope.noteExists = true;
                        }                  

                        angular.forEach(event.dataValues, function(dataValue){

                            //converting event.datavalues[i].datavalue.dataelement = value to
                            //event[dataElement] = value for easier grid display.                                
                            if($scope.prStDes[dataValue.dataElement]){
                                var val = dataValue.value;                                  
                                if(angular.isObject($scope.prStDes[dataValue.dataElement].dataElement)){
                                    val = CommonUtils.formatDataValue(null, val, $scope.prStDes[dataValue.dataElement].dataElement, $scope.optionSets, 'USER');                                                                          
                                }

                                event[dataValue.dataElement] = val;

                                if($scope.prStDes[dataValue.dataElement].dataElement.valueType === 'FILE_RESOURCE'){
                                    FileService.get(val).then(function(response){
                                        if(response && response.name){
                                            if(!$scope.fileNames[event.event]){
                                                $scope.fileNames[event.event] = [];
                                            } 
                                            $scope.fileNames[event.event][dataValue.dataElement] = response.name;
                                        }
                                    });
                                }
                            }
                        });

                        event['uid'] = event.event;                                
                        event.eventDate = DateUtils.formatFromApiToUser(event.eventDate);                                
                        event['eventDate'] = event.eventDate;
                        if(event.status === "ACTIVE") {
                            event.status = false;
                        } else if(event.status === "COMPLETED") {
                            event.status = true;
                        }

                        delete event.dataValues;
                        
                    });

                    $scope.dhis2Events = data.events; 
                    
                    if($scope.noteExists && !GridColumnService.columnExists($scope.eventGridColumns, 'comment')){
                        $scope.eventGridColumns.push({name: 'comment', id: 'comment', type: 'TEXT', filterWithRange: false, compulsory: false, showFilter: false, show: true});
                    }
                    
                    if(!$scope.sortHeader.id){
                        $scope.sortEventGrid({name: $scope.selectedProgramStage.reportDateDescription ? $scope.selectedProgramStage.reportDateDescription : 'incident_date', id: 'eventDate', type: 'DATE', compulsory: false, showFilter: false, show: true});
                    }
                }
                
                $scope.eventFetched = true;
            });
        }
    };    
    
    $scope.jumpToPage = function(){
        
        if($scope.pager && $scope.pager.page && $scope.pager.pageCount && $scope.pager.page > $scope.pager.pageCount){
            $scope.pager.page = $scope.pager.pageCount;
        }
        $scope.loadEvents();
    };
    
    $scope.resetPageSize = function(){
        $scope.pager.page = 1;        
        $scope.loadEvents();
    };
    
    $scope.getPage = function(page){    
        $scope.pager.page = page;
        $scope.loadEvents();
    };
    
    $scope.sortEventGrid = function(gridHeader){        
        if ($scope.sortHeader && $scope.sortHeader.id === gridHeader.id){
            $scope.reverse = !$scope.reverse;
            return;
        }        
        $scope.sortHeader = gridHeader;
        if($scope.sortHeader.type === 'DATE'){
            $scope.reverse = true;
        }
        else{
            $scope.reverse = false;    
        }        
    };
    
    $scope.d2Sort = function(dhis2Event){        
        if($scope.sortHeader && $scope.sortHeader.type === 'DATE'){            
            var d = dhis2Event[$scope.sortHeader.id];         
            return DateUtils.getDate(d);
        }
        return dhis2Event[$scope.sortHeader.id];
    };
    
    $scope.showHideColumns = function(){
        
        $scope.hiddenGridColumns = 0;
        
        angular.forEach($scope.eventGridColumns, function(eventGridColumn){
            if(!eventGridColumn.show){
                $scope.hiddenGridColumns++;
            }
        });
        
        var modalInstance = $modal.open({
            templateUrl: 'views/column-modal.html',
            controller: 'ColumnDisplayController',
            resolve: {
                gridColumns: function () {
                    return $scope.eventGridColumns;
                },
                hiddenGridColumns: function(){
                    return $scope.hiddenGridColumns;
                }
            }
        });

        modalInstance.result.then(function (gridColumns) {
            $scope.eventGridColumns = gridColumns;
        }, function () {
        });
    };
    
    $scope.searchInGrid = function(gridColumn){
        
        $scope.currentFilter = gridColumn;
       
        for(var i=0; i<$scope.eventGridColumns.length; i++){
            
            //toggle the selected grid column's filter
            if($scope.eventGridColumns[i].id === gridColumn.id){
                $scope.eventGridColumns[i].showFilter = !$scope.eventGridColumns[i].showFilter;
            }            
            else{
                $scope.eventGridColumns[i].showFilter = false;
            }
        }
    };    
    
    $scope.removeStartFilterText = function(gridColumnId){
        $scope.filterText[gridColumnId].start = undefined;
    };
    
    $scope.removeEndFilterText = function(gridColumnId){
        $scope.filterText[gridColumnId].end = undefined;
    };
    
    $scope.cancel = function(){
        
        if($scope.formIsChanged()){
            var modalOptions = {
                closeButtonText: 'no',
                actionButtonText: 'yes',
                headerText: 'warning',
                bodyText: 'unsaved_data_exists_proceed'
            };

            ModalService.showModal({}, modalOptions).then(function(result){
                for(var i=0; i<$scope.dhis2Events.length; i++){
                    if($scope.dhis2Events[i].event === $scope.currentEvent.event){
                        $scope.dhis2Events[i] = $scope.currentEventOriginialValue;                        
                        break;
                    }
                }                
                $scope.showEventList();
            });
        }
        else{
            $scope.showEventList();
        }
    };
    
    $scope.showEventList = function(dhis2Event){        
        ContextMenuSelectedItem.setSelectedItem(dhis2Event);
        $scope.eventRegistration = false;
        $scope.editingEventInFull = false;
        $scope.editingEventInGrid = false;
        $scope.currentElement.updated = false;        
        $scope.currentEvent = {};
        $scope.fileNames['SINGLE_EVENT'] = [];
        $scope.currentElement = {};
        $scope.currentEventOriginialValue = angular.copy($scope.currentEvent);
    };
    
    $scope.showEventRegistration = function(){        
        $scope.displayCustomForm = $scope.customForm ? true : false;
        $scope.currentEvent = {};
        $scope.fileNames['SINGLE_EVENT'] = [];
        $scope.eventRegistration = !$scope.eventRegistration;          
        $scope.currentEvent = angular.copy($scope.newDhis2Event);        
        $scope.outerForm.submitted = false;
        $scope.note = {};
        
        if($scope.selectedProgramStage.preGenerateUID){
            $scope.eventUID = dhis2.util.uid();
            $scope.currentEvent['uid'] = $scope.eventUID;
        }        
        $scope.currentEventOriginialValue = angular.copy($scope.currentEvent); 
        
        if($scope.eventRegistration){
            $scope.executeRules();
        }
    };    
    
    $scope.showEditEventInGrid = function(){
        $scope.currentEvent = ContextMenuSelectedItem.getSelectedItem();
        $scope.currentEventOriginialValue = angular.copy($scope.currentEvent);
        $scope.editingEventInGrid = !$scope.editingEventInGrid;
        
        $scope.outerForm.$valid = true;
    };
    
    $scope.showEditEventInFull = function(){       
        $scope.note = {};
        $scope.displayCustomForm = $scope.customForm ? true:false;

        $scope.currentEvent = ContextMenuSelectedItem.getSelectedItem();
        $scope.editingEventInFull = !$scope.editingEventInFull;   
        $scope.eventRegistration = false;
        
        angular.forEach($scope.selectedProgramStage.programStageDataElements, function(prStDe){
            if(!$scope.currentEvent.hasOwnProperty(prStDe.dataElement.id)){
                $scope.currentEvent[prStDe.dataElement.id] = '';
            }
        }); 
        $scope.currentEventOriginialValue = angular.copy($scope.currentEvent);
        
        if($scope.editingEventInFull){
            //Blank out rule effects, as there is no rules in effect before the first
            //time the rules is run on a new page.
            $rootScope.ruleeffects[$scope.currentEvent.event] = {};        
            $scope.executeRules();
        }
    };
    
    $scope.switchDataEntryForm = function(){
        $scope.displayCustomForm = !$scope.displayCustomForm;
    };
    
    $scope.addEvent = function(addingAnotherEvent){                
        
        checkSkipOffline();
        
        //check for form validity
        $scope.outerForm.submitted = true;        
        if( $scope.outerForm.$invalid ){
            $scope.selectedSection.id = 'ALL';
            angular.forEach($scope.selectedProgramStage.programStageSections, function(section){
                section.open = true;
            });
            return;
        }
        
        //the form is valid, get the values
        //but there could be a case where all dataelements are non-mandatory and
        //the event form comes empty, in this case enforce at least one value
        var valueExists = false;
        var dataValues = [];        
        for(var dataElement in $scope.prStDes){            
            var val = $scope.currentEvent[dataElement];
            if(val){
                valueExists = true;                
                val = CommonUtils.formatDataValue(null, val, $scope.prStDes[dataElement].dataElement, $scope.optionSets, 'API');
            }
            dataValues.push({dataElement: dataElement, value: val});
        }
        
        if(!valueExists){
            var dialogOptions = {
                headerText: 'empty_form',
                bodyText: 'please_fill_at_least_one_dataelement'
            };

            DialogService.showDialog({}, dialogOptions);
            return;
        }        
        
        if(addingAnotherEvent){
            $scope.disableSaveAndAddNew = true;
        }
        
        var newEvent = angular.copy($scope.currentEvent);        
        
        //prepare the event to be created
        var dhis2Event = {
                program: $scope.selectedProgram.id,
                programStage: $scope.selectedProgramStage.id,
                orgUnit: $scope.selectedOrgUnit.id,
                status: $scope.currentEvent.status ? 'COMPLETED' : 'ACTIVE',
                eventDate: DateUtils.formatFromUserToApi(newEvent.eventDate),
                dataValues: dataValues
        }; 
        
        if($scope.selectedProgramStage.preGenerateUID && !angular.isUndefined(newEvent['uid'])){
            dhis2Event.event = newEvent['uid'];
        }
        
        if(!angular.isUndefined($scope.note.value) && $scope.note.value !== ''){
            dhis2Event.notes = [{value: $scope.note.value}];
            
            newEvent.notes = [{value: $scope.note.value, storedDate: $scope.today, storedBy: storedBy}];
            
            $scope.noteExists = true;
        }
        
        if($scope.selectedProgramStage.captureCoordinates){
            dhis2Event.coordinate = {latitude: $scope.currentEvent.coordinate.latitude ? $scope.currentEvent.coordinate.latitude : '',
                                     longitude: $scope.currentEvent.coordinate.longitude ? $scope.currentEvent.coordinate.longitude : ''};             
        }
        
        if(!$scope.selectedProgram.categoryCombo.isDefault){            
            if($scope.selectedOptions.length !== $scope.selectedCategories.length){
                var dialogOptions = {
                    headerText: 'error',
                    bodyText: 'fill_all_category_options'
                };

                DialogService.showDialog({}, dialogOptions);
                return;
            }
            
            //dhis2Event.attributeCc = $scope.selectedProgram.categoryCombo.id;
            dhis2Event.attributeCategoryOptions = $scope.selectedOptions.join(';');
        }
        
        //send the new event to server        
        DHIS2EventFactory.create(dhis2Event).then(function(data) {
            if (data.response.importSummaries[0].status === 'ERROR') {
                var dialogOptions = {
                    headerText: 'event_registration_error',
                    bodyText: data.message
                };

                DialogService.showDialog({}, dialogOptions);
            }
            else {
                
                //add the new event to the grid                
                newEvent.event = data.response.importSummaries[0].reference; 
                $scope.currentEvent.event = newEvent.event;
                
                $scope.updateFileNames();
                
                if( !$scope.dhis2Events ){
                    $scope.dhis2Events = [];                   
                }
                newEvent['uid'] = newEvent.event;
                newEvent['eventDate'] = newEvent.eventDate; 
                $scope.dhis2Events.splice(0,0,newEvent);
                
                $scope.eventLength++;
                
                $scope.eventRegistration = false;
                $scope.editingEventInFull = false;
                $scope.editingEventInGrid = false;  
                    
                //reset form              
                $scope.currentEvent = {};
                $scope.currentEvent = angular.copy($scope.newDhis2Event); 
                $scope.currentEventOriginialValue = angular.copy($scope.currentEvent);
                $scope.fileNames['SINGLE_EVENT'] = [];
                               
                $scope.note = {};
                $scope.outerForm.submitted = false;
                $scope.outerForm.$setPristine();
                $scope.disableSaveAndAddNew = false;
                
                //decide whether to stay in the current screen or not.
                if(addingAnotherEvent){
                    $scope.showEventRegistration();
                    $anchorScroll();
                }
            }
        });
    }; 
    
    $scope.updateEvent = function(){
        
        checkSkipOffline();
        
        //check for form validity
        $scope.outerForm.submitted = true;        
        if( $scope.outerForm.$invalid ){
            $scope.selectedSection.id = 'ALL';
            angular.forEach($scope.selectedProgramStage.programStageSections, function(section){
                section.open = true;
            });
            return;
        }
        
        //the form is valid, get the values
        var dataValues = [];        
        for(var dataElement in $scope.prStDes){
            var val = $scope.currentEvent[dataElement];            
            val = CommonUtils.formatDataValue(null, val, $scope.prStDes[dataElement].dataElement, $scope.optionSets, 'API');            
            dataValues.push({dataElement: dataElement, value: val});
        }
        
        var updatedEvent = {
                            program: $scope.currentEvent.program,
                            programStage: $scope.currentEvent.programStage,
                            orgUnit: $scope.currentEvent.orgUnit,
                            status: $scope.currentEvent.status ? 'COMPLETED' : 'ACTIVE',
                            eventDate: DateUtils.formatFromUserToApi($scope.currentEvent.eventDate),
                            event: $scope.currentEvent.event, 
                            dataValues: dataValues
                        };

        if($scope.selectedProgramStage.captureCoordinates){
            updatedEvent.coordinate = {latitude: $scope.currentEvent.coordinate.latitude ? $scope.currentEvent.coordinate.latitude : '',
                                     longitude: $scope.currentEvent.coordinate.longitude ? $scope.currentEvent.coordinate.longitude : ''};             
        }
        
        if(!angular.isUndefined($scope.note.value) && $scope.note.value !== ''){
           
            updatedEvent.notes = [{value: $scope.note.value}];
            
            if($scope.currentEvent.notes){
                $scope.currentEvent.notes.splice(0,0,{value: $scope.note.value, storedDate: $scope.today, storedBy: storedBy});
            }
            else{
                $scope.currentEvent.notes = [{value: $scope.note.value, storedDate: $scope.today, storedBy: storedBy}];
            }   
            
            $scope.noteExists = true;
        }

        DHIS2EventFactory.update(updatedEvent).then(function(data){            
            //reflect the change in the gird            
            $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);    
            $scope.updateFileNames();
            $scope.outerForm.submitted = false;            
            $scope.editingEventInFull = false;
            $scope.currentEvent = {};
            $scope.currentEventOriginialValue = angular.copy($scope.currentEvent); 
        });       
    };
    
    $scope.updateEventDate = function () {
        
        checkSkipOffline();
        
        $scope.updateSuccess = false;
        
        $scope.currentElement = {id: 'eventDate'};
        
        var rawDate = angular.copy($scope.currentEvent.eventDate);
        var convertedDate = DateUtils.format($scope.currentEvent.eventDate);

        if (!rawDate || !convertedDate || rawDate !== convertedDate) {
            $scope.invalidDate = true;
            $scope.currentEvent.eventDate = $scope.currentEventOriginialValue.eventDate;            
            $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);
            $scope.currentElement.updated = false;
            return;
        }

        //get new and old values
        var newValue = $scope.currentEvent.eventDate;   
        var oldValue = $scope.currentEventOriginialValue.eventDate;
        
        if ($scope.currentEvent.eventDate === '') {
            $scope.currentEvent.eventDate = oldValue;            
            $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);
            $scope.currentElement.updated = false;
            return;
        }
        
        if(newValue !== oldValue){
            var e = {event: $scope.currentEvent.event,
                        orgUnit: $scope.currentEvent.orgUnit,     
                        eventDate: DateUtils.formatFromUserToApi($scope.currentEvent.eventDate)
                    };
            
            var updatedFullValueEvent = DHIS2EventService.reconstructEvent($scope.currentEvent, $scope.selectedProgramStage.programStageDataElements);

            DHIS2EventFactory.updateForEventDate(e, updatedFullValueEvent).then(function () {
                //reflect the new value in the grid
                $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);
                
                //update original value
                $scope.currentEventOriginialValue = angular.copy($scope.currentEvent);      
                
                $scope.currentElement.updated = true;
                $scope.updateSuccess = true;
            });
        }        
    };
            
    $scope.updateEventDataValue = function(currentEvent, dataElement){
        
        checkSkipOffline();
        
        $scope.updateSuccess = false;
        
        //get current element
        $scope.currentElement = {id: dataElement, pending: true, updated: false, failed: false};
        
        //get new and old values
        var newValue = $scope.currentEvent[dataElement];        
        var oldValue = $scope.currentEventOriginialValue[dataElement];
        
        //check for form validity
        if( $scope.isFormInvalid() ){
            $scope.currentElement.updated = false;
            
            //reset value back to original
            $scope.currentEvent[dataElement] = oldValue;            
            $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);
            return;            
        }
        
        if( $scope.prStDes[dataElement].compulsory && !newValue ) {
            $scope.currentElement.updated = false;                        
            
            //reset value back to original
            $scope.currentEvent[dataElement] = oldValue;            
            $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);
            return;
        }        
                
        if( newValue !== oldValue ){            
            newValue = CommonUtils.formatDataValue(null, newValue, $scope.prStDes[dataElement].dataElement, $scope.optionSets, 'API');            
            var updatedSingleValueEvent = {event: $scope.currentEvent.event, dataValues: [{value: newValue, dataElement: dataElement}]};
            var updatedFullValueEvent = DHIS2EventService.reconstructEvent($scope.currentEvent, $scope.selectedProgramStage.programStageDataElements);

            DHIS2EventFactory.updateForSingleValue(updatedSingleValueEvent, updatedFullValueEvent).then(function(data){
                
                //reflect the new value in the grid
                $scope.dhis2Events = DHIS2EventService.refreshList($scope.dhis2Events, $scope.currentEvent);
                
                //update original value
                $scope.currentEventOriginialValue = angular.copy($scope.currentEvent);      
                
                $scope.currentElement.pending = false;
                $scope.currentElement.updated = true;
                $scope.updateSuccess = true;
            }, function(){
                $scope.currentElement.pending = false;
                $scope.currentElement.updated = false;
                $scope.currentElement.failed = true;
            });
        }
    };
    
    $scope.removeEvent = function(){
        
        var dhis2Event = ContextMenuSelectedItem.getSelectedItem();
        
        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'remove',
            headerText: 'remove',
            bodyText: 'are_you_sure_to_remove_with_audit'
        };

        ModalService.showModal({}, modalOptions).then(function(result){
            
            DHIS2EventFactory.delete(dhis2Event).then(function(data){
                
                $scope.currentFileNames = [];
                delete $scope.fileNames[$scope.currentEvent.event];
                var continueLoop = true, index = -1;
                for(var i=0; i< $scope.dhis2Events.length && continueLoop; i++){
                    if($scope.dhis2Events[i].event === dhis2Event.event ){
                        $scope.dhis2Events[i] = dhis2Event;
                        continueLoop = false;
                        index = i;
                    }
                }
                $scope.dhis2Events.splice(index,1);                
                $scope.currentEvent = {}; 
                $scope.fileNames['SINGLE_EVENT'] = [];
            });
        });        
    };
        
    $scope.showNotes = function(dhis2Event){
        
        var modalInstance = $modal.open({
            templateUrl: 'views/notes.html',
            controller: 'NotesController',
            resolve: {
                dhis2Event: function () {
                    return dhis2Event;
                }
            }
        });

        modalInstance.result.then(function (){
        });
    };
    
    $scope.getHelpContent = function(){
    };
    
    $scope.showMap = function(event){
        var modalInstance = $modal.open({
            templateUrl: '../dhis-web-commons/angular-forms/map.html',
            controller: 'MapController',
            windowClass: 'modal-full-window',
            resolve: {
                location: function () {
                    return {lat: event.coordinate.latitude, lng: event.coordinate.longitude};
                }
            }
        });

        modalInstance.result.then(function (location) {
            if(angular.isObject(location)){
                event.coordinate.latitude = location.lat;
                event.coordinate.longitude = location.lng;
            }
        }, function () {
        });
    };
    
    $scope.formIsChanged = function(){        
        var isChanged = false;
        var emptyForm = $scope.formIsEmpty();
        for(var i=0; i<$scope.selectedProgramStage.programStageDataElements.length && !isChanged; i++){
            var deId = $scope.selectedProgramStage.programStageDataElements[i].dataElement.id;
            if($scope.currentEventOriginialValue[deId] !== $scope.currentEvent[deId]){
                if($scope.currentEvent[deId] || $scope.currentEventOriginialValue[deId] !== "" && !emptyForm){                    
                    isChanged = true; 
                }                               
            }
        }        
        if(!isChanged){
            if($scope.currentEvent.eventDate !== $scope.currentEventOriginialValue.eventDate){
                isChanged = true;
            }
        }
        
        return isChanged;
    };
    
    $scope.isFormInvalid = function(){
        
        if($scope.outerForm.submitted){
            return $scope.outerForm.$invalid;
        }
        
        if(!$scope.outerForm.$dirty){
            return false;
        }
        
        var formIsInvalid = false;
        for(var k in $scope.outerForm.$error){            
            if(angular.isObject($scope.outerForm.$error[k])){
                
                for(var i=0; i<$scope.outerForm.$error[k].length && !formIsInvalid; i++){
                    if($scope.outerForm.$error[k][i].$dirty && $scope.outerForm.$error[k][i].$invalid){
                        formIsInvalid = true;
                    }
                }
            }
            
            if(formIsInvalid){
                break;
            }
        }
        
        return formIsInvalid;
    };
    
    $scope.formIsEmpty = function(){
        for(var dataElement in $scope.prStDes){
            if($scope.currentEvent[dataElement]){
                return false;
            }
        }
        return true;
    };
    
    //watch for event editing
    $scope.$watchCollection('[editingEventInFull, eventRegistration]', function() {        
        if($scope.editingEventInFull || $scope.eventRegistration){
            //Disable ou selection while in editing mode
            $( "#orgUnitTree" ).addClass( "disable-clicks" );
        }
        else{
            //enable ou selection if not in editing mode
            $( "#orgUnitTree" ).removeClass( "disable-clicks" );
        }
    });
    
    $scope.interacted = function(field) {
        var status = false;
        if(field){            
            status = $scope.outerForm.submitted || field.$dirty;
        }
        return status;        
    };

    //listen for rule effect changes    
    $scope.$on('ruleeffectsupdated', function(event, args) {
        $scope.warningMessages = [];
        $scope.hiddenSections = [];
        $scope.hiddenFields = [];
        $scope.assignedFields = [];
        
        //console.log('args.event:  ', $rootScope.ruleeffects['SINGLE_EVENT'][0]);
        if($rootScope.ruleeffects[args.event]) {
            //Establish which event was affected:
            var affectedEvent = $scope.currentEvent;
            //In most cases the updated effects apply to the current event. In case the affected event is not the current event, fetch the correct event to affect:
            if(args.event !== affectedEvent.event) {
                angular.forEach($scope.currentStageEvents, function(searchedEvent) {
                    if(searchedEvent.event === args.event) {
                        affectedEvent = searchedEvent;
                    }
                });
            }
            angular.forEach($rootScope.ruleeffects[args.event], function(effect) {
                
                if(effect.dataElement && effect.ineffect) {
                    //in the data entry controller we only care about the "hidefield" actions
                    if(effect.action === "HIDEFIELD") {
                        if(effect.dataElement) {
                            if(affectedEvent[effect.dataElement.id]) {
                                //If a field is going to be hidden, but contains a value, we need to take action;
                                if(effect.content) {
                                    //TODO: Alerts is going to be replaced with a proper display mecanism.
                                    alert(effect.content);
                                }
                                else {
                                    //TODO: Alerts is going to be replaced with a proper display mecanism.
                                    alert($scope.prStDes[effect.dataElement.id].dataElement.displayFormName + " was blanked out and hidden by your last action");
                                }

                                //Blank out the value:
                                affectedEvent[effect.dataElement.id] = "";
                            }

                            $scope.hiddenFields[effect.dataElement.id] = effect.ineffect;
                        }
                        else {
                            $log.warn("ProgramRuleAction " + effect.id + " is of type HIDEFIELD, bot does not have a dataelement defined");
                        }
                    }
                    else if(effect.action === "HIDESECTION") {
                        if(effect.programStageSection){
                            $scope.hiddenSections[effect.programStageSection] = effect.programStageSection;
                        }
                    }
                    else if(effect.action === "SHOWERROR" && effect.dataElement.id){
                        var dialogOptions = {
                            headerText: 'validation_error',
                            bodyText: effect.content + (effect.data ? effect.data : "")
                        };
                        DialogService.showDialog({}, dialogOptions);
            
                        $scope.currentEvent[effect.dataElement.id] = $scope.currentEventOriginialValue[effect.dataElement.id];
                    }
                    else if(effect.action === "SHOWWARNING"){
                        $scope.warningMessages.push(effect.content + (effect.data ? effect.data : ""));
                    }
                    else if (effect.action === "ASSIGN") {
                        
                        //For "ASSIGN" actions where we have a dataelement, we save the calculated value to the dataelement:
                        affectedEvent[effect.dataElement.id] = effect.data;
                        $scope.assignedFields[effect.dataElement.id] = true;
                    }
                }
            });
        }
    });
    
    $scope.executeRules = function() {
        $scope.currentEvent.event = !$scope.currentEvent.event ? 'SINGLE_EVENT' : $scope.currentEvent.event;
        $scope.eventsByStage = [];
        $scope.eventsByStage[$scope.selectedProgramStage.id] = [$scope.currentEvent];
        var evs = {all: [$scope.currentEvent], byStage: $scope.eventsByStage};
        
        var flag = {debug: true, verbose: false};        
        TrackerRulesExecutionService.executeRules($scope.allProgramRules, $scope.currentEvent, evs, $scope.prStDes, $scope.selectedTei, $scope.selectedEnrollment, flag);
    };
       
    
    $scope.formatNumberResult = function(val){        
        return dhis2.validation.isNumber(val) ? val : '';
    };
    
    //check if field is hidden
    $scope.isHidden = function(id) {
        //In case the field contains a value, we cant hide it. 
        //If we hid a field with a value, it would falsely seem the user was aware that the value was entered in the UI.
        if($scope.currentEvent[id]) {
           return false; 
        }
        else {
            return $scope.hiddenFields[id];
        }
    }; 
    
    
    $scope.saveDatavalue = function(){        
        $scope.executeRules();
    };
    
    $scope.getInputNotifcationClass = function(id, custom){
        if($scope.currentElement.id && $scope.currentElement.id === id){
            if($scope.currentElement.pending){
                if(custom){
                    return 'input-pending';
                }
                return 'form-control input-pending';
            }
            if($scope.currentElement.updated){
                if(custom){
                    return 'input-success';
                }
                return 'form-control input-success';
            }          
            if($scope.currentElement.failed){
                if(custom){
                    return 'input-error';
                }
                return 'form-control input-error';
            }            
        }  
        if(custom){
            return '';
        }
        return 'form-control';
    };
    
    $scope.getClickFunction = function(dhis2Event, column){
        
        if(column.id === 'comment'){
            return "showNotes(" + dhis2Event + ")"; 
        }
        else{
            if(dhis2Event.event ===$scope.currentEvent.evnet){
                return '';
            }
            else{
                return "showEventList(" + dhis2Event + ")"; 
            }
        }        
        return '';        
    };
    
    $scope.downloadFile = function(eventUid, dataElementUid, e) {
        eventUid = eventUid ? eventUid : $scope.currentEvent.event ? $scope.currentEvent.event : null;        
        if( !eventUid || !dataElementUid){
            
            var dialogOptions = {
                headerText: 'error',
                bodyText: 'missing_file_identifier'
            };

            DialogService.showDialog({}, dialogOptions);
            return;
        }
        
        $window.open('../api/events/files?eventUid=' + eventUid +'&dataElementUid=' + dataElementUid, '_blank', '');
        if(e){
            e.stopPropagation();
            e.preventDefault();
        }
    };
    
    $scope.deleteFile = function(dataElement){
        
        if( !dataElement ){            
            var dialogOptions = {
                headerText: 'error',
                bodyText: 'missing_file_identifier'
            };
            DialogService.showDialog({}, dialogOptions);
            return;
        }
        
        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'remove',
            headerText: 'remove',
            bodyText: 'are_you_sure_to_remove'
        };

        ModalService.showModal({}, modalOptions).then(function(result){            
            $scope.fileNames[$scope.currentEvent.event][dataElement] = null;
            $scope.currentEvent[dataElement] = null;
            $scope.updateEventDataValue($scope.currentEvent, dataElement);
        });
    };
    
    $scope.updateFileNames = function(){        
        for(var dataElement in $scope.currentFileNames){
            if($scope.currentFileNames[dataElement]){
                if(!$scope.fileNames[$scope.currentEvent.event]){
                    $scope.fileNames[$scope.currentEvent.event] = [];
                }                 
                $scope.fileNames[$scope.currentEvent.event][dataElement] = $scope.currentFileNames[dataElement];
            }
        }
    };
});
