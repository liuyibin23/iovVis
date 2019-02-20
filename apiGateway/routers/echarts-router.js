const express = require('express');
const chart_area = require('./echarts/area');
const chart_pie  = require('./echarts/pie');
const node_echarts = require('node-echarts');
const util = require('../util/utils');
const logger = require('../util/logger');
// var multipart = require('connect-multiparty');
// var multipartMiddleware = multipart();

const router = express.Router();
// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//     console.log('echarts Time: ', Date.now())
//     next()
// })

// define the home page route
router.get('/', function (req, res) {
    res.send('Echarts Api home page')
})

// define the about route
router.get('/about', function (req, res) {
    res.send('About Echarts')
})

router.post('/:id', async function (req, res) {
    let assetID = req.params.id;
})
 

function processData(option, params, res){
    if (!option)
    {
        util.responData(404, '访问资源不存在。', res);
        return;  
    }

    let config = {
        width: params.chartWidth   ? params.chartWidth  * 100 : 500, // Image width, type is number.
        height: params.chartHeight ? params.chartHeight * 100 : 400, // Image height, type is number.
        option: option, // Echarts configuration, type is Object.
        //If the path  is not set, return the Buffer of image.
        // path:  '', // Path is filepath of the image which will be created.
        enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
    }
    let bytes = node_echarts(config);
    if (bytes) {
        let data = 'data:image/png;base64,' + bytes.toString('base64');
        util.responData(200, data, res);
    }
}

router.get('/:id', async function (req, res) {
    let type   = req.params.id;
    let params = req.query;
    logger.log('info', 'type:', type, 'query:', req.query);
    let token = req.headers['x-authorization'];;
    switch (type) {
        case 'area':
            chart_area.fillData(params, token, res, processData);
            break;
        case 'pie':
        {
            chart_pie.fillData(params, token, res, processData);
            break;
        }
        default:
    }
})

module.exports = router