import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

/**
 * @author Klayton Killough
 * Date: 12/5/2020
 */
public class TimeListeningClient {

    private final ManagedChannel channel;
    private final RelayTimeServiceGrpc.RelayTimeServiceBlockingStub blockingStub;
    private final RelayTimeServiceGrpc.RelayTimeServiceStub asyncStub;

    private final static String ClientId = UUID.randomUUID().toString();

    public TimeListeningClient() {
        this(ManagedChannelBuilder.forAddress("localhost", ArmeriaServerMain.PORT).usePlaintext());
    }

    public TimeListeningClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = RelayTimeServiceGrpc.newBlockingStub(channel);
        asyncStub = RelayTimeServiceGrpc.newStub(channel);
    }

    public void getListener(StreamObserver<Relay.CurrentTimeMessage> observer) {
        asyncStub.listenForEvents(Relay.IdentificationMessage.newBuilder().setId(ClientId).build(), observer);
        System.out.println("our ID: " + ClientId);
    }

    public void sendReadyMessage() {
        blockingStub.readyUp(Relay.IdentificationMessage.newBuilder().setId(ClientId).build());
        System.out.println("Sent Ready Up");
    }

    public static void main(String[] args) {
        TimeListeningClient client = new TimeListeningClient();

        client.sendReadyMessage();

        final boolean[] weDone = {false};
        client.getListener(new StreamObserver<Relay.CurrentTimeMessage>() {
            @Override
            public void onNext(Relay.CurrentTimeMessage currentTimeMessage) {
                System.out.println("Time Gotten: " + currentTimeMessage.getTimeFromEpoch());
            }

            @Override
            public void onError(Throwable throwable) {
                weDone[0] = true;

            }

            @Override
            public void onCompleted() {
                weDone[0] = true;
                System.out.println("we done!");
                System.exit(1);

            }
        });
        while (!weDone[0]) {
            //do nothing
        }

    }
}
