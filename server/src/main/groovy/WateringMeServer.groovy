@Grab(group='io.vertx', module='vertx-core', version='2.1.4')
@Grab(group='io.vertx', module='vertx-platform', version='2.1.4')
@Grab(group='io.vertx', module='lang-groovy', version='2.1.1-final')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7')
@Grab(group='oauth.signpost', module='signpost-core', version='1.2.1.2')
@Grab(group='oauth.signpost', module='signpost-commonshttp4', version='1.2.1.2')

import org.vertx.groovy.core.Vertx;
import org.vertx.groovy.core.http.RouteMatcher
import Tools
import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*

class WateringMeServer {
    static void main(String[] args) {
        //
        def wateringProps = Tools.propertiesReader("watering-me.properties")
        def server = Vertx.newVertx()
        def rm = new RouteMatcher()
        //
        rm.get('/start') { req ->
            // Reading status file
            def status = Tools.propertiesReader("status.properties")
            // Setting the next execution date
            def nextWateringDate = new Date()
            if (status.nextDate) {
                nextWateringDate = Date.parse(wateringProps.dateFormat, status.nextDate)
            }
            // Getting current date
            def wateringDate = new Date()
            wateringDate.hours = wateringProps.wateringHours as Integer
            wateringDate.minutes = wateringProps.wateringMinutes as Integer
            println "**********************************************************"
            println "New Request from : " + req.getRemoteAddress()
            println "Current Date: ${nextWateringDate}"
            println "Watering Date : ${wateringDate}"
            println "**********************************************************"
            if (nextWateringDate.before(wateringDate)) { // water the plant
                nextWateringDate = nextWateringDate.next()
                status.setProperty("executed", "true")
                status.setProperty("nextDate", nextWateringDate.format(wateringProps.dateFormat))
                status.store(new File("status.properties").newWriter(), null)
                //
                req.response.end("riegame") //status code
            } else {
                req.response.end("no_me_riegues") //status code
            }


        }
        rm.get('/tweet/:statusCode') { req ->
            println "-------------------------------------------"
            println "Tweet status!!!"
            def consumerKey="KvoKNWTfWtoHkY9Lrfwcos0YH"
            def consumerSecret="5sqr8Kp2lLQ2e30lyGH6KNeLlVWeDGiKhW9WJ80SzJKTtVTZDU"
            def accessToken="468504003-neoVctVFfm39iMYgtmELwEk48145AchcWwz6Bq2F"
            def secretToken="nKAB8SAIqH9vCR22DKAruvPdQiBDjG4wDRSsycxl81OmK"
            def twitter = new RESTClient( 'https://api.twitter.com/1.1/statuses/' )
            twitter.auth.oauth(consumerKey, consumerSecret, accessToken, secretToken)
            def generatedStatus = ""
            switch (req.params['statusCode']){
                case '1':
                    generatedStatus = "Tweet from WateringMe :  Regando la planta!  [${new Date()}]"
                    break;
                case '2':
                    generatedStatus = "Tweet from WateringMe :  Abriendo el domo!  [${new Date()}]"
                    break;
                case '3':
                    generatedStatus = "Tweet from WateringMe :  Cerrando el domo!  [${new Date()}]"
                    break;
            }
            println generatedStatus
            println "-------------------------------------------"
            def postBody=[status:generatedStatus]
            def response = twitter.post( path : 'update.json', body: postBody, requestContentType: URLENC )
            req.response.end("Enviado!")

        }
        // Catch all - serve the index page
        rm.getWithRegEx('.*') { req ->
            req.response.sendFile "/index.html"
        }
        server.createHttpServer().requestHandler(rm.asClosure()).listen(wateringProps.port as Integer)
        println "Listening at ${wateringProps.port}"
        System.in.read();
        while (true) {
            //
        }
    }
}