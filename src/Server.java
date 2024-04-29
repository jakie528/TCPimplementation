import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Server
{
    private DatagramSocket socket;
    private Map<Integer, Integer> clientStates; // Maps client identifier to the next seq_no

    public Server(int port) throws Exception {
        socket = new DatagramSocket(port);
        clientStates = new HashMap<>();
    }

    public void listen() throws Exception {
        byte[] buffer = new byte[1024];  // assuming a buffer size
        while(true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            handlePacket(packet);
        }
    }

    private void handlePacket(DatagramPacket packet) throws Exception {
        Packet receivePacket = Packet.deserialize(packet.getData());
        if(receivePacket == null) return;
        int clientKey = getClientKey(packet.getAddress(), packet.getPort());
        int expectedSeqNo = clientStates.getOrDefault(clientKey, 0);

        // if received packet is the expected one
        if(receivePacket.getSeqNo() == expectedSeqNo) {
            clientStates.put(clientKey, expectedSeqNo + receivePacket.getPayload().length);
            processPayload(receivePacket.getPayload());
        }
        // Send ACK for the expected sequence number, regardless of packet order
        sendAck(packet.getAddress(), packet.getPort(), expectedSeqNo);
    }

    private void sendAck(InetAddress clientAddress, int clientPort, int ackNo) throws Exception {
        Packet ackPacket = new Packet(0, ackNo, true, false, false, new byte[0]);
        byte[] ackData = ackPacket.serialize();
        DatagramPacket ack = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
        socket.send(ack);
    }
    private void processPayload(byte[] payload) {
        System.out.println("Received data: " + new String(payload));
    }

    private int getClientKey(InetAddress address, int port) {
        return address.hashCode() + port;
    }
    public void send(Packet packet, InetAddress address, int port) throws Exception {
        byte[] data = packet.serialize();
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
        socket.send(sendPacket);
    }

    public Packet receive() throws Exception {
        byte[] data = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        socket.receive(receivePacket);
        return Packet.deserialize(receivePacket.getData());
    }


    public void close() {

        socket.close();
    }

}
