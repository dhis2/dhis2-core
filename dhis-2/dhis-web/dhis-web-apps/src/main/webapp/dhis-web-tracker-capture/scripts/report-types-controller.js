//Controller for the header section
trackerCapture.controller('ReportTypesController',
        function($scope,
                $location) {    
    $scope.programSummary = function(){   
        $location.path('/program-summary').search();
    };
    
    $scope.programStatistics = function(){   
        $location.path('/program-statistics').search();
    };
    
    $scope.overdueEvents = function(){   
        $location.path('/overdue-events').search();
    };   
    
    $scope.upcomingEvents = function(){
        $location.path('/upcoming-events').search();
    };
});