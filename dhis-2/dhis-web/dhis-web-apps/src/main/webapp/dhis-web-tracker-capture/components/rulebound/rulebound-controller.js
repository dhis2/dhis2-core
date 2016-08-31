trackerCapture.controller('RuleBoundController',
        function(
                $rootScope,
                $scope,
                $translate,
                $log) {
    
    $scope.widget = $scope.$parent.$parent.biggerWidget ? $scope.$parent.$parent.biggerWidget
    : $scope.$parent.$parent.smallerWidget ? $scope.$parent.$parent.smallerWidget : null;
    $scope.widgetTitle = $scope.widget.title;    
    $scope.emptyFeedbackListLabel = $translate.instant('no_feedback_exist');
    $scope.emptyIndicatorListLabel = $translate.instant('no_indicators_exist');
    
    $scope.widgetTitleLabel = $translate.instant($scope.widgetTitle);
    
    $scope.displayTextEffects = {};
    $scope.displayKeyDataEffects = {};
    
    //listen for updated rule effects
    $scope.$on('ruleeffectsupdated', function(event, args) {
        var textInEffect = false;
        var keyDataInEffect = false;
        
        //Bind non-bound rule effects, if any.
        angular.forEach($rootScope.ruleeffects[args.event], function(effect) {
            if(effect.location === $scope.widgetTitle){
                //This effect is affecting the local widget
                
                if(effect.action === "DISPLAYTEXT") {
                    //this action is display text. Make sure the displaytext is
                    //added to the local list of displayed texts
                    if(!angular.isObject($scope.displayTextEffects[effect.id])){
                        $scope.displayTextEffects[effect.id] = effect;
                    }
                    if(effect.ineffect)
                    {
                        textInEffect = true;
                    }
                }
                else if(effect.action === "DISPLAYKEYVALUEPAIR") {
                    //this action is display text. Make sure the displaytext is
                    //added to the local list of displayed texts
                    if(!angular.isObject($scope.displayTextEffects[effect.id])){
                        $scope.displayKeyDataEffects[effect.id] = effect;
                    }
                    if(effect.ineffect)
                    {
                        keyDataInEffect = true;
                    }
                }
                else if(effect.action === "ASSIGNVARIABLE") {
                    //the dataentry control saves the variable and or dataelement
                }
                else {
                    $log.warn("action: '" + effect.action + "' not supported by rulebound-controller.js");
                }
            }
        });
        
        $scope.showKeyDataSection = keyDataInEffect;
        $scope.showTextSection = textInEffect;        
    });     
});
