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
import './dashboard.scss';

import uiRouter from 'angular-ui-router';

import beidouappGrid from '../components/grid.directive';
import beidouappApiWidget from '../api/widget.service';
import beidouappApiUser from '../api/user.service';
import beidouappApiDashboard from '../api/dashboard.service';
import beidouappApiCustomer from '../api/customer.service';
import beidouappDetailsSidenav from '../components/details-sidenav.directive';
import beidouappWidgetConfig from '../components/widget/widget-config.directive';
import beidouappDashboardSelect from '../components/dashboard-select.directive';
import beidouappRelatedEntityAutocomplete from '../components/related-entity-autocomplete.directive';
import beidouappDashboard from '../components/dashboard.directive';
import beidouappExpandFullscreen from '../components/expand-fullscreen.directive';
import beidouappWidgetsBundleSelect from '../components/widgets-bundle-select.directive';
import beidouappSocialsharePanel from '../components/socialshare-panel.directive';
import beidouappTypes from '../common/types.constant';
import beidouappItemBuffer from '../services/item-buffer.service';
import beidouappImportExport from '../import-export';
import dashboardLayouts from './layouts';
import dashboardStates from './states';

import DashboardRoutes from './dashboard.routes';
import {DashboardsController, DashboardCardController, MakeDashboardPublicDialogController} from './dashboards.controller';
import DashboardController from './dashboard.controller';
import DashboardSettingsController from './dashboard-settings.controller';
import AddDashboardsToCustomerController from './add-dashboards-to-customer.controller';
import ManageAssignedCustomersController from './manage-assigned-customers.controller';
import AddWidgetController from './add-widget.controller';
import DashboardDirective from './dashboard.directive';
import EditWidgetDirective from './edit-widget.directive';
import DashboardToolbar from './dashboard-toolbar.directive';

export default angular.module('beidouapp.dashboard', [
    uiRouter,
    beidouappTypes,
    beidouappItemBuffer,
    beidouappImportExport,
    beidouappGrid,
    beidouappApiWidget,
    beidouappApiUser,
    beidouappApiDashboard,
    beidouappApiCustomer,
    beidouappDetailsSidenav,
    beidouappWidgetConfig,
    beidouappDashboardSelect,
    beidouappRelatedEntityAutocomplete,
    beidouappDashboard,
    beidouappExpandFullscreen,
    beidouappWidgetsBundleSelect,
    beidouappSocialsharePanel,
    dashboardLayouts,
    dashboardStates
])
    .config(DashboardRoutes)
    .controller('DashboardsController', DashboardsController)
    .controller('DashboardCardController', DashboardCardController)
    .controller('MakeDashboardPublicDialogController', MakeDashboardPublicDialogController)
    .controller('DashboardController', DashboardController)
    .controller('DashboardSettingsController', DashboardSettingsController)
    .controller('AddDashboardsToCustomerController', AddDashboardsToCustomerController)
    .controller('ManageAssignedCustomersController', ManageAssignedCustomersController)
    .controller('AddWidgetController', AddWidgetController)
    .directive('tbDashboardDetails', DashboardDirective)
    .directive('tbEditWidget', EditWidgetDirective)
    .directive('tbDashboardToolbar', DashboardToolbar)
    .name;
