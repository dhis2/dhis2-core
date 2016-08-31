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
.directive('heightChangeSource', function ($window) {
    return {
        link: function (scope, element, attrs) {
            element.css("width","100%");
            var w = angular.element($window);
            scope.getWindowDimensions = function () {
                return {
                    'h': w.height(),
                    'w': w.width()
                };
            };
            scope.$watch(function(){
                if(element.height()!== scope._height){
                    scope._height = element.height();
                }
            });
            scope.$watch(scope.getWindowDimensions, function (newValue, oldValue) {
                if(scope._height !== element.height()){
                    scope._height = element.height();
                    console.log("heightChangeSourceb");
                }

            }, true);
            
            w.bind('resize', function () {
                scope.$apply();
            });
        }
    };
})
.directive('heightChangeTarget', function () {
    return {
        link: function (scope, element, attrs) {
            scope.$watch('_height', function (newValue, oldValue) {
                if(newValue && newValue !== oldValue){
                    element.css('padding-top',newValue);     
                }

            }, true);
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
            buttonsEnabled: '&',
            deleteActionExtended: '='
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
                        var promise = $scope.deleteActionDefault();
                        if(angular.isDefined($scope.deleteActionExtended)){
                            promise.then(function(){
                                $scope.deleteActionExtended();
                            });
                        }                        
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
                
                $scope.showNoteExistsIcon = true;
                if(angular.isDefined($scope.applicableButtons)){
                    $scope.showNoteExistsIcon = false;
                    for(var i = 0; i < $scope.applicableButtons.length; i++){
                        if($scope.applicableButtons[i] === NOTE){
                            $scope.showNoteExistsIcon = true;
                            break;
                        }
                    }
                }
                
                $scope.notesSummary = function(){
                    var summary = "";
                    angular.forEach($scope.event.notes, function(note){                        
                        if(summary !== ""){
                            summary += "<br/>";
                        }
                        
                        if(note.value.length > 30){
                            //find index of space
                            var noteSubstring = note.value.substr(0, 30);
                            var lastSpace = noteSubstring.lastIndexOf(" ");
                            if(lastSpace !== -1){
                                noteSubstring = noteSubstring.substr(0, lastSpace + 1);
                            }                            
                            
                            summary += "- " + noteSubstring + "...";
                        }
                        else {
                            summary += "- " + note.value;
                        }
                    });
                    
                    var summaryHeader = $translate.instant('notes');
                    summaryHeader += ":<br/>";
                    
                    var summaryFooter = "<br/>(" + $translate.instant('click_to_edit_view_complete_notes') + ")";
                    summary = "<p align='left'>" + summaryHeader + summary + summaryFooter + "</p>";
                    return summary;
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

                    return DHIS2EventFactory.delete($scope.event).then(function (data) {
                        
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
.directive('dhis2CompiledInclude', [
  '$templateCache',
  function($templateCache) {
    return {
      restrict: 'A',
      priority: 400, // Same as ng-include
      compile: function(element, attrs){        
        var templateName = attrs.dhis2CompiledInclude;
        if(!templateName){
          throw new Error('ngInline: expected template name');
        }
        var template = $templateCache.get(templateName);
     	  if(angular.isUndefined(template)){
          throw new Error('ngInline: unknown template ' + templateName);
        }
        element.html(template);
      }
    };
  }
]);