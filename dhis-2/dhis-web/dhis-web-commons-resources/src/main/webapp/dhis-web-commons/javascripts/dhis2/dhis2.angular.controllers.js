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
                DialogService,
                location) {
    
    $scope.home = function(){        
        window.location = DHIS2URL;
    };
    
    $scope.location = location;
    
    $scope.close = function () {
        $modalInstance.close();
    };
    
    $scope.captureCoordinate = function(){
    	if( $scope.location && $scope.location.lng && $scope.location.lat ){
    		$scope.location = CurrentSelection.getLocation();
            $modalInstance.close($scope.location);
    	}
    	else{
    		//notify user
            var dialogOptions = {
                headerText: 'error',
                bodyText: 'nothing_captured'
            };
            DialogService.showDialog({}, dialogOptions);
            return;
    	}
    };
})

//Controller for audit history
.controller('AuditHistoryController', 
    function ($scope, 
            $modalInstance,
            $translate,
            AuditHistoryDataService, 
            DateUtils, 
            eventId, 
            dataType, 
            nameIdMap,
            optionSets,
            CommonUtils) {


    $scope.model = {type: dataType, 
    				name: dataType === 'dataElement' ? $translate.instant('data_element') : $translate.instant('attribute'),
    				searchPlaceholder: dataType === 'dataElement' ? $translate.instant('search_by_data_element') : $translate.instant('search_by_attribute'),
                    auditColumns: ['name', 'auditType', 'value', 'modifiedBy', 'created'], itemList:[], uniqueRows:[]};

    $scope.close = function () {
        $modalInstance.close();
    };
    
    AuditHistoryDataService.getAuditHistoryData(eventId, dataType).then(function (data) {

        $scope.model.itemList = [];
        $scope.model.uniqueRows = [];
        
        var reponseData = data.trackedEntityDataValueAudits ? data.trackedEntityDataValueAudits :
            data.trackedEntityAttributeValueAudits ? data.trackedEntityAttributeValueAudits : null;

        if (reponseData) {
            for (var index = 0; index < reponseData.length; index++) {                
                var dataValue = reponseData[index];                
                var audit = {}, obj = {};
                if (dataType === "attribute") {
                    if (nameIdMap[dataValue.trackedEntityAttribute.id]) {
                        obj = nameIdMap[dataValue.trackedEntityAttribute.id];
                        audit.name = obj.displayName;
                        audit.valueType = obj.valueType;
                    }
                } else if (dataType === "dataElement") {
                    if (nameIdMap[dataValue.dataElement.id] && nameIdMap[dataValue.dataElement.id].dataElement) {
                        obj = nameIdMap[dataValue.dataElement.id].dataElement;
                        audit.name = obj.displayFormName;
                        audit.valueType = obj.valueType;
                    }
                }
                
                dataValue.value = CommonUtils.formatDataValue(null, dataValue.value, obj, optionSets, 'USER');
                audit.auditType = dataValue.auditType;                
                audit.value = dataValue.value;
                audit.modifiedBy = dataValue.modifiedBy;
                audit.created = DateUtils.formatToHrsMinsSecs(dataValue.created);                
                
                $scope.model.itemList.push(audit);
                if( $scope.model.uniqueRows.indexOf(audit.name) === -1){
                	$scope.model.uniqueRows.push(audit.name);
                }
                
                if($scope.model.uniqueRows.length > 0){
                	$scope.model.uniqueRows = $scope.model.uniqueRows.sort();
                }
            }
        }
    });
});
