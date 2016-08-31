//Controller for notes
eventCaptureControllers.controller('NotesController', 
    function($scope, 
            $modalInstance, 
            dhis2Event){
    
    $scope.dhis2Event = dhis2Event;
    
    $scope.close = function () {
        $modalInstance.close();
    };      
});