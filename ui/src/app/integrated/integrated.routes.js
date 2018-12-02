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
/* eslint-disable import/no-unresolved, import/default */

import IntegratedTemplate from './integrated.tpl.html';
import dashboardTemplate from '../dashboard/dashboard.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function IntegratedRoutes($stateProvider) {
    
    $stateProvider
        .state('home.integrated', {
            url: '/integrated',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: IntegratedTemplate,
                    controllerAs: 'vm',
                    controller: 'IntegratedController'
                }
            },
            data: {
                pageTitle: 'home.integrated'
            },
            ncyBreadcrumb: {
                label: '{"icon": "home", "label": "home.integrated"}',
                icon: 'home'
            }
        })
        .state('home.integrated.dashboard', {
            parent: 'home.integrated',
            url: '/dashboard/:dashboardId?state',
            reloadOnSearch: false,
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],           
            views: {
                'contentDetails@home.integrated': {
                     templateUrl: dashboardTemplate,
                     controller: 'DashboardController',
                     controllerAs: 'vm'
                 }
            },
            data: {
                widgetEditMode: false,
                searchEnabled: false,
                pageTitle: 'dashboard.dashboard'
            },
            ncyBreadcrumb: {
                skip: true
            }
        });
}
