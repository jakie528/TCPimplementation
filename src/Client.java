import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.*;
import java.util.*;

public class Client
{
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;


    private int mtu;
    private int sws; // Sliding window size
    private Map<Integer, Packet> sentPackets;
    private int maxRetransmissions = 16;

    public Client(String address, int port, int mtu, int sws) throws Exception {
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        this.mtu = mtu;
        this.sws = sws;
        this.sentPackets = new HashMap<>();
        socket = new DatagramSocket();
        socket.setSoTimeout(1000);  // timeout
    }

    public void send(byte[] data) throws Exception {
        int sequenceNumber = 0;
        for(int i = 0; i < data.length; i += mtu) {
            int end = Math.min(data.length, i+ mtu);
            byte[] payload = Arrays.copyOfRange(data, i, end);
            Packet packet = new Packet(sequenceNumber, 0, false, false, false, payload);
            sendPacket(packet);
            sequenceNumber += payload.length;

        }
    }

    public void sendPacket(Packet packet) throws Exception {
        sentPackets.put(packet.getSeqNo(), packet);
        int attempts = 0;
        while(attempts < maxRetransmissions) {
            byte[] serilizedPacket = packet.serialize();
            DatagramPacket udpPacket = new DatagramPacket(serilizedPacket, serilizedPacket.length, serverAddress, serverPort);
            socket.send(udpPacket);
            try {
                receiveAck();
                break; // ACK received
            } catch (SocketTimeoutException e) {
                attempts++;
                System.out.println("Retransmitting packet: " + packet.getSeqNo());
            }
        }
        if(attempts == maxRetransmissions) {
            System.out.println("Failed to receive ACK after maximum attempts: " + packet.getSeqNo());
        }
    }

    public void receiveAck() throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        Packet ackPacket = Packet.deserialize(packet.getData());
        if(ackPacket!= null && ackPacket.isAck()) {
            sentPackets.remove(ackPacket.getAckNo());
        }
    }

    public void close() {

        socket.close();
    }
}
