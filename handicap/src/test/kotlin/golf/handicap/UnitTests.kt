import golf.handicap.vertx.MainVerticle
import handicap.grpc.*
import io.grpc.*
import io.grpc.stub.*
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SocketAddress
import io.vertx.grpc.*
import io.vertx.grpc.client.*
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.rxjava3.core.Vertx
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.util.concurrent.TimeUnit

/*
    Make sure the dev vertx server and envoy proxy are running before running tests.
*/

@ExtendWith(VertxExtension::class)
class AppTest {
    companion object {
        /* Do a delete from db where pins = (pet1234, ace1234) */
    }
    @Test
    @Throws(IOException::class)
    fun getJavaResource() {
        val inf = this.javaClass.classLoader.getResource("application-conf.json")
        assertNotEquals(inf, null)
        assertNotNull(inf, "should find resource file.")
    }
    /*
        Requires that the Handicap Verticle is running
    */
    val vertx = Vertx.vertx()
//    var config = MainVerticle.getAlternateConfig()
    @Disabled("Disabled - problem with Http Client - timesout")
    @Test
    @Timeout(3, unit = TimeUnit.SECONDS)
    fun `verticle returned html`(): Unit =
            runBlocking(vertx.delegate.dispatcher()) {
                val httpClient = vertx.delegate.createHttpClient()

                val request =
                        httpClient
                                .request(HttpMethod.GET, 8085, "http://localhost", "/handicap.html")
                                // .compose { result ->
                                //     System.out.println("Composed on Client Request: " + result)
                                //     val promise: Promise<HttpClientRequest?> = Promise.promise()
                                //     result.send()
                                //     promise.complete(result)
                                //     promise.future()
                                // }
                                .onFailure { err ->
                                    System.out.println(
                                            err
                                    ) // execute "gradlew test --rerun --info" to get output
                                }
                                .await()
                request.send()
                // val req = request.await()
                val responseJson = request!!.response().result().body().await()
                request.end()
                val data: String = responseJson.toString()
                assertNotEquals("", data)
                val containsTitle: Boolean = data.contains("<title>Golf Handicap Index</title>")
                assertTrue(containsTitle, "Handicap is up")
            }
    /*
        Requires that both the Handicap Verticle and envoy are running
    */
    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun `gRpc client request is successful`(): Unit =
            runBlocking(vertx.delegate.dispatcher()) {
                val config = MainVerticle.getAlternateConfig()
                val port = config.getInteger("grpc:port")
                val host = "localhost"
                val client: GrpcClient = GrpcClient.client(vertx.getDelegate())
                val server: SocketAddress = SocketAddress.inetSocketAddress(port, host)
                val getHandicapMethod: MethodDescriptor<HandicapSetup, HandicapData> =
                        HandicapIndexGrpc.getGetGolferMethod()
                val fut: Future<GrpcClientRequest<HandicapSetup, HandicapData>> =
                        client.request(server, getHandicapMethod)
                var requestMade = false

                fut.compose { request ->
                    val jsonObject = JsonObject()
                    jsonObject.put("pin", "pet1234")
                    jsonObject.put("firstName", "Ace")
                    jsonObject.put("lastName", "Ventura")
                    jsonObject.put("state", "NV")
                    jsonObject.put("country", "US")
                    jsonObject.put("lastLogin", 0L)
                    request.end(
                        HandicapSetup.newBuilder()
                            .setJson(jsonObject.toString())
                            .setCmd(3)
                            .setMessage("Test")
                            .build()
                    )
                }
                    .onFailure { fail ->
                            requestMade = false
                            throw Exception(fail.message)
                        }
                        .onSuccess { requestMade = true }
                        .await()

                assertTrue(requestMade, "gRpc request made")
            }
    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun `gRpc client response is successful`(): Unit =
            runBlocking(vertx.delegate.dispatcher()) {
                val config = MainVerticle.getAlternateConfig()
                val port = config.getInteger("grpc:port")
                val host = "localhost"
                val client: GrpcClient = GrpcClient.client(vertx.getDelegate())
                val server: SocketAddress = SocketAddress.inetSocketAddress(port, host)
                var responseMade = false
                var cmd = 0

                client.request(server, HandicapIndexGrpc.getGetGolferMethod())
                        .compose { request ->
                            val jsonObject = JsonObject()
                            jsonObject.put("pin", "ace1234")
                            jsonObject.put("firstName", "Ace")
                            jsonObject.put("lastName", "Ventura")
                            jsonObject.put("state", "NV")
                            jsonObject.put("country", "US")
                            jsonObject.put("lastLogin", System.currentTimeMillis())

                            request.end(
                                HandicapSetup.newBuilder()
                                    .setJson(jsonObject.toString())
                                    .setCmd(3)
                                    .setMessage("Test")
                                    .build()
                            )
                            request.response()
                                .compose { response -> response.last() }
                                .onSuccess { reply ->
                                    System.out.println("Received " + reply.cmd)
                                    cmd = reply.cmd
                                    responseMade = true
                                }
                        }
                    .onSuccess { reply -> println("Received $reply") }
                    .onFailure { err ->
                            println("Error on client Response: " + err.message)
                        }
                    .await()

                assertTrue(responseMade, "gRpc response from server")
                assertEquals(3, cmd, "Returned cmd value")
            }
}
