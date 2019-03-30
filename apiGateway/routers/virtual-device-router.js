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

async function configMQTT(deviceId, deviceIdList, token, res){
    var failCnt    = 0;
    var sucess_cnt = 0;

    for (let i = 0; i < deviceIdList.length; i++){
        let devID = deviceIdList[i];
        let api = util.getAPI() + `plugins/telemetry/DEVICE/${devID}/attributes/SERVER_SCOPE`;

        await axios.post(api,
        {
            "is_mqtt_trans": true
        },
        {
            headers: {
            "X-Authorization": token
            }
        }).then(resp => {
            sucess_cnt++;
        }).catch(err =>{
            failCnt++;
            console.log(err);
           });    
    }

    if (sucess_cnt == deviceIdList.length){
        util.responData(util.CST.OK200, util.CST.MSG200, res);
    }
    else {
        util.responData(util.CST.OK200, `成功${sucess_cnt}个设备, 失败${failCnt}个设备`, res);
    }
}

function configVirtualDevive(deviceId, deviceIdList, token, res) {
    let get_credentials = util.getAPI() + `device/${deviceId}/credentials`;
    axios.get(get_credentials,
        {
            headers: { "X-Authorization": token }
        }).then(resp => {
            virtual_dev_tok = resp.data.credentialsId;
            console.log(virtual_dev_tok);

            let _dt = {'deviceToken': virtual_dev_tok};

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
            configMQTT(deviceId, deviceIdList, token, res);
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