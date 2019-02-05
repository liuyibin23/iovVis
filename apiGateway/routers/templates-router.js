const express = require('express');
const router = express.Router();
//var toks = require('../middleware/token-verifier');
const util = require('../util/utils');
const logger = require('../util/logger');
const fs = require('fs');
const axios = require('axios');
const request = require('request');
const multipart = require('connect-multiparty');
const multipartMiddleware = multipart();

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Templat Time: ', Date.now())
//   next()
// })
// define the home page route
router.get('/', function (req, res) {
  res.status(200).json({ result: 'Templates Api home page' });
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About templates')
})
// GET
router.get('/:assetId', async function (req, res) {
  let assetId = req.params.assetId;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetId}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      let find = false;
      for (var i = 0; i < resp.data.length; i++) {
        let info = resp.data[i];
        if (info.key === 'TEMPLATES') {
          find = true;
          info.value = JSON.parse(info.value);

          util.responData(200, info.value, res);
          break;
        }
      }
      if (!find) {
        util.responData(resp.status, '无数据', res);
      }
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
})
//POST
router.post('/:id', multipartMiddleware, async function (req, res) {
  let assetID = req.params.id;
  let msg = 'Post ID:' + assetID + 'template_name:' + req.body.template_name;

  
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    postTemplates(assetID, resp, req, res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
})
//DELETE
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
      processDeleteReq(assetID, resp, req, res);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
})

function postTemplates(assetID, resp, req, res) {
  let fileInfo = req.files.template_file;
  let info = {
    'filename': fileInfo.originalFilename,
    'path': fileInfo.path,
    'size': fileInfo.size,
    'type': fileInfo.type
  };

  let token = req.headers['x-authorization'];

  var formData = {
    file: fs.createReadStream(fileInfo.path),
  };
  let host = 'http://sm.schdri.com:80/';
  let uploadFileHost = host + 'api/file/upload/';
  request.post({ url: uploadFileHost, formData: formData }, function (err, httpResponse, body) {
    if (err) {
      util.responData(501, '报表模板上传失败。', res);
    }
    else {
      logger.log('info', 'Upload successful!  Server responded with:', body);
      if (JSON.parse(body).success) {
        // 保存到属性表
        // http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/SERVER_SCOPE
        let url = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;
        let bodyData = JSON.parse(body)
        let str = [{
          "template_name": req.body.template_name,
          "template_url": host + bodyData.fileId
        }];
        // FIXME:资产模板名称重复会多次出现，需要修改
        resp.data.forEach(info => {
          if (info.key === 'TEMPLATES') {
            let data = JSON.parse(info.value);
            data.forEach(_dt => {
              str.push(_dt);
            })
          }
        });

        let val = JSON.stringify(str);

        let data = {
          "TEMPLATES": `${val}`
        };
        //接口因权限问题修改，需要根据assetId查询tenantId
        let urlGetTenant = util.getAPI() + `asset/${assetID}`;
        axios.get(urlGetTenant, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          axios.post(url, (data), {
            headers: { "X-Authorization": token },
            params: { "tenantIdStr": resp.data.tenantId.id }
          }).then(response => {
            util.responData(response.status, '成功创建报表模板并关联到资产。', res);
          }).catch(err => {
            util.responErrorMsg(err, res);
          });
        }).catch(err => {
          util.responErrorMsg(err, res);
        });
      }
      else {
        util.responData(501, '报表模板上传失败。', res);
      }
    }
  });
}
function processDeleteReq(assetID, resp, req, res) {
  let token = req.headers['x-authorization'];
  var info = null;
  // 遍历，查找TEMPLATES属性
  for (var i = 0; i < resp.data.length; i++) {
    info = resp.data[i];
    if (info.key === 'TEMPLATES') {
      break;
    }
  }

  // 遍历TEMPLATES属性，删除匹配的
  let find = false;
  let template_url = null;
  let new_value = new Array();
  if (info) {
    let jsonVal = JSON.parse(info.value);
    for (var i = 0; i < jsonVal.length; i++) {
      let _dt = jsonVal[i];
      if (_dt.template_name === req.query.templateName) {
        find = true;
        template_url = _dt.template_url;
      }
      else {
        new_value.push(_dt);
      }
    }
  }

  if (find && template_url) {
    // 从文件服务器删除
    let host = util.getFSVR(); //'http://sm.schdri.com:80/';
    let deleteFileHost = host + 'api/file/delete/';
    let filePath = template_url.substr(host.length);
    request.post({ url: deleteFileHost, form: { fileId: filePath } }, function (err, httpResponse, body) {
      if (err) {
        util.responData(501, '报表模板删除失败。', res);
      }
      else {
        let result = JSON.parse(body);
        if (result.success|| result.code ==='error.fastdfs.file_delete_failed') {
          // 更新删除后的属性
          let val = JSON.stringify(new_value);
          let data = {
            "TEMPLATES": `${val}`
          };
          //接口因权限问题修改，需要根据assetId查询tenantId
          let urlGetTenant = util.getAPI() + `asset/${assetID}`;
          axios.get(urlGetTenant, {
            headers: {
              "X-Authorization": token
            }
          }).then(resp => {
            let url = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;
            axios.post(url, (data), {
              headers: { "X-Authorization": token },
              params: { "tenantIdStr": resp.data.tenantId.id }
            }).then(response => {
              util.responData(response.status, '成功删除资产的模板。', res);
            }).catch(err => {
              util.responErrorMsg(err, res);
            });
          })
            .catch(err => {
              util.responErrorMsg(err, res);
            });
        }
        else {
          util.responData('501', '报表模板删除失败。', res);
        }
      }
    })
  }
  else {
    util.responData(200, '未找到匹配数据。', res);
  }
}

module.exports = router