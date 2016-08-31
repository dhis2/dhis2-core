trackerCapture.controller('MakeReferralController', function($scope, $modalInstance, stage, OrgUnitFactory){
    $scope.stage = stage;
    $scope.cancel = function(){
        $modalInstance.close();
    };
    
    $scope.makeReferral = function(){
        if(!$scope.referralDate){
            $scope.dateError = true;
        }else{
            $scope.dateError = false;
        }
        
        if(!$scope.selectedSearchingOrgUnit){
            $scope.orgError = true;     
        }else{
            $scope.orgError = false;
        }
        if(!$scope.dateError && !$scope.orgError){
            $modalInstance.close();
        }
    };
    
    $scope.setSelectedSearchingOrgUnit = function(orgUnit){
        $scope.selectedSearchingOrgUnit = orgUnit;     
    };
    
    OrgUnitFactory.getSearchTreeRoot().then(function(response) {
        $scope.orgUnits = response.organisationUnits;
        angular.forEach($scope.orgUnits, function(ou){
            ou.show = true;
            angular.forEach(ou.children, function(o){                    
                o.hasChildren = o.children && o.children.length > 0 ? true : false;
            });            
        });
    });
    
    $scope.expandCollapse = function(orgUnit) {
        if( orgUnit.hasChildren ){            
            //Get children for the selected orgUnit
            OrgUnitFactory.get(orgUnit.id).then(function(ou) {                
                orgUnit.show = !orgUnit.show;
                orgUnit.hasChildren = false;
                orgUnit.children = ou.organisationUnits[0].children;                
                angular.forEach(orgUnit.children, function(ou){                    
                    ou.hasChildren = ou.children && ou.children.length > 0 ? true : false;
                });                
            });           
        }
        else{
            orgUnit.show = !orgUnit.show;   
        }        
    };
})