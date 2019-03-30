const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');


// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
  res.send('Alarms Api home page');
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About alarms');
})

function generateReturnRules(keysInfo, additionalInfo, res) {
  let retInfo = [];
  if (!additionalInfo) {
    if (keysInfo && keysInfo[0]) {
      let keys = JSON.parse(keysInfo[0].value);

      for (let i = 0; i < keys.length; i++) {
        let _dt = {
          "Key": keys[i].name,
          "IndeterminateRules": {
            "min": "",
            "max": ""
          },
          "WarningRules": {
            "min": "",
            "max": ""
          }
        };
        retInfo.push(_dt);
      }
    }
  }
  else {
    let keys = JSON.parse(keysInfo[0].value);
    for (let i = 0; i < keys.length; i++) {
      let IndeterminateRules = additionalInfo.IndeterminateRules;
      let WarningRules = additionalInfo.WarningRules;

      let _dt = {
        "Key": "",
        "IndeterminateRules": "",
        "WarningRules": ""
      };
      for (let j = 0; j < IndeterminateRules.length; j++) {
        if (keys[i].name == IndeterminateRules[i].Key) {
          _dt.Key = keys[i].name;
          _dt.IndeterminateRules = IndeterminateRules[i].IndeterminateRules;
          break;
        }
      }

      for (let j = 0; j < WarningRules.length; j++) {
        if (keys[i].name == WarningRules[i].Key) {
          _dt.WarningRules = WarningRules[i].WarningRules;
          break;
        }
      }
      retInfo.push(_dt);
    }
  }
  util.responData(util.CST.OK200, retInfo, res);
}

//GET获取指定设备号上绑定的告警规则
router.get('/:id', async function (req, res) {
  var devID = req.params.id;
  let token = req.headers['x-authorization'];

  let api = util.getAPI() + `plugins/telemetry/DEVICE/${devID}/values/attributes/CLIENT_SCOPE`;
  axios.get(api, {
    headers: {
      "X-Authorization": token
    },
    params: {
      keys: "phy_qua"
    }
  }).then(resp => {
    var keysInfo = resp.data;
    if (keysInfo.length == 0)
    {
      util.responData(util.CST.ERR404, util.CST.MSG404, res);
    }
    else{
      let api = util.getAPI() + `device/${devID}`;
      axios.get(api, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        generateReturnRules(keysInfo, resp.data.additionalInfo, res);
      }).catch(err => {
        util.responErrorMsg(err, res);
      })
    }
  }).catch(err => {
    // 由devID查询tenantId出现问题
    // util.responData(util.CST.ERR512, util.CST.MSG512, res);
    util.responErrorMsg(err, res);
  });
})

function updateDeviceInfo(tenantId, data, WarningRules, IndeterminateRules, token, res) {
  let url = util.getAPI() + `device`;
  data.additionalInfo = {
    WarningRules,
    IndeterminateRules
  };
  axios.post(url, (data), {
    headers: { "X-Authorization": token },
    params: { "tenantIdStr": tenantId }
  }).then(resp => {
    //添加addinfo后更新device信息
    util.responData(util.CST.OK200, util.CST.MSG200, res);
  }).catch(err => {
    //通过设备ID获取设备信息失败
    util.responErrorMsg(err, res);
  });
}

//POST更新设备的告警规则，并且更新设备表的addtionalInfo，记录该告警规则
router.post('/:id', async function (req, res) {
  let devID = req.params.id;
  let token = req.headers['x-authorization'];
  let TID = '';
  var params = req.body;

  //接口因权限问题修改，需要根据devID查询tenantId
  let urlGetTenant = util.getAPI() + `device/${devID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    let tenantId = resp.data.tenantId.id;

    let IndeterminateRules = [];
    let WarningRules = [];
    if (params.length > 0) {
      let i = 0;
      for (i = 0; i < params.length; i++) {
        let _dt = {
          "Key": `${params[i].Key}`,
          "IndeterminateRules": {
            "min": `${params[i].IndeterminateRules.min}`,
            "max": `${params[i].IndeterminateRules.max}`
          }
        };
        IndeterminateRules.push(_dt);

        let _dt2 = {
          "Key": `${params[i].Key}`,
          "WarningRules": {
            "min": `${params[i].WarningRules.min}`,
            "max": `${params[i].WarningRules.max}`
          }
        };
        WarningRules.push(_dt2);
      }
    }

    let api = util.getAPI() + `plugins/telemetry/DEVICE/${devID}/attributes/SERVER_SCOPE`;
    axios.post(api,
      //{"alarm_level_1_cfg":JSON.stringify(IndeterminateRules)},
      {
        "alarm_level_1_cfg": JSON.stringify(IndeterminateRules),
        "alarm_level_2_cfg": JSON.stringify(WarningRules)
      },
      {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        if (resp.status == 200) {
          //2.写入设备的addinfo（失败的情况不考虑）
          //IndeterminateRules, WarningRules, devID, TID
          //通过设备ID获取设备信息
          url = util.getAPI() + `device/${devID}`;
          axios.get(url, {
            headers: {
              "X-Authorization": token
            }
          }).then(resp => {
            //添加addinfo后更新device信息
            updateDeviceInfo(tenantId, resp.data, WarningRules, IndeterminateRules, token, res);
          }).catch(err => {
            //通过设备ID获取设备信息失败
            util.responErrorMsg(err, res);
          });
        }
        else {
          logger.log('error', 'FIXME: why should I go here?')
          util.responData(util.CST.ERR404, util.CST.MSG404, res);
        }
      }).catch(err => {
        util.responErrorMsg(err, res);
      });
  }).catch(err => {
    //无法通过设备号devID获取tenantId
    util.responErrorMsg(err, res);
  });
})

module.exports = router