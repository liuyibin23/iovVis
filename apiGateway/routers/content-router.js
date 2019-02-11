
const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');

async function excuteGraphQL(req, res) {
    let graphQL = req.query.graphQL;
    if (graphQL) {
        axios.post('http://swapi.apis.guru', {query:graphQL},
        {
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            }
        })
        .then(resp => {
            util.responData(200, resp.data, res)
        })
        .catch(err => { 
            util.responErrorMsg(err, res);
        });
    }
}

// define the home page route
router.get('/', function (req, res) {
    if (req.baseUrl === '/api/v1/tables') {
        excuteGraphQL(req, res);
    }
    else {
        res.send('content Api home page');
    }
})

// define the about route
router.get('/about', function (req, res) {
    res.send('About Content')
})

router.post('/:id', async function (req, res) {
    let assetID = req.params.id;
    res.status(200).json({code:200,message:'not support this time!'});
})

router.get('/:id', async function (req, res) {
    let token = req.headers['x-authorization'];
    // download file
    let downloadFileHost = req.params.id;
    axios.get(downloadFileHost, {
        headers: {
            "X-Authorization": token
        }
    }).then((resp) => {
        util.responData(200, resp.data, res);
    }).catch((err) => {
        util.responErrorMsg(err, res);
    });
})

module.exports = router;