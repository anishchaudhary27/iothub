let admin = require('firebase-admin');
const { exec } = require("child_process");
const axios = require('axios')
admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    databaseURL: 'https://<iot-hub-273405.firebaseio.com'
  });
  
let db = admin.firestore()
const url = 'https://mdash.net/api/v2/devices?access_token=rt90vnkss4vQ2mzHTWzCmAA'

let p = axios.post(url)
.then(res => {
    const token = res.data.token
    const id = res.data.id
    return db.collection('devices').add({
        mdashId: id,
        mdashToken: token,
        deploymentStatus:0,
        fwCurrent:0,
        cloudRegion: 'us-central1',
        registryId: 'registery_1',
        lightIntensity: 0,
        healthStatus: 1,
        mode:0,
        state:0,
        online: 0
    })
}).then(doc => {
    exec(`mos config-set device.id=D${doc.id}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        console.log(stdout)
    })
    return doc
}).then(doc => {
    console.log(`deviceId: D${doc.id}`)
}).catch(err => {
    console.log(err)
})