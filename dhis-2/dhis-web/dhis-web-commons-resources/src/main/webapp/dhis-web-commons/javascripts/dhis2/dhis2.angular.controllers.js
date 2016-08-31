'use strict';

/* Controllers */
var d2Controllers = angular.module('d2Controllers', [])

//Controller for column show/hide
.controller('ColumnDisplayController', 
    function($scope, 
            $modalInstance, 
            hiddenGridColumns,
            gridColumns){
    
    $scope.gridColumns = gridColumns;
    $scope.hiddenGridColumns = hiddenGridColumns;
    
    $scope.close = function () {
      $modalInstance.close($scope.gridColumns);
    };
    
    $scope.showHideColumns = function(gridColumn){
       
        if(gridColumn.show){                
            $scope.hiddenGridColumns--;            
        }
        else{
            $scope.hiddenGridColumns++;            
        }      
    };    
})

//controller for dealing with google map
.controller('MapController',
        function($scope, 
                $modalInstance,
                CurrentSelection,
                DHIS2URL,                
                location) {
    
    $scope.home = function(){        
        window.location = DHIS2URL;
    };
    
    $scope.location = location;
    
    $scope.close = function () {
        $modalInstance.close();
    };
    
    $scope.captureCoordinate = function(){
        $scope.location = CurrentSelection.getLocation();
        $modalInstance.close($scope.location);
    }; 
})

//Controller for audit history
.controller('AuditHistoryController', function( $scope, $modalInstance, $modal, AuditHistoryDataService,
                                                              dataElementId, dataElementName, currentEvent, dataType, selectedTeiId,
                                                              DateUtils ) {

    $scope.close = function() {
        $modalInstance.close();
    };

    $scope.trackedEntity = dataElementName;

    AuditHistoryDataService.getAuditHistoryData(dataElementId, dataType, dataElementName, currentEvent, selectedTeiId).then(function( data ) {

        $scope.itemList = [];

        var reponseData = data.trackedEntityDataValueAudits ? data.trackedEntityDataValueAudits : data.trackedEntityAttributeValueAudits;
        if( reponseData ) {
            angular.forEach(reponseData, function( dataValue ) {
                /*The true/false values are displayed as Yes/No*/
                if (dataValue.value === "true") {
                    dataValue.value = "Yes";
                } else if (dataValue.value === "false") {
                    dataValue.value = "No";
                }
                $scope.itemList.push({created: DateUtils.formatToHrsMinsSecs(dataValue.created), value: dataValue.value, auditType: dataValue.auditType,
                    modifiedBy:dataValue.modifiedBy});
            });
        }
    });

});


