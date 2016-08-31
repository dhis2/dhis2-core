/* global trackerCapture */

trackerCapture.controller('SelectedInfoController',
        function($scope,                
                SessionStorageService,
                CurrentSelection) {
    $scope.dashboardReady = false;                
    //listen for the selected items
    $scope.$on('selectedItems', function(event, args) {
        $scope.dashboardReady = true;
        var selections = CurrentSelection.get();
        $scope.selectedEntity = selections.tei; 
        $scope.selectedProgram = selections.pr; 
        
        $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
        $scope.selections = [];
        
        $scope.selections.push({title: 'registering_unit', value: $scope.selectedOrgUnit ? $scope.selectedOrgUnit.displayName : 'not_selected'});
        $scope.selections.push({title: 'program', value: $scope.selectedProgram ? $scope.selectedProgram.displayName : 'not_selected'});               
        
    });     
});