var express = require('express')
var router = express.Router()
var toks  = require('../middleware/token-verifier')
var util  = require('./utils')
var multipart = require('connect-multiparty');  
var multipartMiddleware = multipart();


// middleware that is specific to this router
router.use(function timeLog (req, res, next) {
  console.log('Time: ', Date.now())
  next()
})

// Get asset
async function getAsset(assetId, token)
{
  let get_asset_api = util.getAPI() + 'assets?assetIds=' + assetId;
  let assetInfo = await util.getSync(get_asset_api, {
      headers: {
          "X-Authorization": token
      }
  });

  if (!assetInfo) return;

  let len = assetInfo.length;
  let data = new Array();
  if (len > 0) {
      console.log('1. Get asset info:%d', len);
      assetInfo.forEach(info => {
          let _dt = {id:'', name:'', type:''};
          _dt.id   = info.id.id;
          _dt.name = info.name;
          _dt.type = info.type;

          data.push(_dt);
      });

      return data;
  }

  let _dt = {id:'', name:'', type:''};
  data.push(_dt);
  return data;
}

// middleware that is specific to this router
router.use(function timeLog (req, res, next) {
  console.log('Reports Time: ', Date.now())
  next()
})

// GET
router.get('/:assetId', async function (req, res) {
  let assetID = req.params.assetId;
  console.log('assetId=' + assetID);

  let token = req.headers['x-authorization'];
  let data = await getAsset(assetID, token);

  res.status(200).json(data);
})

//POST
router.post('/:id', multipartMiddleware, async function(req, res){
  let fileInfo = req.files.report_file;
  let info = {
    'Post ID:':req.params.id,
    'filename':fileInfo.originalFilename,
    'path':fileInfo.path,
    'size':fileInfo.size,
    'type':fileInfo.type
  }

  res.status(200).json(info);
})


//DELETE
router.delete('/:id', async function(req, res){
  let msg = 'Delete ID:' + req.params.id + ' Name:' + req.query.reportemplateName ;

  res.status(200).json(msg);
})

// define the home page route
router.get('/', function (req, res) {
  res.send('Reports Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About reports')
})


module.exports = router