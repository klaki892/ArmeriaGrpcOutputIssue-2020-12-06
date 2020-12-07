import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.common.logging.LogLevel;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.cors.CorsServiceBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder;
import com.linecorp.armeria.server.logging.AccessLogWriter;
import com.linecorp.armeria.server.logging.LoggingService;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Klayton Killough
 * Date: 12/5/2020
 */
public class ArmeriaServerMain {

    public static final int PORT = 32768;

    public static void main(String[] args) {


        //start the server
        ServerBuilder sb = Server.builder();
        //configurations
        sb.http(PORT);
        sb.accessLogWriter(AccessLogWriter.combined(), true);


        HashMap<String, LinkedBlockingQueue<Boolean>> queueMap = new HashMap<>();

        GrpcServiceBuilder grpcGameService = GrpcService.builder()
                .addService(new RelayTimeService(queueMap));
        //support GRPC-WEB and JSON formats
        grpcGameService.supportedSerializationFormats(GrpcSerializationFormats.values());
        grpcGameService.useBlockingTaskExecutor(true);

        //handle CORS
        CorsServiceBuilder corsBuilder = CorsService.builderForAnyOrigin()
                .allowRequestMethods(HttpMethod.POST)
                .allowRequestHeaders(HttpHeaderNames.CONTENT_TYPE,
                        HttpHeaderNames.of("X-GRPC-WEB"))
                // Expose trailers of the HTTP response to the client.
                .exposeHeaders("Grpc-Status", "Grpc-Message", "Grpc-Encoding", "Grpc-Accept-Encoding");

        //add services and build server
        sb.service(grpcGameService.build(), corsBuilder.newDecorator());

        //enable debugging
        sb.decorator(LoggingService.builder()
                .successfulResponseLogLevel(LogLevel.DEBUG)
                .failureResponseLogLevel(LogLevel.ERROR)
                .newDecorator());
        grpcGameService.addService(ProtoReflectionService.newInstance());


        Server server = sb.build();
        CompletableFuture<Void> future = server.start();
        //wait for server to finish starting up
        future.join();
        System.out.println("Server finished starting up - " + server.defaultHostname() + ":" + server.activeLocalPort());

        //ask the server console who to queue messages for
        new Thread(() -> {
            while (true) {
                Scanner in = new Scanner(System.in);
                System.out.println("Enter ID to queue a message: ");
                String idToQueue = in.nextLine();
                if (queueMap.containsKey(idToQueue.trim())) {
                    queueMap.get(idToQueue).add(true);
                    System.out.println("Queued!");
                } else if (idToQueue.trim().equalsIgnoreCase("all")) {
                    queueMap.forEach((s, booleans) -> booleans.add(true));
                    System.out.println("Queued!");

                } else {
                    System.out.println("ID didnt exist");
                }
            }
        }).start();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server Shutting down");
            server.stop();
        }));
    }
}
