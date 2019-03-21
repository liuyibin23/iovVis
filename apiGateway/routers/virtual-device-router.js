const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');

var virtual_dev_id = "";
var virtual_dev_tok = "";

// define the home page route
router.get('/', function (req, res) {
    res.send('content  Virtual Device home page');
})

// define the about route
router.get('/about', function (req, res) {
    res.send('About Virtual Device')
})

function postMetaData(ruleMeta, token, res) {
    let postApi = util.getAPI() + 'ruleChain/metadata';

    axios.post(postApi, ruleMeta,
        {
            headers: { "X-Authorization": token }
        }).then(resp => {
            let respData = {
                'deviceToken': virtual_dev_tok,
                'mqttBackendBUS': util.getBackendBUS()
            };
            util.responData(util.CST.OK200, respData, res);
        }).catch(err => {
            util.responErrorMsg(err, res);
        });
}

function modifyMetaData(ruleMeta, deviceIdList, token, res) {
    if (ruleMeta) {
        //获取节点
        let nodes = ruleMeta.nodes;
        let js1, index1 = -1;
        let js2, index2 = -1;
        for (let index in nodes) {
            if (nodes[index].name === 'switch by dev_id') {
                //预警规则设置节点
                js1 = nodes[index].configuration.jsScript;
                index1 = index;
            } else if (nodes[index].name === 'EAI_BUS') {
                index2 = index;
            }
        }

        // 找到JS代码 并修改
        if (js1) {
            let jsScript = js1;
            var mapID = new Map();
            let index = jsScript.indexOf('/*device ids array*/');
            eval(jsScript.substr(0, index));
            if (typeof devids !== 'undefined') {
                // ID踢重
                deviceIdList.forEach(element => {
                    mapID.set(element, 1);
                });

                devids.forEach(element => {
                    mapID.set(element, 1);
                });

                var newIDs = new Array();
                mapID.forEach(function (value, key) {
                    newIDs.push(key);
                });

                js1 = "var devids = " + JSON.stringify(newIDs) + ";\n" + js1.substr(index);
                nodes[index1].configuration.jsScript = js1;

                // 修改MQTT服务地址
                if (index2 != -1) {
                    let mqttBus = util.getBackendBUS();
                    let idx = mqttBus.indexOf(':');

                    nodes[index2].configuration.host = mqttBus.substr(0, idx);
                    nodes[index2].configuration.port = Number.parseInt(mqttBus.substr(idx + 1));
                }

                // POST meta data
                postMetaData(ruleMeta, token, res);
            }
        }
    }
}

function updateRuleChain(deviceIdList, token, res) {
    // 获取规则链
    let getRuleChainApi = util.getAPI() + 'ruleChains';
    axios.get(getRuleChainApi,
        {
            headers: { "X-Authorization": token },
            params: {
                textSearch: "MQTT_ENGINE_BUS",
                limit: 1
            }
        }).then(resp => {
            //根据规则链编号获取规则链中存储的meta数据
            let ruleChain = resp.data;
            if (ruleChain) {
                ruleID = ruleChain.data[0].id;
                let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
                axios.get(url, {
                    headers: {
                        "X-Authorization": token
                    }
                }).then(resp => {
                    let ruleMeta = resp.data;
                    modifyMetaData(ruleMeta, deviceIdList, token, res);
                }).catch(err => {
                    util.responErrorMsg(err, res);
                });
            }
        }).catch(err => {
            util.responErrorMsg(err, res);
        });
}

function createVirtualDevice(virtualName, token, res) {
    let api = util.getAPI() + 'device';
    axios.post(api, {
        name: virtualName,
        type: "虚拟设备"
    },
        {
            headers: { "X-Authorization": token }
        }).then(resp => {
            // 根据设备ID查询token
            virtual_dev_id = resp.data.id.id;
            console.log(virtual_dev_id);
            let deviceIdList = [];
            deviceIdList.push(virtual_dev_id);

            configVirtualDevive(virtual_dev_id, deviceIdList, token, res);
        }).catch(err => {
            util.responErrorMsg(err, res);
        });
}

function configVirtualDevive(deviceId, deviceIdList, token, res) {
    let get_credentials = util.getAPI() + `device/${deviceId}/credentials`;
    axios.get(get_credentials,
        {
            headers: { "X-Authorization": token }
        }).then(resp => {
            virtual_dev_tok = resp.data.credentialsId;
            console.log(virtual_dev_tok);
            updateRuleChain(deviceIdList, token, res);
        }).catch(err => {
            util.responErrorMsg(err, res);
        });
}

router.post('/:id', async function (req, res) {
    if (req.baseUrl === '/api/v1/virtualDevice/config') {
        let deviceId = req.params.id;
        try {
            let otherDeviceId = JSON.parse(req.query.otherDeviceId);
            let deviceIdList = [];
            if (typeof(otherDeviceId) === 'string') {
                deviceIdList.push(otherDeviceId);
            }
            else {
                otherDeviceId.forEach(element => {
                    if (typeof(element) === 'number'){
                        deviceIdList.push(element.toString());
                    }
                    else {
                        deviceIdList.push(element);
                    }
                });
            }
            let token = req.headers['x-authorization'];
            configVirtualDevive(deviceId, deviceIdList, token, res);
        } catch (err) {
            util.responErrorMsg(err, res);
        }       
    }
    else {
        let virtualName = req.params.id;
        let token = req.headers['x-authorization'];

        createVirtualDevice(virtualName, token, res);
    }
})


module.exports = router;