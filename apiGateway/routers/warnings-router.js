const express = require('express');
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');
const qs = require('qs')
const router = express.Router();

// define warnings route
router.get('/:assetId', async function (req, res) {
  if (req.baseUrl === '/api/v1/rules/warnings') {
    getWarningRules(req, res);
  }
  else if (req.baseUrl === '/api/v1/warnings') {
    getWarningStatus(req, res);
  }
})
router.post('/:assetId', async function (req, res) {
  if (req.baseUrl === '/api/v1/rules/warnings') {
    postWarningRules(req, res);
  }
  else if (req.baseUrl === '/api/v1/warnings') {
    postWarningStatus(req, res);
  }
})

//获取预警状态
async function getWarningStatus(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;
  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    let resMsg = {
      "code": `${resp.status}`
    };
    resp.data.forEach(info => {
      if (info.key === 'asset_warning_level') {
        // info.value = JSON.parse(info.value);
        // res.status(200).json(info);
        resMsg.info = info;
      }
    });
    if (resMsg.info) {
      let asset_warning_level = resMsg.info.value;
      util.responData(resp.status, { asset_warning_level }, res);
    } else {
      let data = [];
      util.responData(util.CST.OK200, data, res);
    }
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
}

function addWarningOperatorRecord(assetID, warningsInfo, warningsType, token, res){
  let api = util.getAPI() + 'currentUser/setWarningEventRecord';

  let params = {
    warningsInfo:JSON.stringify(warningsInfo),
    warningsType:warningsType,
    assetIdStr:assetID
  };

  axios.post(api, qs.stringify(params), {
    headers: { "X-Authorization": token }
  }).then(resp => {
    //返回成功
    util.responData(util.CST.OK200, util.CST.MSG200, res);
  }).catch(err => {
    //通过资产ID获取信息失败
    util.responErrorMsg(err, res);
  });
}

// 添加additionalInfo 属性
async function postAsset(assetID, token, args, res) {
  // GET
  let getapi = util.getAPI() + `assets?assetIds=${assetID}`;
  await axios.get(getapi, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      let pdata = resp.data[0];
      pdata.additionalInfo = args;

      let post_api = util.getAPI() + `asset?tenantIdStr=${pdata.id.id}`;
      axios.post(post_api, pdata, {
        headers: {
          "X-Authorization": token
        }
      }).then((resp) => {
        // 添加预警操作记录
        addWarningOperatorRecord(assetID, args, '手动设置资产预警状态', token, res);
      }).catch((err) => {
        util.responErrorMsg(err, res);
      });
    }).catch((err) => {
      util.responErrorMsg(err, res);
    });
}

//设置预警状态
async function postWarningStatus(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;

  axios.post(get_attributes_api, req.query, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      // post asset
      postAsset(assetID, token, req.query, res);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
}

//从规则引擎获取资产的预警规则
async function getWarningRules(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  // let TID = '';

  //接口因权限问题修改，需要根据assetID查询tenantId
  let urlGetTenant = util.getAPI() + `asset/${assetID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    let url = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;
    let tenantId = resp.data.tenantId.id;
 
    //获取规则
    axios.get(url, {
      headers: {
        "X-Authorization": token
      },
      params: {
        keys: "warning_rule_cfg"
      }
    }).then(resp => {
      let dataValid = false;
      let rules = resp.data;
      let data = [];
      if (rules[0] && rules[0].value) {
        data = JSON.parse(rules[0].value);
        if (data){
          dataValid = true;
          util.responData(util.CST.OK200, data, res);
        }    
      } 
      
      if (!dataValid){
        util.responData(util.CST.OK200, data, res);
      }
    }).catch(err => {
      //规则链获取出现问题
      logger.log('error', 'Cannot get rulechain.');
      util.responErrorMsg(err, res);
    });
  }).catch(err => {
    // 由资产号查询tenantId出现问题
    logger.log('error', 'Cannot get tenantId by assetId.');
    util.responErrorMsg(err, res);
  });
}

function updateAssetInfo(assetID, WarningRule, tenantId, token, req, res){
  let url = util.getAPI() + `asset/${assetID}`;
  axios.get(url, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    //添加addinfo后更新资产信息
    url = util.getAPI() + `asset`;
    let data = resp.data;
    data.additionalInfo = {
      WarningRule
    };
    axios.post(url, (data), {
      headers: { "X-Authorization": token },
      params: { "tenantIdStr": tenantId }
    }).then(resp => {
      // 添加预警操作记录
      addWarningOperatorRecord(assetID, req.body, '手动设置资产预警规则', token, res);
    })
  }).catch(err => {
    //通过资产ID获取信息失败
    util.responErrorMsg(err, res);
  });
}

//POST更新规则引擎中指定资产的预警规则，并把该规则存入资产表的addtionalInfo
async function postWarningRules(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let WarningRule = req.body;
  let TID = '';

  //接口因权限问题修改，需要根据devID查询tenantId
  let urlGetTenant = util.getAPI() + `asset/${assetID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    let tenantId = resp.data.tenantId.id;
    let api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;
    axios.post(api,
      {
      "warning_rule_cfg":JSON.stringify(WarningRule)
      },
      {
      headers: {
        "X-Authorization": token
      }
      }).then(resp => {
        if (resp.status == 200) {
          //2.写入资产的addinfo（失败的情况不考虑）
          //WarningRule, assetID, TID
          //通过资产ID获取资产信息
          updateAssetInfo(assetID, WarningRule, tenantId, token, req, res);
        }
        else {
          //
          logger.log('error', 'FIXME: why should I go here?')
          util.responData(util.CST.ERR404, util.CST.MSG404, res);
        }
      }).catch(err =>{
        util.responErrorMsg(err, res);
      })
  }).catch(err => {
    //无法通过设备号devID获取tenantId
    util.responErrorMsg(err, res);
  });
}

module.exports = router