/* global trackerCapture, angular */

trackerCapture.controller('ProfileController',
        function($rootScope,
                $scope,
                $timeout,
                CurrentSelection) {    
    
    $scope.editingDisabled = true;
    $scope.enrollmentEditing = false;
    $scope.widget = 'PROFILE';
    
    //listen for the selected entity
    var selections = {};
    $scope.$on('dashboardWidgets', function(event, args) {        
        listenToBroadCast();
    });
    
    //listen to changes in profile
    $scope.$on('profileWidget', function(event, args){
        listenToBroadCast();
    });
    
    //listen to changes in enrollment editing
    $scope.$on('enrollmentEditing', function(event, args){
        $scope.enrollmentEditing = args.enrollmentEditing;
    });
    
    var listenToBroadCast = function(){     
        $scope.editingDisabled = true;
        selections = CurrentSelection.get();
        $scope.selectedTei = angular.copy(selections.tei);
        $scope.trackedEntity = selections.te;
        $scope.selectedProgram = selections.pr;   
        $scope.selectedEnrollment = selections.selectedEnrollment;
        $scope.optionSets = selections.optionSets;
        $scope.trackedEntityForm = null;
        $scope.customForm = null;
        $scope.attributes = [];
        $scope.attributesById = CurrentSelection.getAttributesById();
        
        //display only those attributes that belong to the selected program
        //if no program, display attributesInNoProgram        
        angular.forEach($scope.selectedTei.attributes, function(att){
            $scope.selectedTei[att.attribute] = att.value;
        });
        
        delete $scope.selectedTei.attributes;

        $timeout(function() { 
            $rootScope.$broadcast('registrationWidget', {registrationMode: 'PROFILE', selectedTei: $scope.selectedTei, enrollment: $scope.selectedEnrollment});
        }, 200);
    };
    
    $scope.enableEdit = function(){
        $scope.teiOriginal = angular.copy($scope.selectedTei);
        $scope.editingDisabled = !$scope.editingDisabled; 
        $rootScope.profileWidget.expand = true;
    };
    
    $scope.cancel = function(){
        $scope.selectedTei = $scope.teiOriginal;  
        $scope.editingDisabled = !$scope.editingDisabled;
        $timeout(function() { 
            $rootScope.$broadcast('registrationWidget', {registrationMode: 'PROFILE', selectedTei: $scope.selectedTei, enrollment: $scope.selectedEnrollment});
        }, 200);
    };  
});