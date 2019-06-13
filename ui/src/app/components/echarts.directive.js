/*
 * Copyright © 2016-2018 The BeiDouApp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as echarts from 'echarts';

export default angular.module('beidouapp.directives.echarts', [])
    .directive('echarts', echartsAngularjs)
    .name;

/*@ngInject*/
function echartsAngularjs($window) {

    return {
        restrict: 'E',
        template: '<div></div>',
        scope: {
            theme: '@',
            option: '=',
            onInit: "&",
            onChange: "&"
        },
        link: function ($scope, element, attrs) {
            // 配置项
            var DEFAULT_THEME = "default";
            var opts = {
                height: attrs.height || 400,
                width: attrs.width || 'auto'
            };
            var container = element.children()[0] || element[0];

            /**
             * echarts初始化
             * @param {Object} dom		实例容器
             * @param {Object} theme	主题
             * @param {Object} opts		附加参数
             */
            var init = function (dom, theme, opts) {
                $scope.chart = echarts.init(dom, theme || DEFAULT_THEME, opts);
                $scope.onInit() && $scope.onInit()($scope.chart);
            };
            /**
             * 设置图表实例的配置项以及数据
             * @param {Object} option	图表实例的配置项以及数据
             */
            var setOption = function (option) {
                if ($scope.chart && option) {
                    $scope.chart.setOption(option);
                    $scope.onChange() && $scope.onChange()(option);
                }
            }

            // 监听.初始化
            $scope.$watch(function () {
                return $scope.option;
            }, function (option) {
                setOption(option);
            });
            init(container, $scope.theme, opts);

            angular.element($window).bind('resize', function () {
                $scope.chart && $scope.chart.resize();
            });

            // 销毁
            $scope.$on('$destroy', function () {
                if ($scope.chart) {
                    $scope.chart.dispose();
                    $scope.chart = null;
                }
            });
        }
    }
}

/* eslint-disable angular/angularelement */
