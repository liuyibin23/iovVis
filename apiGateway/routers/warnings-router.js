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
      util.responData(resp.status, { asset_warning_level }, res);
    } else
      util.responData(util.CST.ERR404, util.CST.MSG404, res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
}

// 添加additionalInfo 属性
async function postAsset(assetID, token, args, res){
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

    let post_api = util.getAPI() +　`asset?tenantIdStr=${pdata.id.id}`;
    axios.post(post_api, pdata, {
      headers: {
        "X-Authorization": token
      }
    }).then((resp) => {
      util.responData(util.CST.OK200, util.CST.MSG200, res);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    }); 
  })
  .catch((err) => {
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
    let url = util.getAPI() + `currentUser/ruleChains`;
    let tenantId = resp.data.tenantId.id;
    // TID = tenantId;
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
      let ruleChain = resp.data;
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

    //根据tenantId和规则链名称获取规则链
    let url = util.getAPI() + `currentUser/ruleChains`;
    let tenantId = resp.data.tenantId.id;
    TID = tenantId; // save it for later usage.
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

      //根据规则链编号获取规则链中存储的meta数据
      let ruleChain = resp.data;
      if (ruleChain) {
        ruleID = ruleChain[0].id;
        let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
        axios.get(url, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          let ruleMeta = resp.data;
          if (ruleMeta) {
            //获取现存的预警规则相关的两个规则链节点
            let nodes = ruleMeta.nodes;
            let js1, js2, index1 = -1, index2 = -1;
            for (let index in nodes) {
              if (nodes[index].name === util.CFG.WARN_NODE_RULE) {
                //预警规则设置节点
                js1 = nodes[index].configuration.jsScript;
                index1 = index;
              } else if (nodes[index].name === util.CFG.WARN_NODE_ALARM_DEV) {
                //获取所有规则需要的属性
                js2 = nodes[index].configuration.latestTsKeyNames;
                index2 = index;
              }
            }
            //js1,js2都需要被找到并赋值
            if (typeof js1 !== 'undefined' && typeof js2 !== 'undefined') {
              let jsScript = js1;
              let index = jsScript.indexOf('/* warning rule tables */');
              eval(jsScript.substr(0, index));
              if (typeof ruleTables !== 'undefined') {
                ruleTables[assetID] = WarningRule;
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
                js1 = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + js1.substr(index);
                nodes[index1].configuration.jsScript = js1;
                nodes[index2].configuration.latestTsKeyNames = js2;
                delete ruleTables;
                //post写库
                let url = util.getAPI() + 'ruleChain/metadata';
                //1.写入规则链
                axios.post(url, (ruleMeta), {
                  headers: { "X-Authorization": token }
                }).then(response => {
                  if (response.status == 200) {
                    //2.写入资产的addinfo（失败的情况不考虑）
                    //WarningRule, assetID, TID
                    //通过资产ID获取资产信息
                    url = util.getAPI() + `asset/${assetID}`;
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
                        params: { "tenantIdStr": TID }
                      }).then(resp => {
                        //返回成功
                        util.responData(util.CST.OK200, util.CST.MSG200, res);
                      }).catch(err => {
                        //post资产ID更新资产失败
                        util.responErrorMsg(err, res);
                      });
                    }).catch(err => {
                      //通过资产ID获取信息失败
                      util.responErrorMsg(err, res);
                    });
                  }
                  else {
                    //
                    logger.log('error', 'FIXME: why should I go here?')
                    util.responData(util.CST.ERR404, util.CST.MSG404, res);
                  }
                }).catch(err => {
                  util.responErrorMsg(err, res);
                })

              }
            }
          }
          // util.responData(511, 'CONFIG_ALARM_RULE规则链MetaData获取失败。' , res);
        }).catch(err => {
          //无法通过规则链编号获取规则链中存储的meta数据
          util.responErrorMsg(err, res);
        });
      } else {
        logger.log('error', util.CST.MSG510);
        util.responData(util.ERR510, util.MSG510, res);
      }
    }).catch(err => {
      //无法通过tenantId和规则链名称获取规则链
      util.responErrorMsg(err, res);
    });
  }).catch(err => {
    //无法通过设备号devID获取tenantId
    util.responErrorMsg(err, res);
  });
}

module.exports = router