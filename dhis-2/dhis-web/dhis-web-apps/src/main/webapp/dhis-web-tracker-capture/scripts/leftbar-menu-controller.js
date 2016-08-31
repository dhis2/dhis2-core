//Controller for column show/hide
trackerCapture.controller('LeftBarMenuController',
        function($scope,
                $location) {
    $scope.showHome = function(){
        $location.path('/').search();
    }; 
    
    $scope.showReportTypes = function(){
        $location.path('/report-types').search();
    };
});