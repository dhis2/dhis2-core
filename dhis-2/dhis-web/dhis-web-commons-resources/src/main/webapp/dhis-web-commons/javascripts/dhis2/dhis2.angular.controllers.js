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
})

.controller('OrgUnitTreeController', function($scope, $modalInstance, OrgUnitFactory, orgUnitId) {

    $scope.model = {selectedOrgUnitId: orgUnitId ? orgUnitId : null};

    function expandOrgUnit( orgUnit, ou ){
        if( ou.path.indexOf( orgUnit.path ) !== -1 ){
            orgUnit.show = true;
        }

        orgUnit.hasChildren = orgUnit.children && orgUnit.children.length > 0 ? true : false;
        if( orgUnit.hasChildren ){
            for( var i=0; i< orgUnit.children.length; i++){
                if( ou.path.indexOf( orgUnit.children[i].path ) !== -1 ){
                    orgUnit.children[i].show = true;
                    expandOrgUnit( orgUnit.children[i], ou );
                }
            }
        }
        return orgUnit;
    };

    function attachOrgUnit( orgUnits, orgUnit ){
        for( var i=0; i< orgUnits.length; i++){
            if( orgUnits[i].id === orgUnit.id ){
                orgUnits[i] = orgUnit;
                orgUnits[i].show = true;
                orgUnits[i].hasChildren = orgUnits[i].children && orgUnits[i].children.length > 0 ? true : false;
                return;
            }
            if( orgUnits[i].children && orgUnits[i].children.length > 0 ){
                attachOrgUnit(orgUnits[i].children, orgUnit);
            }
        }
        return orgUnits;
    };

    //Get orgunits for the logged in user
    OrgUnitFactory.getViewTreeRoot().then(function(response) {
        $scope.orgUnits = response.organisationUnits;
        var selectedOuFetched = false;
        var levelsFetched = 0;
        angular.forEach($scope.orgUnits, function(ou){
            ou.show = true;
            levelsFetched = ou.level;
            if( orgUnitId && orgUnitId === ou.id ){
                selectedOuFetched = true;
            }
            angular.forEach(ou.children, function(o){
                levelsFetched = o.level;
                o.hasChildren = o.children && o.children.length > 0 ? true : false;
                if( orgUnitId && !selectedOuFetched && orgUnitId === ou.id ){
                    selectedOuFetched = true;
                }
            });
        });

        levelsFetched = levelsFetched > 0 ? levelsFetched - 1 : levelsFetched;

        if( orgUnitId && !selectedOuFetched ){
            var parents = null;
            OrgUnitFactory.get( orgUnitId ).then(function( ou ){
                if( ou && ou.path ){
                    parents = ou.path.substring(1, ou.path.length);
                    parents = parents.split("/");
                    if( parents && parents.length > 0 ){
                        var url = "fields=id,displayName,path,level,";
                        for( var i=levelsFetched; i<ou.level; i++){
                            url = url + "children[id,displayName,level,path,";
                        }

                        url = url.substring(0, url.length-1);
                        for( var i=levelsFetched; i<ou.level; i++){
                            url = url + "]";
                        }

                        OrgUnitFactory.getOrgUnits(parents[levelsFetched], url).then(function(response){
                            if( response && response.organisationUnits && response.organisationUnits[0] ){
                                response.organisationUnits[0].show = true;
                                response.organisationUnits[0].hasChildren = response.organisationUnits[0].children && response.organisationUnits[0].children.length > 0 ? true : false;
                                response.organisationUnits[0] = expandOrgUnit(response.organisationUnits[0], ou );
                                $scope.orgUnits = attachOrgUnit( $scope.orgUnits, response.organisationUnits[0] );
                            }
                        });
                    }
                }
            });
        }
    });


    //expand/collapse of search orgunit tree
    $scope.expandCollapse = function(orgUnit) {
        if( orgUnit.hasChildren ){
            //Get children for the selected orgUnit
            OrgUnitFactory.getChildren(orgUnit.id).then(function(ou) {
                orgUnit.show = !orgUnit.show;
                orgUnit.hasChildren = false;
                orgUnit.children = ou.children;
                angular.forEach(orgUnit.children, function(ou){
                    ou.hasChildren = ou.children && ou.children.length > 0 ? true : false;
                });
            });
        }
        else{
            orgUnit.show = !orgUnit.show;
        }
    };

    $scope.setSelectedOrgUnit = function( orgUnitId ){
        $scope.model.selectedOrgUnitId = orgUnitId;
    };

    $scope.select = function () {
        $modalInstance.close( $scope.model.selectedOrgUnitId );
    };

    $scope.close = function(){
        $modalInstance.close();
    };
});
