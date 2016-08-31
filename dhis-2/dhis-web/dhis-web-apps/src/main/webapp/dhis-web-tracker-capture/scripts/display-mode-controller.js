//Controller for column show/hide
trackerCapture.controller('DisplayModeController',
        function($scope, $modalInstance) {
    
    $scope.close = function () {
      $modalInstance.close($scope.gridColumns);
    };
});