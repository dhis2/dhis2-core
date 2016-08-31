//Controller for the dashboard widgets
trackerCapture.controller('DashboardWidgetsController', 
    function($scope, 
            $modalInstance){
    
    $scope.close = function () {
        $modalInstance.close();
    };       
});