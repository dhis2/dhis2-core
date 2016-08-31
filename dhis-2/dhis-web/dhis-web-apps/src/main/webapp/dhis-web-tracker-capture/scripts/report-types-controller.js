//Controller for the header section
trackerCapture.controller('ReportTypesController',
        function($scope,
                $location) {    
    $scope.programSummary = function(){
        selection.load();
        $location.path('/program-summary').search();
    };
    
    $scope.programStatistics = function(){   
        selection.load();
        $location.path('/program-statistics').search();
    };
    
    $scope.overdueEvents = function(){   
        selection.load();
        $location.path('/overdue-events').search();
    };   
    
    $scope.upcomingEvents = function(){
        selection.load();
        $location.path('/upcoming-events').search();
    };
});