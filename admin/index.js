const admin = require('firebase-admin')

admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    databaseURL: 'https://iot-hub-273405.firebaseio.com'
})

admin.messaging().send({
    notification:{
        title: 'Good Morning!!',
        body: 'Have a great day'
    }
})