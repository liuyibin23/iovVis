// const url   = require("url");
const axios = require('axios')

var API = 'http://cf.beidouapp.com:8080/api/';


function getAPI(){
    return API;
}

async function getSync(url, data, tok) {
    try {
        let res = await axios.get(url, data);
        let res_data = res.data;
        return new Promise((resolve, reject) => {
            if (res.status === 200) {
                resolve(res_data);
            } else {
                reject(res);
            }
        })
    } catch (err) {
        console.log("error:", err);
    }
}

async function postSync(url, data, tok) {
  try {
      let res = await axios.post(url, (data), { headers: { "X-Authorization": "Bearer " + tok } });
      let res_data = res.data;
      return new Promise((resolve, reject) => {
          if (res.status === 200) {
              resolve(res_data);
          } else {
              reject(res);
          }
      })
  } catch (err) {
      console.log("error:", err);
  }
}

exports.getSync  = getSync;
exports.postSync = postSync;
exports.getAPI   = getAPI;