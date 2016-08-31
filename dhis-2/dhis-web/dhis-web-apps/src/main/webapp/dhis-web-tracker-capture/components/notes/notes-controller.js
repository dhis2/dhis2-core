/* global trackerCapture, angular */

trackerCapture.controller('NotesController',
        function($scope,
                DateUtils,
                EnrollmentService,
                CurrentSelection,
                SessionStorageService,
                orderByFilter) {
    
    var userProfile = SessionStorageService.get('USER_PROFILE');
    var storedBy = userProfile && userProfile.username ? userProfile.username : '';

    var today = DateUtils.getToday();
    
    $scope.note = {};
    $scope.showMessagingDiv = false;
    $scope.showNotesDiv = true;
    
    $scope.$on('dashboardWidgets', function() {
        $scope.selectedEnrollment = null;
        var selections = CurrentSelection.get();
        $scope.selectedTei = selections.tei;
        
        var selections = CurrentSelection.get();
        if(selections.selectedEnrollment && selections.selectedEnrollment.enrollment){
            EnrollmentService.get(selections.selectedEnrollment.enrollment).then(function(data){    
                $scope.selectedEnrollment = data;   
                if(!angular.isUndefined( $scope.selectedEnrollment.notes)){
                    $scope.selectedEnrollment.notes = orderByFilter($scope.selectedEnrollment.notes, '-storedDate');            
                    angular.forEach($scope.selectedEnrollment.notes, function(note){
                        note.storedDate = DateUtils.formatToHrsMins(note.storedDate);
                    });
                }
            });
        }
        
        if($scope.selectedTei){
            //check if the selected TEI has any of the contact attributes
            //that can be used for communication
            var continueLoop = true;
            for(var i=0; i<$scope.selectedTei.attributes.length && continueLoop; i++){
                if( ($scope.selectedTei.attributes[i].type === 'phoneNumber' && $scope.selectedTei.attributes[i].show) || 
                    ($scope.selectedTei.attributes[i].type === 'email' && $scope.selectedTei.attributes[i].show) ){
                    $scope.messagingPossible = true;
                    continueLoop = false;
                }
            }
        }
    });
       
    $scope.addNote = function(){
        if ($scope.note.value !== "" || !angular.isUndefined($scope.note.value)) {
            var newNote = {value: $scope.note.value};

            if(angular.isUndefined( $scope.selectedEnrollment.notes) ){
                $scope.selectedEnrollment.notes = [{value: newNote.value, storedDate: DateUtils.formatFromUserToApi(today), storedBy: storedBy}];
                
            }
            else{
                $scope.selectedEnrollment.notes.splice(0,0,{value: newNote.value, storedDate: DateUtils.formatFromUserToApi(today), storedBy: storedBy});
            }

            var e = angular.copy($scope.selectedEnrollment);

            e.notes = [newNote];
            EnrollmentService.update(e).then(function(){
                $scope.note = {};
                $scope.addNoteField = false; //note is added, hence no need to show note field.                
            });
        }        
    };
    
    $scope.clearNote = function(){
        $scope.note = {};
    };
    
    $scope.showNotes = function(){
        $scope.showNotesDiv = !$scope.showNotesDiv;
        $scope.showMessagingDiv = !$scope.showMessagingDiv;
    };
    
    $scope.showMessaging = function(){
        $scope.showNotesDiv = !$scope.showNotesDiv;
        $scope.showMessagingDiv = !$scope.showMessagingDiv;
    };
});
