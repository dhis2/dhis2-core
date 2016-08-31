/* global trackerCapture, angular */

trackerCapture.controller('NotesController',
        function($scope,
                DateUtils,
                EnrollmentService,
                CurrentSelection,
                DialogService,
                SessionStorageService,
                orderByFilter) {
    $scope.dashboardReady = false;
    var userProfile = SessionStorageService.get('USER_PROFILE');
    var storedBy = userProfile && userProfile.username ? userProfile.username : '';

    var today = DateUtils.getToday();
    
    $scope.note = {};
    $scope.showNotesDiv = true;
    
    $scope.$on('dashboardWidgets', function() {
        $scope.selectedEnrollment = null;
        var selections = CurrentSelection.get();
        $scope.selectedTei = selections.tei;
        $scope.dashboardReady = true;
        var selections = CurrentSelection.get();
        if(selections.selectedEnrollment && selections.selectedEnrollment.enrollment){
            EnrollmentService.get(selections.selectedEnrollment.enrollment).then(function(data){    
                $scope.selectedEnrollment = data;   
                if(!angular.isUndefined( $scope.selectedEnrollment.notes)){
                    $scope.selectedEnrollment.notes = orderByFilter($scope.selectedEnrollment.notes, '-storedDate');            
                    angular.forEach($scope.selectedEnrollment.notes, function(note){
                        note.displayDate = DateUtils.formatFromApiToUser(note.storedDate);
                        note.storedDate = DateUtils.formatToHrsMins(note.storedDate);
                    });
                }
            });
        }
    });
       
    $scope.addNote = function(){
        if(!$scope.note.value){
            var dialogOptions = {
                headerText: 'error',
                bodyText: 'please_add_some_text'
            };                

            DialogService.showDialog({}, dialogOptions);
            return;
        }

        var newNote = {value: $scope.note.value};

        if(angular.isUndefined( $scope.selectedEnrollment.notes) ){
            $scope.selectedEnrollment.notes = [{value: newNote.value, storedDate: DateUtils.formatFromUserToApi(today), displayDate: today, storedBy: storedBy}];

        }
        else{
            $scope.selectedEnrollment.notes.splice(0,0,{value: newNote.value, storedDate: DateUtils.formatFromUserToApi(today),displayDate: today, storedBy: storedBy});
        }

        var e = angular.copy($scope.selectedEnrollment);
        e.enrollmentDate = DateUtils.formatFromUserToApi(e.enrollmentDate);
        if(e.incidentDate){
            e.incidentDate = DateUtils.formatFromUserToApi(e.incidentDate);
        }
        e.notes = [newNote];
        EnrollmentService.updateForNote(e).then(function(){
            $scope.clear();
        });
    };
    
    $scope.clear = function(){
        $scope.note = {};
    };
    
    $scope.showNotes = function(){
        $scope.showNotesDiv = !$scope.showNotesDiv;
    };
});
