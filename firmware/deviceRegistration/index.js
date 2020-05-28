let admin = require('firebase-admin');
const { exec } = require("child_process");
const axios = require('axios')
admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    databaseURL: 'https://<iot-hub-273405.firebaseio.com'
  });
  
let db = admin.firestore()
const url = 'https://mdash.net/api/v2/devices?access_token=rt90vnkss4vQ2mzHTWzCmAA'

const projectId = 'iot-hub-273405'
const regionId = 'us-central1'
const registryId = 'registery_1'

let p = axios.post(url)
.then(res => {
    const token = res.data.token
    const id = res.data.id
    console.log(`mdashId: ${id}`)
    return db.collection('devices').add({
        mdashId: id,
        mdashToken: token,
        deploymentStatus:0,
        fwCurrent:1,
        cloudRegion: regionId,
        registryId: registryId,
        lightIntensity: 0,
        mode:0,
        state:0,
        active:0,
        online: 0
    }).then(doc=>doc.id)
}).then(doc => {
    console.log(`deviceId: D${doc.id}`)
    setDeviceId(doc.id)
})
    




function setDeviceId(deviceId) {
    exec(`mos config-set device.id=D${deviceId}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done deviceId")
            setWifi()
        }
    })
}

function setWifi() {
    exec('mos wifi King_pin iubi0792',(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done WiFi")
            setupGCP()
        }
    })
}

function setBT() {
    exec(`mos config-set bt.enable=true bt.dev_name=${deviceId}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done bt")
            setupGCP()
        }
    })
}

function setupGCP() {
    exec(`mos gcp-iot-setup --gcp-project ${projectId} --gcp-region ${regionId} --gcp-registry ${registryId}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done gcp")
        }
    })
}