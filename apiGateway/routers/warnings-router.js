const express = require('express');
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');
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
      util.responData(resp.status, {asset_warning_level}, res);
    } else
      util.responData(util.CST.ERR404, util.CST.MSG404, res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
}
//设置预警状态
async function postWarningStatus(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/attributes/SERVER_SCOPE`;

  axios.post(get_attributes_api, req.query, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      util.responData(util.CST.OK200, util.CST.MSG200, res);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
}

//获取规则引擎
async function getWarningRules(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let TID = '';

  //接口因权限问题修改，需要根据devID查询tenantId
  let urlGetTenant = util.getAPI() + `asset/${assetID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    let url = util.getAPI() + `beidouapp/ruleChains`;
    let tenantId = resp.data.tenantId.id;
    let TID = tenantId;
    //获取规则链
    axios.get(url, {
      headers: {
        "X-Authorization": token
      },
      params: {
        textSearch: "CONFIG_WARNING_RULE",
        limit: 1,
        tenantIdStr: tenantId
      }
    }).then(resp => {
      var ruleChain = resp.data;
      if (ruleChain) {
        ruleID = ruleChain[0].id;
        let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
        //获取预警规则链的meta数据
        axios.get(url, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          let nodes = resp.data.nodes;
          //获取预警规则
          let js1 = '';
          for (let node of nodes) {
            if (node.name === util.CFG.WARN_NODE_RULE)
              js1 = node.configuration.jsScript;
          }
          if (js1 !== '') {
            let jsScript = js1;
            var index = jsScript.indexOf('/* warning rule tables */');
            eval(jsScript.substr(0, index));
            if (typeof ruleTables !== 'undefined') {
              let rules = ruleTables[assetID];
              if (rules) {
                util.responData(util.CST.OK200, rules, res);
              }
              else {
                util.responData(util.CST.ERR404, util.CST.MSG404, res);
              }
              delete ruleTables; // clear the enviroment variable, only for eval case.
            }
          } else {
            //没找到指定名称的规则节点
            util.responData(util.CST.ERR404, util.CST.MSG404, res);
          }
        }).catch(err => {
          //获取规则链metaData出现问题
          logger.log('error', 'Cannot get rulechain meta data.');
          util.responErrorMsg(err, res);
        });
      } else {
        //规则链返回值格式不对
        logger.log('error', 'Got rulechain, but response data is wrong.');
        util.responData(util.ERR510, util.MSG510, res);
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

// POST ---OBJ---
async function postWarningRules(req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];

  // 获取规则链
  let ruleChain = await util.getSync('http://cf.beidouapp.com:8080/api/ruleChains',
    {
      headers: {
        "X-Authorization": token
      },
      params: {
        textSearch: "CONFIG_WARNING_RULE",
        limit: 1
      }
    }
  );

  if (ruleChain) {
    ruleID = ruleChain.data[0].id;
    // 获取告警规则链的meta数据
    let ruleMeta = await util.getSync('http://cf.beidouapp.com:8080/api/ruleChain/' + ruleID.id + '/metadata',
      {
        headers: {
          "X-Authorization": token
        }
      }
    );
    //预警规则（节点2#，5#）
    let js5 = ruleMeta.nodes[5].configuration.jsScript;
    let js2 = ruleMeta.nodes[2].configuration.latestTsKeyNames;

    let index = js5.indexOf('/* warning rule tables */');
    eval(js5.substr(0, index));
    if (ruleTables) {
      // 更新
      ruleTables[assetID] = req.body;

      //遍历新的ruleTables，把devId全部整理出来
      let allDevIds = new Set();
      let keys = Object.keys(ruleTables);
      keys.forEach(it => {
        let rule = ruleTables[it];
        let bRules = rule['blueRules'];
        let oRules = rule['orangeRules'];
        bRules.forEach(element => {
          let ids = element.andRule;
          ids.forEach(el => {
            allDevIds.add(el);
          });
        });
        oRules.forEach(element => {
          let ids = element.andRule;
          ids.forEach(el => {
            allDevIds.add(el);
          });
        });
      });

      js2 = [...allDevIds];

      js5 = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + js5.substr(index);

      ruleMeta.nodes[2].configuration.latestTsKeyNames = js2;
      ruleMeta.nodes[5].configuration.jsScript = js5;

      // post 更新
      url = util.getAPI() + 'ruleChain/metadata';
      axios.post(url, (ruleMeta), { headers: { "X-Authorization": token } })
        .then(response => {
          if (response.status == 200) {
            util.responData(200, '设置预警规则成功。', res);
          }
          else {
            //let code = response.status;
            //util.responErrorMsg(code, res);
          }
        })
        .catch(err => {
          util.responErrorMsg(err, res);
        })
    }
  }
}



module.exports = router