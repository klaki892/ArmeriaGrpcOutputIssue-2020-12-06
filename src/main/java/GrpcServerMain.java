import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Klayton Killough
 * Date: 12/5/2020
 */
public class GrpcServerMain {


    public static void main(String[] args) {
        HashMap<String, LinkedBlockingQueue<Boolean>> queueMap = new HashMap<>();

        Server server = ServerBuilder.forPort(ArmeriaServerMain.PORT)
                .addService(new RelayTimeService(queueMap))
                .build();

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

        try {
            server.start();
            System.out.println("Server started, listening on " + ArmeriaServerMain.PORT);
            server.awaitTermination();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            System.err.println(e);
            System.exit(-1);
        }

    }
}
