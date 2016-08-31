/* global trackerCapture, angular */

trackerCapture.controller('ActiveProgramsController',
        function($scope, 
        $location,
        $translate,
        CurrentSelection) {
    //listen for the selected items
    $scope.emptyActiveProgramLabel = $translate.instant('no_active_program');
    
    $scope.$on('selectedItems', function(event, args) {        
        var selections = CurrentSelection.get();
        $scope.selectedTeiId = selections.tei ? selections.tei.trackedEntityInstance : null;
        $scope.activeEnrollments =  [];
        $scope.selectedProgram = selections.pr ? selections.pr : null;
        angular.forEach(selections.enrollments, function(en){
            if(en.status === "ACTIVE"){
                $scope.activeEnrollments.push(en);                           
            }
        });
    });
    
    $scope.changeProgram = function(program){
        $location.path('/dashboard').search({tei: $scope.selectedTeiId, program: program});
    };
});