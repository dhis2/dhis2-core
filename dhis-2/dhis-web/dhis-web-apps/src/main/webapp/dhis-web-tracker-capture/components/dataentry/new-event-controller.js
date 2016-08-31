
/* global trackerCapture, angular */

trackerCapture.controller('EventCreationController',
        function ($scope,
                $modalInstance,
                $timeout,
                $translate,
                DateUtils,
                DHIS2EventFactory,
                OrgUnitFactory,
                DialogService,
                EventCreationService,
                eventsByStage,
                stage,
                stages,
                allStages,
                tei,
                program,
                orgUnit,
                enrollment,                
                eventCreationAction,
                autoCreate,
                EventUtils,
                events,
                suggestedStage,
                selectedCategories) {
    $scope.stages = stages;
    $scope.allStages = allStages;
    $scope.events = events;
    $scope.eventCreationAction = eventCreationAction;
    $scope.eventCreationActions = EventCreationService.eventCreationActions;
    $scope.isNewEvent = (eventCreationAction === $scope.eventCreationActions.add);
    $scope.isScheduleEvent = (eventCreationAction === $scope.eventCreationActions.schedule || eventCreationAction === $scope.eventCreationActions.referral);
    $scope.isReferralEvent = (eventCreationAction === $scope.eventCreationActions.referral);
    $scope.model = {selectedStage: stage, dueDateInvalid: false, eventDateInvalid: false, currentSelectedIndex: 0};
    $scope.stageSpecifiedOnModalOpen = angular.isObject(stage) ? true : false;
    $scope.suggestedStage = suggestedStage;
    $scope.selectedProgram = program;
    $scope.selectedCategories = selectedCategories;
    $scope.pleaseSelectLabel = $translate.instant('please_select');

    var dummyEvent = {};
    
    function prepareEvent(){
        
        dummyEvent = EventUtils.createDummyEvent(eventsByStage[stage.id], tei, program, stage, orgUnit, enrollment);
        
        $scope.newEvent = {programStage: stage};
        $scope.dhis2Event = {eventDate: $scope.isScheduleEvent ? '' : DateUtils.getToday(), dueDate: dummyEvent.dueDate, excecutionDateLabel : dummyEvent.excecutionDateLabel, name: dummyEvent.name, invalid: true};        

        if ($scope.model.selectedStage.periodType) {
            $scope.dhis2Event.eventDate = dummyEvent.dueDate;
            $scope.dhis2Event.periodName = dummyEvent.periodName;
            $scope.dhis2Event.periods = dummyEvent.periods;
            $scope.dhis2Event.selectedPeriod = dummyEvent.periods[0];
            $scope.model.currentSelectedIndex = 0;
        }
    };
    
    function suggestStage(){        
        var suggestedStage;
        var events = $scope.events;        
        var allStages = $scope.allStages;
        
        var availableStagesOrdered = $scope.stages.slice();
        availableStagesOrdered.sort(function (a,b){
            return a.sortOrder - b.sortOrder;
        });
        
        var stagesById = [];
        
        if((angular.isUndefined(events) || events.length === 0) && angular.isUndefined($scope.suggestedStage)){
            suggestedStage = availableStagesOrdered[0];
        }
        else{
            angular.forEach(allStages, function(stage){
                stagesById[stage.id] = stage;
            });
            
            var lastStageForEvents;
            
            if(angular.isUndefined($scope.suggestedStage)){
                for(i = 0; i < events.length; i++){
                    var event = events[i];
                    var eventStage = stagesById[event.programStage];
                        if(i > 0){
                            if(eventStage.sortOrder > lastStageForEvents.sortOrder){
                                lastStageForEvents = eventStage;
                            }
                            else if(eventStage.sortOrder === lastStageForEvents.sortOrder){
                                if(eventStage.id !== lastStageForEvents.id){
                                    if(eventStage.displayName.localeCompare(lastStageForEvents.displayName) > 0){
                                        lastStageForEvents = eventStage;
                                    }
                                }                            
                            }
                        }
                        else {
                            lastStageForEvents = eventStage;
                        }
                }   
            }
            else {
                lastStageForEvents = $scope.suggestedStage;
            }

            
            for(j = 0; j < availableStagesOrdered.length; j++){
                var availableStage = availableStagesOrdered[j];
                
                if(availableStage.id === lastStageForEvents.id){
                    suggestedStage = availableStage;
                    break;
                }
                else if(availableStage.sortOrder === lastStageForEvents.sortOrder){
                    if(availableStage.displayName.localeCompare(lastStageForEvents.displayName) > 0){
                        suggestedStage = availableStage;
                        break;
                    }
                }
                else if(availableStage.sortOrder > lastStageForEvents.sortOrder){
                    suggestedStage = availableStage;
                    break;
                }
            }
            
            if(angular.isUndefined(suggestedStage)){
                suggestedStage = availableStagesOrdered[availableStagesOrdered.length - 1];
            }
        }
        
        $scope.model.selectedStage = suggestedStage;
        stage = $scope.model.selectedStage;
    };
    
    if(!$scope.stageSpecifiedOnModalOpen){
        //suggest stage
        suggestStage();        
    }
    
    
    $scope.$watch('model.selectedStage', function(){       
        if(angular.isObject($scope.model.selectedStage)){
            stage = $scope.model.selectedStage;            
            prepareEvent();
            $scope.model.selectedStage.excecutionDateLabel = $scope.model.selectedStage.excecutionDateLabel ? $scope.model.selectedStage.excecutionDateLabel : $translate.instant('report_date');
            //If the caller wants to create right away, go ahead and save.
            if (autoCreate) {
                $scope.save();
            };
        }
    });    

    $scope.getCategoryOptions = function(){
        $scope.selectedOptions = [];
        for (var i = 0; i < $scope.selectedCategories.length; i++) {
            if ($scope.selectedCategories[i].selectedOption && $scope.selectedCategories[i].selectedOption.id) {
                $scope.selectedOptions.push($scope.selectedCategories[i].selectedOption.id);
            }
        }
    };

    $scope.save = function () {

        $scope.getCategoryOptions();
        
        //check for form validity
        $scope.eventCreationForm.submitted = true;        
        if( $scope.eventCreationForm.$invalid ){
            return false;
        }
        
        
        if($scope.isReferralEvent && !$scope.selectedSearchingOrgUnit){
            $scope.orgUnitError = true;
            return false;
        }        
        
        $scope.orgUnitError =  false;
        
        if ($scope.model.selectedStage.periodType) {
            $scope.dhis2Event.eventDate = $scope.dhis2Event.selectedPeriod.endDate;
            $scope.dhis2Event.dueDate = $scope.dhis2Event.selectedPeriod.endDate;
        }
        
        var eventDate = DateUtils.formatFromUserToApi($scope.dhis2Event.eventDate);
        var dueDate = DateUtils.formatFromUserToApi($scope.dhis2Event.dueDate);
        var newEvents = {events: []};
        var newEvent = {
            trackedEntityInstance: dummyEvent.trackedEntityInstance,
            program: dummyEvent.program,
            programStage: dummyEvent.programStage,
            enrollment: dummyEvent.enrollment,
            orgUnit: dummyEvent.orgUnit,
            dueDate: dueDate,
            eventDate: eventDate,
            notes: [],
            dataValues: [],
            status: 'ACTIVE'
        };

        newEvent.status = newEvent.eventDate ? 'ACTIVE' : 'SCHEDULE';

        /*for saving category combo*/
        if ($scope.selectedProgram.categoryCombo && !$scope.selectedProgram.categoryCombo.isDefault) {
            if ($scope.selectedOptions.length !== $scope.selectedCategories.length) {
                var dialogOptions = {
                    headerText: 'error',
                    bodyText: 'fill_all_category_options'
                };
                DialogService.showDialog({}, dialogOptions);
                return;
            }
            newEvent.attributeCategoryOptions = $scope.selectedOptions.join(';');
        }
        /*for saving category combo*/

        newEvents.events.push(newEvent);
        DHIS2EventFactory.create(newEvents).then(function (response) {
            if (response.response && response.response.importSummaries[0].status === 'SUCCESS') {
                newEvent.event = response.response.importSummaries[0].reference;
                $modalInstance.close({dummyEvent: dummyEvent, ev: newEvent});
            }
            else {
                var dialogOptions = {
                    headerText: 'event_creation_error',
                    bodyText: response.message
                };
                
                $scope.eventCreationForm.submitted = false;
                DialogService.showDialog({}, dialogOptions);
            }
        });
    };

    //Start referral logic
    $scope.setSelectedSearchingOrgUnit = function(orgUnit){
        $scope.selectedSearchingOrgUnit = orgUnit;
        dummyEvent.orgUnit = orgUnit.id;
        dummyEvent.orgUnitName = orgUnit.name;
    };
    
    
    if(angular.isDefined(orgUnit) && angular.isDefined(orgUnit.id) && $scope.isReferralEvent){
        $scope.orgUnitsLoading = true;
        $timeout(function(){
            OrgUnitFactory.get(orgUnit.id).then(function(data){
                if(data && data.organisationUnits && data.organisationUnits.length >0){
                    orgUnit = data.organisationUnits[0];
                    var url = generateFieldsUrl();
                    OrgUnitFactory.getOrgUnits(orgUnit.id,url).then(function(data){
                        if(data && data.organisationUnits && data.organisationUnits.length >0){
                            $scope.orgWithParents = data.organisationUnits[0];
                            var org = data.organisationUnits[0];
                            var orgUnitsById ={};
                            orgUnitsById[org.id] = org;
                            while(org.parent){
                                org.parent.childrenLoaded = true;
                                orgUnitsById[org.parent.id] = org.parent;
                                org.parent.show = true;
                                for(var i=0;i<org.parent.children.length;i++){
                                    angular.forEach(org.parent.children[i].children, function(child){
                                       if(!orgUnitsById[child.id]){
                                            orgUnitsById[child.id] =child; 
                                       }
                                    });
                                    if(org.parent.children[i].id===org.id){
                                        org.parent.children[i] = org;
                                        i= org.parent.children.length;
                                    }else{
                                        orgUnitsById[org.parent.children[i].id] = org.parent.children[i];
                                    }
                                }
                                org = org.parent;
                            }
                            $scope.orgUnits = [org];
                        }
                        $scope.orgUnitsLoading = false;
                    });
                }

                
            });
            
        },350);
    }
    
    function generateFieldsUrl(){
        var fieldUrl = "fields=id,displayName,organisationUnitGroups[shortName]";
        var parentStartDefault = ",parent[id,displayName,children[id,displayName,organisationUnitGroups[shortName],children[id,displayName,organisationUnitGroups[shortName]]]";
        var parentEndDefault = "]";
        if(orgUnit.level && orgUnit.level > 1){
            var parentStart = parentStartDefault;
            var parentEnd = parentEndDefault;
            for(var i =0; i< orgUnit.level-2;i++){
                parentStart+= parentStartDefault;
                parentEnd +=parentStartDefault;
            }
            fieldUrl += parentStart;
            fieldUrl += parentEnd;
        }
        return fieldUrl;
    }
    
    $scope.expandCollapse = function(orgUnit) {
        orgUnit.show = !orgUnit.show;
        if(!orgUnit.childrenLoaded){
            OrgUnitFactory.get(orgUnit.id).then(function(data){
                orgUnit.children = data.organisationUnits[0].children;
                orgUnit.childrenLoaded = true;
                
            });
        }
    };
    //end referral logic
    $scope.cancel = function () {
        $modalInstance.close();
    };
    
    $scope.interacted = function(field) {        
        var status = false;
        if(field){            
            status = $scope.eventCreationForm.submitted || field.$dirty;
        }
        return status;        
    };


    $scope.updateSelection = function() {
        for (var index = 0; index < $scope.dhis2Event.periods.length; index++) {
            if ($scope.dhis2Event.periods[index].id === $scope.dhis2Event.selectedPeriod.id) {
                $scope.model.currentSelectedIndex = index;
                break;
            }
        }
    };

    $scope.fetchPeriod = function (period) {
        if (period === "PREV") {
            $scope.dhis2Event.selectedPeriod = $scope.dhis2Event.periods[$scope.model.currentSelectedIndex-1];
        } else if (period === "NEXT") {
            $scope.dhis2Event.selectedPeriod = $scope.dhis2Event.periods[$scope.model.currentSelectedIndex+1];
        }
        $scope.updateSelection();
    };

});
