/* global directive, selection, dhis2, angular */

'use strict';

/* Directives */

var trackerCaptureDirectives = angular.module('trackerCaptureDirectives', [])

.directive('stringToNumber', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, ngModel) {
            ngModel.$parsers.push(function (value) {
                return '' + value;
            });
            ngModel.$formatters.push(function (value) {
                return parseFloat(value, 10);
            });
        }
    };
})

.directive('eventstatusInTable', function (){
    return {
        restrict: 'E',
        templateUrl: 'components/dataentry/eventstatus-in-table.html',
        scope: {
            event: '=',            
            chosenEventWrapped: '=', //two-way-binding not working if not wrapped in object!
            getEventStyle: '=',
            programStage: '=',
            optionSets: '=',            
            completeActionCustom: '=', //optional
            reopenActionCustom: '=', //optional
            validateActionCustom: '=', //optional
            deleteActionCustom: '=', //optional
            skipActionCustom: '=', //optional
            unskipActionCustom: '=', //optional
            notesActionCustom: '=', //optional            
            applicableButtons: '=', //optional
            actions: '=',
            allEvents: '=',
            formData: '=',
            buttonsEnabled: '&'
        },
        controller: [
            '$scope',
            '$element',
            '$attrs',
            '$q',
            'EventUtils',
            'DHIS2EventFactory',
            'DialogService',
            '$translate',
            function($scope, $element, $attrs, $q, EventUtils, DHIS2EventFactory, DialogService, $translate){
                
                $scope.EVENTSTATUSCOMPLETELABEL = "COMPLETED";
                $scope.EVENTSTATUSSKIPPEDLABEL = "SKIPPED";
                $scope.EVENTSTATUSVISITEDLABEL = "VISITED";
                $scope.EVENTSTATUSACTIVELABEL = "ACTIVE";
                $scope.EVENTSTATUSSCHEDULELABEL = "SCHEDULE";
                var COMPLETE = "Complete";
                var INCOMPLETE = "Incomplete";
                var VALIDATE = "Validate";
                var DELETE = "Delete";    
                var SKIP = "Skip";
                var UNSKIP = "Unskip";
                var NOTE = "Note";     
                
                $scope.completeAction = function() {                    
                    if(angular.isDefined($scope.completeActionCustom)){
                        $scope.completeActionCustom();
                    }
                    else {
                        $scope.completeActionDefault();
                    }
                };     
                
                $scope.reopenAction = function() {                    
                     if(angular.isDefined($scope.reopenActionCustom)){
                        $scope.reopenActionCustom();
                    }
                    else {
                        $scope.reopenActionDefault();
                    }                  
                };
                
                $scope.validateAction = function(){
                    if(angular.isDefined($scope.validateActionCustom)){
                        $scope.validateActionCustom();
                    }                    
                };
                
                $scope.deleteAction = function(){
                    if(angular.isDefined($scope.deleteActionCustom)){
                        $scope.deleteActionCustom();
                    }
                    else {
                        $scope.deleteActionDefault();
                    }

                };
                
                $scope.skipAction = function(){
                    if(angular.isDefined($scope.skipActionCustom)){
                        $scope.skipActionCustom();
                    }                    
                };
                
                $scope.unskipAction = function(){
                    if(angular.isDefined($scope.unskipActionCustom)){
                        $scope.unskipActionCustom();
                    }                    
                };
                
                $scope.showNotes = function(){
                    if(angular.isDefined($scope.notesActionCustom)){
                        $scope.notesActionCustom();
                    }
                    else {
                        $scope.notesModal();
                    }                                       
                };
                
                $scope.eventTableOptions = {};    
                $scope.eventTableOptions[COMPLETE] = {text: "Complete", tooltip: 'Complete', icon: "<span class='glyphicon glyphicon-check'></span>", value: COMPLETE, onClick: $scope.completeAction, sort: 0};
                $scope.eventTableOptions[INCOMPLETE] = {text: "Reopen", tooltip: 'Reopen', icon: "<span class='glyphicon glyphicon-pencil'></span>", value: INCOMPLETE, onClick: $scope.reopenAction, sort: 1};
                $scope.eventTableOptions[VALIDATE] = {text: "Validate", tooltip: 'Validate', icon: "<span class='glyphicon glyphicon-cog'></span>", value: VALIDATE, onClick: $scope.validateAction, sort: 2};    
                $scope.eventTableOptions[DELETE] = {text: "Delete", tooltip: 'Delete', icon: "<span class='glyphicon glyphicon-floppy-remove'></span>", value: DELETE, onClick: $scope.deleteAction, sort: 3};
                $scope.eventTableOptions[SKIP] = {text: "Skip", tooltip: 'Skip', icon: "<span class='glyphicon glyphicon-step-forward'></span>", value: SKIP, onClick: $scope.skipAction, sort: 4};
                $scope.eventTableOptions[UNSKIP] = {text: "Schedule back", tooltip: 'Schedule back', icon: "<span class='glyphicon glyphicon-step-backward'></span>", value: UNSKIP, onClick: $scope.unskipAction, sort: 5};
                $scope.eventTableOptions[NOTE] = {text: "Notes", tooltip: 'Show notes', icon: "<span class='glyphicon glyphicon-list-alt'></span>", value: NOTE, onClick: $scope.showNotes, sort: 6};
                
                $scope.event.validatedEventDate = $scope.event.eventDate;   
    
                updateEventTableOptions();

                $scope.$watch("event.status", function(newValue, oldValue){        
                    
                    if(newValue !== oldValue){
                        updateEventTableOptions();
                    }
                });

                $scope.$watch("validatedDateSetForEvent", function(newValue, oldValue){                        

                    if(angular.isDefined(newValue)){
                        if(!angular.equals(newValue, {})){
                            var updatedEvent = newValue.event;
                            if(updatedEvent === $scope.event){                    
                                    $scope.event.validatedEventDate = newValue.date; 
                                    updateEventTableOptions();                    
                            }
                        }
                    } 
                });

                function updateEventTableOptions(){

                    var eventRow = $scope.event;        

                    for(var key in $scope.eventTableOptions){
                        $scope.eventTableOptions[key].show = true;
                        $scope.eventTableOptions[key].disabled = false;
                    }

                    $scope.eventTableOptions[UNSKIP].show = false;        

                    switch(eventRow.status){
                        case $scope.EVENTSTATUSCOMPLETELABEL:
                            $scope.eventTableOptions[COMPLETE].show = false;
                            $scope.eventTableOptions[SKIP].show = false;                            
                            $scope.eventTableOptions[VALIDATE].show = false;                                      
                            $scope.defaultOption = $scope.eventTableOptions[INCOMPLETE];
                            $scope.defaultOption2 = $scope.eventTableOptions[DELETE];
                            break;
                        case $scope.EVENTSTATUSSKIPPEDLABEL:
                            $scope.eventTableOptions[COMPLETE].show = false;
                            $scope.eventTableOptions[INCOMPLETE].show = false;
                            $scope.eventTableOptions[VALIDATE].show = false;                
                            $scope.eventTableOptions[SKIP].show = false;

                            $scope.eventTableOptions[UNSKIP].show = true;
                            $scope.defaultOption = $scope.eventTableOptions[UNSKIP];
                            $scope.defaultOption2 = $scope.eventTableOptions[DELETE];
                            break;            
                        default:                 
                            if(eventRow.validatedEventDate){
                                $scope.eventTableOptions[INCOMPLETE].show = false;
                                $scope.defaultOption = $scope.eventTableOptions[COMPLETE];
                                $scope.defaultOption2 = $scope.eventTableOptions[DELETE];
                            }
                            else {
                                $scope.eventTableOptions[INCOMPLETE].show = false;                    
                                $scope.eventTableOptions[VALIDATE].show = false;
                                $scope.eventTableOptions[COMPLETE].disabled = true;
                                $scope.defaultOption = $scope.eventTableOptions[COMPLETE];
                                $scope.defaultOption2 = $scope.eventTableOptions[DELETE];
                            }                          
                            break;
                    }

                    createOptionsArray();
                }

                function createOptionsArray(){
                    $scope.eventTableOptionsArr = [];                            
                   
                    if(angular.isDefined($scope.applicableButtons)){
                        var defaultFound = false;
                        for(var key in $scope.eventTableOptions){
                            var show = false;
                            
                            for(var i = 0; i < $scope.applicableButtons.length; i++){
                                if($scope.applicableButtons[i] === key){
                                    show = true;
                                    break;
                                }
                            }
                            
                            if(show){
                                if($scope.eventTableOptions[key] === $scope.defaultOption){
                                    defaultFound = true;
                                }
                                $scope.eventTableOptionsArr.push($scope.eventTableOptions[key]);
                            }
                        }
                        
                        $scope.eventTableOptionsArr.sort(function(a,b){                           
                            return a.sort - b.sort;
                        });
                        
                        if(!defaultFound){
                            $scope.defaultOption = $scope.defaultOption2;
                        }
                    }
                    else {
                        for(var key in $scope.eventTableOptions){
                            $scope.eventTableOptionsArr[$scope.eventTableOptions[key].sort] = $scope.eventTableOptions[key];
                        }
                    }
                }
                
                //-----------                
                $scope.notesModal = function(){
                    
                    var def = $q.defer();
                    
                    var bodyList = [];
                    if($scope.event.notes) {
                        for(var i = 0; i < $scope.event.notes.length; i++){
                            var currentNote = $scope.event.notes[i];            
                            bodyList.push({value1: currentNote.storedDate, value2: currentNote.value});
                        }
                    }

                    var dialogOptions = {
                        closeButtonText: 'Close',            
                        textAreaButtonText: 'Add',
                        textAreaButtonShow: $scope.event.status === $scope.EVENTSTATUSSKIPPEDLABEL ? false : true,
                        headerText: 'Notes',
                        bodyTextAreas: [{model: 'note', placeholder: 'Add another note here', required: true, show: $scope.event.status === $scope.EVENTSTATUSSKIPPEDLABEL ? false : true}],
                        bodyList: bodyList,
                        currentEvent: $scope.event
                    };        

                    var dialogDefaults = {

                        templateUrl: 'views/list-with-textarea-modal.html',
                        controller: function ($scope, $modalInstance, DHIS2EventFactory, DateUtils) {                    
                                $scope.modalOptions = dialogOptions;
                                $scope.formSubmitted = false;
                                $scope.currentEvent = $scope.modalOptions.currentEvent;
                                $scope.textAreaValues = [];

                                $scope.textAreaButtonClick = function(){                                               
                                    if($scope.textAreaModalForm.$valid){                                
                                        $scope.note = $scope.textAreaValues["note"];
                                        $scope.addNote();
                                        $scope.textAreaModalForm.$setUntouched();
                                        $scope.formSubmitted = false;                            
                                    }
                                    else {
                                        $scope.formSubmitted = true;
                                    }
                                };                   

                                $scope.modalOptions.close = function(){                        
                                    $modalInstance.close($scope.currentEvent);
                                };

                                $scope.addNote = function(){

                                    var newNote = {value: $scope.note};
                                    var date = DateUtils.formatToHrsMins(new Date());                                    

                                    var e = {event: $scope.currentEvent.event,
                                            program: $scope.currentEvent.program,
                                            programStage: $scope.currentEvent.programStage,
                                            orgUnit: $scope.currentEvent.orgUnit,
                                            trackedEntityInstance: $scope.currentEvent.trackedEntityInstance,
                                            notes: [newNote]
                                    };
                                    
                                    DHIS2EventFactory.updateForNote(e).then(function (data) {
                                        if (angular.isUndefined($scope.modalOptions.bodyList) || $scope.modalOptions.bodyList.length === 0) {
                                            $scope.modalOptions.bodyList = [{value1: date, value2: newNote.value}];
                                            $scope.modalOptions.currentEvent.notes = [{storedDate: date, value: newNote.value}];
                                        }
                                        else {
                                            $scope.modalOptions.bodyList.splice(0, 0, {value1: date, value2: newNote.value});
                                            $scope.modalOptions.currentEvent.notes.splice(0,0,{storedDate: date, value: newNote.value});
                                        }
                                            $scope.note = $scope.textAreaValues["note"] = "";
                                    });

                                };                    
                            }            
                    };

                    DialogService.showDialog(dialogDefaults, dialogOptions).then(function(e){                        
                        $scope.event.notes = e.notes;
                        def.resolve();
                    });
                    
                    return def.promise;
                };   
                
                if(angular.isDefined($scope.actions)){                    
                    $scope.actions[$scope.event.event] = {};
                    $scope.actions[$scope.event.event].notes = $scope.notesModal;
                }
                //-----------
                
                $scope.deleteActionDefault = function() {

                    DHIS2EventFactory.delete($scope.event).then(function (data) {
                        
                        var foundIndex = -1;
                        //find index
                        for(var i = 0; i < $scope.allEvents.length; i++){
                            if($scope.allEvents[i] === $scope.event){
                                foundIndex = i;
                                break;
                            }
                        }
                        
                        if(foundIndex !== -1){
                            $scope.allEvents.splice(foundIndex,1);                            
                        }
                        setChosenEventToNothing();
                    });
                };
                
                $scope.completeActionDefault = function() {                    
                    
                    $scope.event.submitted = true;
                    if($scope.formData.$valid){
                        var dhis2EventToUpdate = makeDhis2EventToUpdate();
                        dhis2EventToUpdate.status = $scope.EVENTSTATUSCOMPLETELABEL;
                        DHIS2EventFactory.update(dhis2EventToUpdate).then(function (data) {
                            $scope.event.status = $scope.EVENTSTATUSCOMPLETELABEL;                                                  
                            setChosenEventToNothing();  
                            
                            //reset dataElementStatus for event
                            $scope.event.deStatus = {};
                        }, function(){
                            
                        });
                    }
                };
                
                $scope.reopenActionDefault = function () {
                    var dhis2EventToUpdate = makeDhis2EventToUpdate();
                    dhis2EventToUpdate.status = $scope.EVENTSTATUSACTIVELABEL;
                    DHIS2EventFactory.update(dhis2EventToUpdate).then(function (data) {
                        $scope.event.status = $scope.EVENTSTATUSACTIVELABEL;
                    });
                }
                
                function makeDhis2EventToUpdate() {
                    
                    var dhis2EventToUpdate = {};
                    
                    if(angular.isDefined($scope.programStage) && angular.isDefined($scope.optionSets)){
                                                
                        var dhis2Event = EventUtils.reconstruct($scope.event, $scope.programStage, $scope.optionSets);
                        dhis2EventToUpdate = angular.copy(dhis2Event);
                    }
                    else {
                        dhis2EventToUpdate = angular.copy($scope.event);                      
                    }
                    
                    /*
                    dhis2EventToUpdate.dataValues = [];
                    
                    for(var key in $scope.event[assocValuesProp]){
                        dhis2EventToUpdate.dataValues.push($scope.event[assocValuesProp][key]);
                    } */                   
                    
                    return dhis2EventToUpdate;
                }
                
                function setChosenEventToNothing(){
                    $scope.chosenEventWrapped.currentEvent = {};
                }
                
                $scope.$watch('chosenEventWrapped.currentEvent', function(newEvent, oldEvent){                          
                    if(angular.isDefined(newEvent)){
                        
                        if(newEvent !== oldEvent){
                            $scope.chosenEvent = newEvent;
                        }
                    }                                        
                });
                
                
            }
        ]
    };
})
.directive('dhis2RadioButton', function (){  
    return {
        restrict: 'E',
        templateUrl: 'views/dhis2-radio-button.html',
        scope: {
            required: '=dhRequired',
            value: '=dhValue',
            disabled: '=dhDisabled',
            name: '@dhName',            
            customOnClick: '&dhClick',
            currentElement: '=dhCurrentElement',
            event: '=dhEvent',
            id: '=dhId'
        },
        controller: [
            '$scope',
            '$element',
            '$attrs',
            '$q',   
            'CommonUtils',
            function($scope, $element, $attrs, $q, CommonUtils){
                
                $scope.status = "";                
                $scope.clickedButton = "";
                
                $scope.valueClicked = function (buttonValue){
                                        
                    $scope.clickedButton = buttonValue;
                    
                    var originalValue = $scope.value;
                    var tempValue = buttonValue;
                    if($scope.value === buttonValue){
                        tempValue = "";
                    }
                    
                    if(angular.isDefined($scope.customOnClick)){
                        var promise = $scope.customOnClick({value: tempValue});
                        if(angular.isDefined(promise) && angular.isDefined(promise.then)){
                            promise.then(function(status){
                                if(angular.isUndefined(status) || status !== "notSaved"){
                                    $scope.status = "saved";                                    
                                }
                                $scope.value = tempValue;                            
                            }, function(){   
                                $scope.status = "error";
                                $scope.value = originalValue;
                            });
                        }
                        else if(angular.isDefined(promise)){
                            if(promise === false){
                                $scope.value = originalValue;
                            }
                            else {
                                $scope.value = tempValue;
                            }
                        }
                        else{
                            $scope.value = tempValue;
                        }
                    }
                    else{
                        $scope.value = tempValue;
                    }
                };
                
                $scope.getDisabledValue = function(inValue){                    
                    return CommonUtils.displayBooleanAsYesNo(inValue);                    
                };
                
                $scope.getDisabledIcon = function(inValue){                    
                    if(inValue === true || inValue === "true"){
                        return "fa fa-check";
                    }
                    else if(inValue === false || inValue === "false"){
                        return "fa fa-times";
                    }
                    return '';
                }
                
            }],
        link: function (scope, element, attrs) {
            
            scope.radioButtonColor = function(buttonValue){
                
                if(scope.value !== ""){
                    if(scope.status === "saved"){
                        if(angular.isUndefined(scope.currentElement) || (scope.currentElement.id === scope.id && scope.currentElement.event === scope.event)){
                            if(scope.clickedButton === buttonValue){
                                return 'radio-save-success';
                            }
                        }                                            
                    //different solution with text chosen
                    /*else if(scope.status === "error"){
                        if(scope.clickedButton === buttonValue){
                            return 'radio-save-error';
                        }
                    }*/
                    }
                }                
                return 'radio-white';
            };
            
            scope.errorStatus = function(){
                
                if(scope.status === 'error'){
                    if(angular.isUndefined(scope.currentElement) || (scope.currentElement.id === scope.id && scope.currentElement.event === scope.event)){
                        return true;
                    }
                }
                return false;
            };

            scope.radioButtonImage = function(buttonValue){        

                if(angular.isDefined(scope.value)){
                    if(scope.value === buttonValue && buttonValue === "true"){
                        return 'fa fa-stack-1x fa-check';                
                    }            
                    else if(scope.value === buttonValue && buttonValue === "false"){
                        return 'fa fa-stack-1x fa-times';
                    }
                }
                return 'fa fa-stack-1x';        
            };    
        }
    };
})

.directive('dhis2Deselect', function ($document) {
    return {
        restrict: 'A',
        scope: {
            onDeselected: '&dhOnDeselected',
            id: '@dhId',
            preSelected: '=dhPreSelected'                   
        },
        controller: [
            '$scope',
            '$element',
            '$attrs',
            '$q',            
            function($scope, $element, $attrs, $q){
                
                $scope.documentEventListenerSet = false;
                $scope.elementClicked = false;
                
                $element.on('click', function(event) {                    
                                        
                    $scope.elementClicked = true;
                    if($scope.documentEventListenerSet === false){
                        $document.on('click', $scope.documentClick);
                        $scope.documentEventListenerSet = true;
                    }                             
                });
                
                $scope.documentClick = function(event){
                    
                    var modalPresent = $(".modal-backdrop").length > 0;
                    var calendarPresent = $(".calendars-popup").length > 0;
                    var calendarPresentInEvent = $(event.target).parents(".calendars-popup").length > 0;
                    
                    if($scope.elementClicked === false && 
                        modalPresent === false && 
                        calendarPresent === false && 
                        calendarPresentInEvent === false){                        
                        $scope.onDeselected({id:$scope.id});
                        $scope.$apply();  
                        $document.off('click', $scope.documentClick);
                        $scope.documentEventListenerSet = false;
                    }
                    $scope.elementClicked = false;
                };
                
                if(angular.isDefined($scope.preSelected) && $scope.preSelected === true){                    
                    $document.on('click', $scope.documentClick);
                    $scope.documentEventListenerSet = true;
                }
                
            }],
        link: function (scope, element, attrs) {}
    };
});
