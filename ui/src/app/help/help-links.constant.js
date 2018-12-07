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

var ruleNodeClazzHelpLinkMap = {
    'com.beidouapp.rule.engine.filter.TbCheckRelationNode': 'ruleNodeCheckRelation',
    'com.beidouapp.rule.engine.filter.TbJsFilterNode': 'ruleNodeJsFilter',
    'com.beidouapp.rule.engine.filter.TbJsSwitchNode': 'ruleNodeJsSwitch',
    'com.beidouapp.rule.engine.filter.TbMsgTypeFilterNode': 'ruleNodeMessageTypeFilter',
    'com.beidouapp.rule.engine.filter.TbMsgTypeSwitchNode': 'ruleNodeMessageTypeSwitch',
    'com.beidouapp.rule.engine.filter.TbOriginatorTypeFilterNode': 'ruleNodeOriginatorTypeFilter',
    'com.beidouapp.rule.engine.filter.TbOriginatorTypeSwitchNode': 'ruleNodeOriginatorTypeSwitch',
    'com.beidouapp.rule.engine.metadata.TbGetAttributesNode': 'ruleNodeOriginatorAttributes',
    'com.beidouapp.rule.engine.metadata.TbGetOriginatorFieldsNode': 'ruleNodeOriginatorFields',
    'com.beidouapp.rule.engine.metadata.TbGetCustomerAttributeNode': 'ruleNodeCustomerAttributes',
    'com.beidouapp.rule.engine.metadata.TbGetDeviceAttrNode': 'ruleNodeDeviceAttributes',
    'com.beidouapp.rule.engine.metadata.TbGetRelatedAttributeNode': 'ruleNodeRelatedAttributes',
    'com.beidouapp.rule.engine.metadata.TbGetTenantAttributeNode': 'ruleNodeTenantAttributes',
    'com.beidouapp.rule.engine.transform.TbChangeOriginatorNode': 'ruleNodeChangeOriginator',
    'com.beidouapp.rule.engine.transform.TbTransformMsgNode': 'ruleNodeTransformMsg',
    'com.beidouapp.rule.engine.mail.TbMsgToEmailNode': 'ruleNodeMsgToEmail',
    'com.beidouapp.rule.engine.action.TbClearAlarmNode': 'ruleNodeClearAlarm',
    'com.beidouapp.rule.engine.action.TbCreateAlarmNode': 'ruleNodeCreateAlarm',
    'com.beidouapp.rule.engine.delay.TbMsgDelayNode': 'ruleNodeMsgDelay',
    'com.beidouapp.rule.engine.debug.TbMsgGeneratorNode': 'ruleNodeMsgGenerator',
    'com.beidouapp.rule.engine.action.TbLogNode': 'ruleNodeLog',
    'com.beidouapp.rule.engine.rpc.TbSendRPCReplyNode': 'ruleNodeRpcCallReply',
    'com.beidouapp.rule.engine.rpc.TbSendRPCRequestNode': 'ruleNodeRpcCallRequest',
    'com.beidouapp.rule.engine.telemetry.TbMsgAttributesNode': 'ruleNodeSaveAttributes',
    'com.beidouapp.rule.engine.telemetry.TbMsgTimeseriesNode': 'ruleNodeSaveTimeseries',
    'tb.internal.RuleChain': 'ruleNodeRuleChain',
    'com.beidouapp.rule.engine.aws.sns.TbSnsNode': 'ruleNodeAwsSns',
    'com.beidouapp.rule.engine.aws.sqs.TbSqsNode': 'ruleNodeAwsSqs',
    'com.beidouapp.rule.engine.kafka.TbKafkaNode': 'ruleNodeKafka',
    'com.beidouapp.rule.engine.mqtt.TbMqttNode': 'ruleNodeMqtt',
    'com.beidouapp.rule.engine.rabbitmq.TbRabbitMqNode': 'ruleNodeRabbitMq',
    'com.beidouapp.rule.engine.rest.TbRestApiCallNode': 'ruleNodeRestApiCall',
    'com.beidouapp.rule.engine.mail.TbSendEmailNode': 'ruleNodeSendEmail'
};

var helpBaseUrl = "http://www.beidouapp.com";

export default angular.module('beidouapp.help', [])
    .constant('helpLinks',
        {
            linksMap: {
                outgoingMailSettings: helpBaseUrl + "/docs/user-guide/ui/mail-settings",
                ruleEngine: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/overview/",
                ruleNodeCheckRelation: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#check-relation-filter-node",
                ruleNodeJsFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node",
                ruleNodeJsSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#switch-node",
                ruleNodeMessageTypeFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node",
                ruleNodeMessageTypeSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node",
                ruleNodeOriginatorTypeFilter: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-filter-node",
                ruleNodeOriginatorTypeSwitch: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node",
                ruleNodeOriginatorAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes",
                ruleNodeOriginatorFields: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-fields",
                ruleNodeCustomerAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes",
                ruleNodeDeviceAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes",
                ruleNodeRelatedAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes",
                ruleNodeTenantAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes",
                ruleNodeChangeOriginator: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#change-originator",
                ruleNodeTransformMsg: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node",
                ruleNodeMsgToEmail: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node",
                ruleNodeClearAlarm: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node",
                ruleNodeCreateAlarm: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node",
                ruleNodeMsgDelay: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#delay-node",
                ruleNodeMsgGenerator: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#generator-node",
                ruleNodeLog: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#log-node",
                ruleNodeRpcCallReply: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node",
                ruleNodeRpcCallRequest: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node",
                ruleNodeSaveAttributes: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node",
                ruleNodeSaveTimeseries: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node",
                ruleNodeRuleChain: helpBaseUrl + "/docs/user-guide/ui/rule-chains/",
                ruleNodeAwsSns: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node",
                ruleNodeAwsSqs: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node",
                ruleNodeKafka: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#kafka-node",
                ruleNodeMqtt: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#mqtt-node",
                ruleNodeRabbitMq: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node",
                ruleNodeRestApiCall: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node",
                ruleNodeSendEmail: helpBaseUrl + "/docs/user-guide/rule-engine-2-0/external-nodes/#send-email-node",
                rulechains: helpBaseUrl + "/docs/user-guide/ui/rule-chains/",
                tenants: helpBaseUrl + "/docs/user-guide/ui/tenants",
                customers: helpBaseUrl + "/docs/user-guide/ui/customers",
                assets: helpBaseUrl + "/docs/user-guide/ui/assets",
                devices: helpBaseUrl + "/docs/user-guide/ui/devices",
                entityViews: helpBaseUrl + "/docs/user-guide/ui/entity-views",
                dashboards: helpBaseUrl + "/docs/user-guide/ui/dashboards",
                users: helpBaseUrl + "/docs/user-guide/ui/users",
                widgetsBundles: helpBaseUrl + "/docs/user-guide/ui/widget-library#bundles",
                widgetsConfig:  helpBaseUrl + "/docs/user-guide/ui/dashboards#widget-configuration",
                widgetsConfigTimeseries:  helpBaseUrl + "/docs/user-guide/ui/dashboards#timeseries",
                widgetsConfigLatest: helpBaseUrl +  "/docs/user-guide/ui/dashboards#latest",
                widgetsConfigRpc: helpBaseUrl +  "/docs/user-guide/ui/dashboards#rpc",
                widgetsConfigAlarm: helpBaseUrl +  "/docs/user-guide/ui/dashboards#alarm",
                widgetsConfigStatic: helpBaseUrl +  "/docs/user-guide/ui/dashboards#static",
            },
            getRuleNodeLink: function(ruleNode) {
                if (ruleNode && ruleNode.component) {
                    if (ruleNode.component.configurationDescriptor &&
                        ruleNode.component.configurationDescriptor.nodeDefinition &&
                        ruleNode.component.configurationDescriptor.nodeDefinition.docUrl) {
                        return ruleNode.component.configurationDescriptor.nodeDefinition.docUrl;
                    } else if (ruleNode.component.clazz) {
                        if (ruleNodeClazzHelpLinkMap[ruleNode.component.clazz]) {
                            return ruleNodeClazzHelpLinkMap[ruleNode.component.clazz];
                        }
                    }
                }
                return 'ruleEngine';
            }
        }
    ).name;
