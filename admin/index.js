const admin = require('firebase-admin')

admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    databaseURL: 'https://iot-hub-273405.firebaseio.com'
})

admin.messaging().send({
    notification:{
        title: 'Good Morning!!',
        body: 'Have a great day anish😃'
    },
    token:'eCn5icqEqQY:APA91bG2I-bYjQoo2mTeeeE83Q1wUk4YvCXfa9g1mgsQyPesAYMlGUvDiRy0gA44CNwZoIIoL9C_M3sItPgqb1M4j_EOtVE8dlh_GHqOUFkvfliHLxrUOfBKsCX05gDPDEwYzQl1WtmV'
})
.then(al=>{
    console.log(al)
})
.catch(err=>{
    console.log(err)
})