const express = require('express');
const axios = require('axios');
const util = require('./utils');
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
      "code": `${resp.status}`,
      info: {}
    };
    resp.data.forEach(info => {
      if (info.key === 'asset_warning_level') {
        // info.value = JSON.parse(info.value);
        // res.status(200).json(info);
        resMsg.info = info;
      }
    });
    res.status(resp.status).json(resMsg);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
}
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
      util.responData(200, '成功设置预警状态。', res);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
}
async function getWarningRules(req, res) {
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

    let index = js5.indexOf('/* warning rule tables */');
    eval(js5.substr(0, index));
    if (ruleTables) {
      // 找到资产对应的规则 并返回
      let rules = ruleTables[assetID];
      if (rules) {
        util.responData(200, rules, res);
      }
      else {
        util.responData(404, '访问资源不存在。', res);
      }
    }
  }
}

// POST
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