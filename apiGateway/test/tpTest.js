const axios = require('axios');
main();

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

async function getSync(url, data) {
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

async function main() {
    var assetId = 'cd59be20-12fc-11e9-bae8-7562662cc4ee'; //bridgeABC should passed
    // var assetId = 'd82f0d80-25c8-11e9-a39e-ad5bf9acdacd'; // should passed
    // var assetId = 'd82f0d80-25c8-11e9-a39e-ad5bf9acdacd'; // should not passed

    // 1. get token
    var loginRes = await postSync('http://cf.beidouapp.com:8080/api/auth/login',
        // { "username": "lvyu@beidouapp.com", "password": "12345" }, //tenant admin should passed
        { "username": "shenji@beidouapp.com", "password": "shenji" },  //system admin should passed
        "");
    if (!loginRes) return;
    var token = loginRes.token;

    // 2. get report templates api test
    var devRes = await getSync(`http://cf.beidouapp.com:20050/api/v1/templates/${assetId}`,
        {
            headers: {
                "X-Authorization": "Bearer " + token
            }
        }
    );
    if (devRes.code === '200') console.log('---An asset has TEMPLATES attribute test passed!---');

    // 3. post report templates api test
    devRes = await postSync(`http://cf.beidouapp.com:20050/api/v1/templates/${assetId}`,
        {
            headers: {
                "X-Authorization": "Bearer " + token
            }
        }
    );
    if (devRes.code === '200') console.log('---An asset has TEMPLATES attribute test passed!---');


}



