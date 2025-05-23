import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import golf.handicap.vertx.HandicapGrpcServer
import handicap.grpc.HandicapData
import handicap.grpc.HandicapIndexGrpc
import handicap.grpc.HandicapSetup
import io.grpc.stub.StreamObserver
import io.reactivex.rxjava3.functions.Consumer
import io.vertx.core.Promise
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpVersion
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SocketAddress
import io.vertx.grpcio.client.GrpcIoClient
import io.vertx.grpcio.client.GrpcIoClientChannel
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.rxjava3.core.Vertx
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class AppTest {
    companion object {
        var logger: Logger = LoggerFactory.getLogger(AppTest::class.java)
        val vertx: Vertx? = Vertx.vertx()

        @JvmStatic
        @BeforeAll
        fun deployVerticle() {
            System.setProperty("kotlinTest", "true")
            val latch = CountDownLatch(1)
            val config = getAlternateConfig()

            try {
                val mainVerticle = HandicapGrpcServer()
                vertx?.deployVerticle(mainVerticle)?.doOnSuccess(Consumer { actual: String? ->
                assertNotNull(actual)
                latch.countDown();
            })?.subscribe()
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        private fun getAlternateConfig(): JsonObject {
            val jsonMapper = ObjectMapper()
            var node: JsonNode?
            val path = Paths.get("src", "test",  "kotlin", "resources").toFile().absolutePath;
            val bufferedReader: BufferedReader = File("$path/application-conf.json").bufferedReader()
            bufferedReader.use { node = jsonMapper.readTree(it) }

            return JsonObject(node.toString())
        }
    }

    var testContext: VertxTestContext? = null

    @AfterEach
    fun tearDown() {
        if (testContext != null) {
            testContext!!.completeNow()
        }
    }

    @Test
    @Throws(IOException::class)
    fun getJavaResource() {
        val jsonMapper = ObjectMapper()
        var node: JsonNode?
        val path = Paths.get("src", "test",  "kotlin", "resources").toFile().absolutePath;
        val bufferedReader: BufferedReader = File("$path/application-conf.json").bufferedReader()
        bufferedReader.use { node = jsonMapper.readTree(it) }
        assertNotEquals(node, null)
        assertNotNull(node, "should find resource file.")
    }

    val vertx: Vertx? = Vertx.vertx()
    /*
        This is not working, not connecting to the gRPC router??
     */
//    @Test
    @Timeout(2, unit = TimeUnit.SECONDS)
    fun `gRpc client request-response is successful`(): Unit =
        runBlocking(vertx?.delegate!!.dispatcher()) {
            val port: Int = getAlternateConfig().getInteger("grpc:port")
            val host = "localhost"
            val options: HttpClientOptions = HttpClientOptions();
            options.protocolVersion = HttpVersion.HTTP_1_1
            val client: GrpcIoClient = GrpcIoClient.client(vertx.delegate) // , options)
            val server: SocketAddress = SocketAddress.inetSocketAddress(port, host)

            var cmd: Int? = 0
            val whenDone: Promise<Void> = Promise.promise()

            val channel = GrpcIoClientChannel(client, server)
            val handicapRequest = HandicapIndexGrpc.newStub(channel).withDeadlineAfter(2, TimeUnit.SECONDS)

            val observer: StreamObserver<HandicapData?> = object : StreamObserver<HandicapData?> {
                override fun onNext(value: HandicapData?) {
                    cmd = value?.cmd
                }

                override fun onCompleted() {
                    whenDone.complete()
                }

                override fun onError(t: Throwable?) {
                    logger.info("Calling GrpcIo Error: {}", t?.cause?.message)
                    whenDone.complete()
//                    throw(RuntimeException(t))
                }
            }

            val jsonObject = JsonObject()
                    .put("pin", "pet1234")
                    .put("firstName", "Ace")
                    .put("lastName", "Ventura")
                    .put("state", "NV")
                    .put("country", "US")
                    .put("lastLogin", 0L)

            handicapRequest.getGolfer(HandicapSetup.newBuilder()
                .setJson(jsonObject.toString())
                        .setCmd(3)
                        .setMessage("Test")
                        .build(), observer)

            whenDone.future().coAwait()

            assertEquals(3, cmd, "Returned cmd value from response")
        }
}
