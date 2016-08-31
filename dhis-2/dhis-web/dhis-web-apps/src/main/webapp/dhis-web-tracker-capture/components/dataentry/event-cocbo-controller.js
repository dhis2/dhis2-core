/* global trackerCapture */

trackerCapture.controller('EventCategoryComboController', 
        function($scope, 
                $modalInstance, 
                DialogService, 
                selectedProgram, 
                selectedCategories,
                selectedTeiId,
                DHIS2EventFactory){
                    
    $scope.selectedOptions = [];
    $scope.selectedProgram = selectedProgram;
    $scope.selectedCategories  = selectedCategories;
    $scope.selectedTeiId = selectedTeiId;
    $scope.applyOptions = function(){                    
        var attributeCategory = {cc: $scope.selectedProgram.categoryCombo.id, default: $scope.selectedProgram.categoryCombo.isDefault, cp: ""};
        if(!$scope.selectedProgram.categoryCombo.isDefault){            
            if($scope.selectedOptions.length !== $scope.selectedCategories.length){
                var dialogOptions = {
                    headerText: 'error',
                    bodyText: 'fill_all_category_options'
                };

                DialogService.showDialog({}, dialogOptions);
                return;
            }            
            attributeCategory.cp = $scope.selectedOptions.join(';');
        }

        DHIS2EventFactory.getEventsByProgram($scope.selectedTeiId, $scope.selectedProgram.id, attributeCategory).then(function(events){
            $scope.close( events );
        });
    };

    $scope.getCategoryOptions = function(){
        $scope.selectedOptions = [];
        for (var i = 0; i < $scope.selectedCategories.length; i++) {
            if ($scope.selectedCategories[i].selectedOption && $scope.selectedCategories[i].selectedOption.id) {
                $scope.selectedOptions.push($scope.selectedCategories[i].selectedOption.id);
            }
        }
    };

    $scope.close = function ( obj ) {
        $modalInstance.close( obj );
    };    
});

