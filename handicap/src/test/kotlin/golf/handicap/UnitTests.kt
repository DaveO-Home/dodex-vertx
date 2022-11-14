import handicap.grpc.*
import io.grpc.*
import io.grpc.stub.*
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.net.SocketAddress
import io.vertx.core.json.JsonObject
import io.vertx.grpc.*
import io.vertx.grpc.client.*
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import golf.handicap.Golfer
import org.junit.jupiter.api.Disabled;

class AppTest {
    companion object {
        @BeforeAll
        fun printMessage() {
            System.out.println("Starting Tests")
        }
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
    @Disabled("Disabled because it requires that the verticle is running")
    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun `verticle returned html`(): Unit =
            runBlocking(vertx.dispatcher()) {
                val httpClient = vertx.createHttpClient()
                val request =
                        httpClient
                                .request(HttpMethod.GET, 8888, "localhost", "/handicap/handicap.html")
                                .await()
                val response = request.send().await()
                val responseJson = response.body().await()
                val data: String = responseJson.toString()
                assertNotEquals("", data)
                val containsTitle: Boolean = data.contains("<title>Golf Handicap Index</title>")
                assertTrue(containsTitle, "Handicap is up")
            }
    /*
        Requires that both the Handicap Verticle and envoy are running
    */
    @Disabled("Disabled because it requires that the verticle is running")
    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun `gRpc client request is successful`(): Unit = runBlocking(vertx.dispatcher()) {
        val port = 15001
        val host = "localhost"
        val client: GrpcClient = GrpcClient.client(vertx)
        val server: SocketAddress = SocketAddress.inetSocketAddress(port, host)        
        val getHandicapMethod: MethodDescriptor<HandicapSetup, HandicapData> =
                HandicapIndexGrpc.getGetGolferMethod()
        val fut: Future<GrpcClientRequest<HandicapSetup, HandicapData>> =
                client.request(server, getHandicapMethod)
        var requestMade = false
        
        fut
        .onFailure { fail ->
            requestMade = false
            throw Exception(fail.message)
        }
        .onSuccess { request ->
            val jsonObject = JsonObject()
            jsonObject.put("pin", "pet1234")
            jsonObject.put("firstName", "Ace")
            jsonObject.put("lastName", "Ventura")
            jsonObject.put("state", "NV")
            jsonObject.put("lastLogin", 0L)
            request.end(
                HandicapSetup.newBuilder()
                    .setJson(jsonObject.toString())
                    .setCmd(3)
                    .setMessage("Test")
                    .build()
            )
            .onFailure {
                requestMade = false
            }
            .onSuccess({
                requestMade = true   // The Server should print the request data e.g. "pet1234"
            })
        }
        .await()

        assertTrue(requestMade, "gRpc request made")
    }
    @Disabled("Disabled because it requires that the verticle is running")
    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun `gRpc client response is successful`(): Unit = runBlocking(vertx.dispatcher()) {
        val port = 15001
        val host = "localhost"
        val client: GrpcClient = GrpcClient.client(vertx)
        val server: SocketAddress = SocketAddress.inetSocketAddress(port, host)
        var responseMade = false
        var cmd = 0

        client.request(server, HandicapIndexGrpc.getGetGolferMethod()).compose({request ->
            val jsonObject = JsonObject()
            jsonObject.put("pin", "ace1234")
            jsonObject.put("firstName", "Ace")
            jsonObject.put("lastName", "Ventura")
            jsonObject.put("state", "NV")
            jsonObject.put("lastLogin", System.currentTimeMillis())

            request.end(HandicapSetup
                .newBuilder()
                .setJson(jsonObject.toString())
                .setCmd(3)
                .setMessage("Test")
                .build())
            request.response().compose({response -> response.last()})
                .onSuccess({reply ->
                    System.out.println("Received " + reply.cmd);
                    cmd = reply.cmd
                    responseMade = true
                })
        }).onSuccess({reply -> 
            System.out.println("Received " + reply);
        }).await()

        assertTrue(responseMade, "gRpc response from server")
        assertEquals(3, cmd, "Returned cmd value")
    }
}
