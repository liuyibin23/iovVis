const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');

var virtual_dev_id = "";
var virtual_dev_tok = "";

const MQTT_TYPE_ADD = 2;       // 加
const MQTT_TYPE_DEL = 3;       // 减

var sucess_cnt = 0;
var failed_cnt = 0;
var total_cnt  = 0;

// define the home page route
router.get('/', function (req, res) {
    res.send('content  Virtual Device home page');
})

// define the about route
router.get('/about', function (req, res) {
    res.send('About Virtual Device')
})

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

function sendResponse(res){
    if ((sucess_cnt + failed_cnt) == total_cnt)
    {
        if (sucess_cnt == total_cnt) {
            util.responData(util.CST.OK200, util.CST.MSG200, res);
        }
        else {
            util.responData(util.CST.OK200, `成功${sucess_cnt}个设备, 失败${failed_cnt}个设备`, res);
        }
    }
}

async function updateMQTTValue(deviceId, value, token, res) {
    console.log(`Update: ${deviceId} Value:${value}`);

    let api = util.getAPI() + `plugins/telemetry/DEVICE/${deviceId}/attributes/SERVER_SCOPE`;
    await axios.post(api,
        {
            "is_mqtt_trans": value
        },
        {
            headers: {
            "X-Authorization": token
            },
            timeout: 5000
        }).then(resp => {
            sucess_cnt++;
            console.log(`ID=${deviceId} sucess=${sucess_cnt}`);
            sendResponse(res);
        }).catch(err => {
            failed_cnt++;
            console.log(`failed_cnt=${failed_cnt}`);
            sendResponse(res);
        })
}

function getOldAndConfigMQTTValue(type, deviceId, token, res) {
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${deviceId}/values/attributes/SERVER_SCOPE`;
    axios.get(api,
    {
        headers: {
            "X-Authorization": token
        },
        timeout: 5000,
        params: {
            keys: "is_mqtt_trans"
        }
    }).then(resp => {
        let mqtt_cfg_value = 0;
        if (resp.data[0] && resp.data[0].value)
            mqtt_cfg_value = resp.data[0].value;

        if (type == MQTT_TYPE_ADD) {
            if (mqtt_cfg_value == true) {
                mqtt_cfg_value = 1;
            }
            mqtt_cfg_value += 1;
        } else {
            if (mqtt_cfg_value == true) {
                mqtt_cfg_value = 1;
            }
            mqtt_cfg_value -= 1;
            if (mqtt_cfg_value < 0) {
                mqtt_cfg_value = 0;
            }
        }

        updateMQTTValue(deviceId, mqtt_cfg_value, token, res);
    }).catch(err => {
        failed_cnt++;
        console.log(err);

        console.log(`getOldAndConfigMQTTValue failed_cnt=${failed_cnt}`);
        sendResponse(res);
    });
}

async function configMQTTValue(addDevList, delDevList, token, res) {
    total_cnt =  addDevList.length + delDevList.length;

    console.log(`configMQTTValue==> add:${addDevList.length} del=${delDevList.length}`);

    // Add
    for (let i = 0; i < addDevList.length; i++) {
        let devID = addDevList[i];
        getOldAndConfigMQTTValue(MQTT_TYPE_ADD, devID, token, res);
    }

    // Del
    for (let i = 0; i < delDevList.length; i++) {
        let devID = delDevList[i];
        getOldAndConfigMQTTValue(MQTT_TYPE_DEL, devID, token, res);
    }
}

function ProcessDevList(oldDevList, newDevList, token, res) {
    // 查找新增的
    let addDevList = [];
    let delDevList = [];

    if (oldDevList.length == 0) {
        addDevList = newDevList;
    } else {
        // 查找哪些是新增的设备
        for (let i = 0; i < newDevList.length; i++) {
            let find = false;
            for (j = 0; j < oldDevList.length; j++) {
                if (newDevList[i] === oldDevList[j]) {
                    find = true;
                }
            }

            if (!find) {
                addDevList.push(newDevList[i]);
            }
        }

        // 查找删除的设备
        for (let i = 0; i < oldDevList.length; i++) {
            let find = false;
            for (j = 0; j < newDevList.length; j++) {
                if (oldDevList[i] == newDevList[j]) {
                    find = true;
                }
            }
            if (!find) {
                delDevList.push(oldDevList[i]);
            } else {
                addDevList.push(oldDevList[i]);
            }
        }        
    }

    // 配置新值
    configMQTTValue(addDevList, delDevList, token, res);
}

function updateDeviceList(deviceId, oldDevList, otherDeviceId, token, req, res){
    sucess_cnt = 0;
    failed_cnt = 0;
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${deviceId}/attributes/SERVER_SCOPE`;
    axios.post(api,
    {
        "other_device_id": req.query.otherDeviceId,
    },
    {
        headers: {
            "X-Authorization": token
        }
    }).then(resp => {
        let deviceIdList = [];
        if (typeof (otherDeviceId) === 'string') {
            deviceIdList.push(otherDeviceId);
        }
        else {
            otherDeviceId.forEach(element => {
                if (typeof (element) === 'number') {
                    deviceIdList.push(element.toString());
                }
                else {
                    deviceIdList.push(element);
                }
            });
        }

        // 比较旧列表和新列表
        ProcessDevList(oldDevList, deviceIdList, token, res);
    }).catch(err => {
        util.responErrorMsg(err, res);
    }); 
}

function GetOtherDevList(deviceId, otherDeviceId, token, req, res) {
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${deviceId}/values/attributes/SERVER_SCOPE`;
    axios.get(api,
        {
            headers: {
                "X-Authorization": token
            },
            params: {
                keys: "other_device_id"
            }
        }).then(resp => {
            let oldDevList = [];
            if (resp.data[0] && resp.data[0].value) {
                try{
                    oldDevList = JSON.parse(resp.data[0].value);
                    
                }catch(err) {
                    util.responErrorMsg(err, res);
                    return;
                }
            }

            // 更新新的设备列表
            updateDeviceList(deviceId, oldDevList, otherDeviceId, token, req, res);
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

            let _dt = { 'deviceToken': virtual_dev_tok };

            util.responData(util.CST.OK200, _dt, res);
        }).catch(err => {
            util.responErrorMsg(err, res);
        });
}

router.post('/:id', async function (req, res) {
    if (req.baseUrl === '/api/v1/virtualDevice/config') {
        let deviceId = req.params.id;
        try {
            let otherDeviceId = JSON.parse(req.query.otherDeviceId);
            if (otherDeviceId && otherDeviceId.length > 0 && typeof(otherDeviceId) === "object") {
                let token = req.headers['x-authorization'];
                // 获取旧配置列表
                GetOtherDevList(deviceId, otherDeviceId, token, req, res);
            }
            else {
                util.responData(util.CST.ERR400, util.CST.MSG400, res);
            }
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