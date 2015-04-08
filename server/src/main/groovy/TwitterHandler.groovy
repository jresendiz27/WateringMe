// Twitter Handler
import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*

class TwitterHandler {
    def twitterKeys
    def url
    def path
    def twitterRest

    TwitterHandler(def keys, def url, def path) {
        this.url = url
        this.path = path
        twitterRest = new RESTClient(url)
        twitterKeys = [:] << keys

        twitterRest.auth.oauth(
                twitterKeys.consumerKey,
                twitterKeys.consumerSecret,
                twitterKeys.accessToken,
                twitterKeys.secretToken)
    }

    def tweetStatus(def tweetContent) {
        def postBody = [status: tweetContent]
        twitterRest.post(path: this.path, body: postBody, requestContentType: URLENC) { response ->
            response.success = { resp ->
                println "Tweet Published!!"
                assert resp.statusLine.statusCode == 200
            }
            response.failure = { resp ->
                println "Failing at connecting to Twitter API"
            }
        }
    }
}