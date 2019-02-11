const express = require('express');
const multipart = require('connect-multiparty');
const axios = require('axios');
const fs = require('fs');
const request = require('request');
// const toks = require('../middleware/token-verifier');
const util = require('../util/utils');

const router = express.Router();
const multipartMiddleware = multipart();

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Reports Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
  res.send('Reports Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About reports')
})

//GET STAT
router.get('/stat', function (req, res) {
  let ret_data = [];
  let limit = req.query.limit;
  let token = req.headers['x-authorization'];
  let urlGetReports = util.getAPI() + `assets/assetattr`;
  if (limit) {
    axios.get(urlGetReports, {
      headers: {
        "X-Authorization": token
      },
      params: {
        limit,
        attrKey: 'REPORTS'
      }
    }).then(resp => {
      try {
        resp.data.forEach(element => {
          if(element.entity_type='ASSET') {
            let it = {};
            it.assetId = element.id;
            it.reports = JSON.parse(element.strV);
            ret_data.push(it);
          }
        });
        let reports = [];
        ret_data.forEach(element=>{
          reports = [... element.reports, ...reports];
        })
        util.responData(util.CST.OK200, reports, res);
      } catch (err) {
        util.responErrorMsg(err, res);
      }
    }).catch(err => {
      util.responErrorMsg(err, res);
    });
  } else {
    util.responData(util.CST.ERR400, util.CST.MSG400, res);
  }
})
// GET
router.get('/:assetId', async function (req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    let find = false;
    for (var i = 0; i < resp.data.length; i++) {
      let info = resp.data[i];
      if (info.key === 'REPORTS') {
        find = true;
        info.value = JSON.parse(info.value);

        util.responData(200, info.value, res);
        break;
      }
    }
    if (!find) {
      util.responData(resp.status, '无数据。', res);
    }
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
})
// POST
router.post('/:id', multipartMiddleware, async function (req, res) {
  let assetID = req.params.id;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    PostReports(assetID, resp, req, res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
})
// DELETE
router.delete('/:id', async function (req, res) {
  // 查询属性
  let assetID = req.params.id;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      processDeleteReq(assetID, resp, req, res, token);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
})

// 保存到属性表
function saveAssetSeverScope(hisReprtsData, assetID, reportInfo, urlPath, req, res, token) {
  // let host = 'http://sm.schdri.com:80/';
  // http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/SERVER_SCOPE

  //接口因权限问题修改，需要根据assetId查询tenantId
  let urlGetTenant = util.getAPI() + `asset/${assetID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    let url = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;
    //let bodyData = JSON.parse(body)
    let str = [{
      "report_name": reportInfo.report_name,
      "report_type": reportInfo.report_type,
      "report_date": reportInfo.report_date,
      "report_url": urlPath
    }];

    hisReprtsData.data.forEach(info => {
      if (info.key === 'REPORTS') {
        let data = JSON.parse(info.value);
        data.forEach(_dt => {
          str.push(_dt);
        })
      }
    });
    let val = JSON.stringify(str);
    let data = {
      "REPORTS": `${val}`
    };
    axios.post(url, (data), {
      headers: { "X-Authorization": token },
      params: { "tenantIdStr": resp.data.tenantId.id }
    }).then(response => {
      util.responData(200, '成功创建报表并关联到资产。', res);
    }).catch(err => {
      util.responErrorMsg(err, res);
    });
  }).catch(error => {
    util.responErrorMsg(error, res);
  });
}
// 生成报表文件
async function generateReports(assetId, hisReprtsData, reportFilePath, req, res, token) {
  // 上传生成的报表文件    ==> 返回上传成功的地址
  var formData = {
    file: fs.createReadStream(reportFilePath),
  };
  let host = 'http://sm.schdri.com:80/';
  let uploadFileHost = host + 'api/file/upload/';
  request.post({ url: uploadFileHost, formData: formData }, function (err, httpResponse, body) {
    if (err) {
      return console.error('upload failed:', err);
    }
    else {
      console.log('Upload successful!  Server responded with:', body);

      if (JSON.parse(body).success) {
        let bodyData = JSON.parse(body)
        let urlPath = host + bodyData.fileId;
        // 保存到属性表
        saveAssetSeverScope(hisReprtsData, assetId, req.body, urlPath, req, res, token);
      }
      else {
        util.responData(501, '报表文件上传失败。', res);
      }
    }
  });
}

function PostReports(assetID, hisReprtsData, req, res) {
  let token = req.headers['x-authorization'];
  let fileInfo = req.files.report_file;
  // 根据发送过来的报表模板 ==> 生成报表文件
  generateReports(assetID, hisReprtsData, fileInfo.path, req, res, token);
}
//DELETE
function processDeleteReq(assetID, resp, req, res, token) {
  var info = null;
  // 遍历，查找REPORTS属性
  for (var i = 0; i < resp.data.length; i++) {
    info = resp.data[i];
    if (info.key === 'REPORTS') {
      break;
    }
  }

  // 遍历REPORTS属性，删除匹配的
  let find = false;
  let new_value = new Array();
  let report_url = null;
  if (info) {
    let jsonVal = JSON.parse(info.value);
    for (var i = 0; i < jsonVal.length; i++) {
      let _dt = jsonVal[i];
      if (_dt.report_name === req.query.reportemplateName) {
        find = true;
        report_url = _dt.report_url;
      }
      else {
        new_value.push(_dt);
      }
    }
  }

  if (find && report_url) {
    // 从文件服务器删除
    let host = util.getFSVR(); //'http://sm.schdri.com:80/';
    let deleteFileHost = host + 'api/file/delete/';
    let filePath = report_url.substr(host.length);
    request.post({ url: deleteFileHost, form: { fileId: filePath } }, function (err, httpResponse, body) {
      if (err) {
        util.responData(501, '删除资产的报表失败。', res);
      }
      else {
        let result = JSON.parse(body);
        if (result.success || result.code === 'error.fastdfs.file_delete_failed') {

          // 更新删除后的属性
          let val = JSON.stringify(new_value);
          let data = {
            "REPORTS": `${val}`
          };
          //接口因权限问题修改，需要根据assetId查询tenantId
          let urlGetTenant = util.getAPI() + `asset/${assetID}`;
          axios.get(urlGetTenant, {
            headers: { "X-Authorization": token }
          }).then(resp => {
            let url = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;
            axios.post(url, (data), {
              headers: { "X-Authorization": token },
              params: { "tenantIdStr": resp.data.tenantId.id }
            }).then(response => {
              util.responData(200, '成功删除资产的报表。', res);
            }).catch(err => {
              util.responErrorMsg(err, res);
            });
          }).catch(err => {
            util.responErrorMsg(err, res);
          });



        }
      }
    })
  }
  else {
    util.responData(200, '未找到匹配数据。', res);
  }
}

module.exports = router