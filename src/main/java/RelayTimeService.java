import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Klayton Killough
 * Date: 12/5/2020
 */
public class RelayTimeService extends RelayTimeServiceGrpc.RelayTimeServiceImplBase {


    private final HashMap<String, LinkedBlockingQueue<Boolean>> idToQueueMap;

    public RelayTimeService(HashMap<String, LinkedBlockingQueue<Boolean>> idToQueueMap) {
        this.idToQueueMap = idToQueueMap;
    }

    @Override
    public void readyUp(Relay.IdentificationMessage request, StreamObserver<Relay.IdentificationMessage> responseObserver) {
        idToQueueMap.put(request.getId(), new LinkedBlockingQueue<>());
        System.out.println(request.getId() + " Connected!");
        responseObserver.onNext(Relay.IdentificationMessage.newBuilder().setId(request.getId()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void listenForEvents(Relay.IdentificationMessage request, StreamObserver<Relay.CurrentTimeMessage> responseObserver) {
        int messagesSent = 0;

        while (true) {
            try {
                Boolean canSendMessage = idToQueueMap.get(request.getId()).poll(30, TimeUnit.SECONDS);
                if (canSendMessage != null && canSendMessage) {
                    System.out.println("Sent messsage to: " + request.getId());
                    responseObserver.onNext(Relay.CurrentTimeMessage.newBuilder().setTimeFromEpoch(System.currentTimeMillis()).build());
                    messagesSent++;
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ex) {
                ex.printStackTrace();
                responseObserver.onCompleted();
                return;
            }
        }
    }
}
