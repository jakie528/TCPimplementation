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
    private Map<Integer, Long> sendTimes; // To store send times for RTT calculation
    private int maxRetransmissions = 16;

    // RTT and Timeout Management
    private double estimatedRTT = 5000000000.0; // Initial RTT (5 seconds in nanoseconds)
    private double devRTT = 0.0;
    private double timeoutInterval = estimatedRTT * 2;


    public Client(String address, int port, int mtu, int sws) throws Exception {
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        this.mtu = mtu;
        this.sws = sws;
        this.sentPackets = new HashMap<>();
        this.sendTimes = new HashMap<>();
        socket = new DatagramSocket();
        updateSocketTimeout();
    }

    private void updateSocketTimeout() throws SocketException {
        socket.setSoTimeout((int) (timeoutInterval / 1000000)); // Convert nanoseconds to milliseconds
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
        byte[] serializedPacket = packet.serialize();
        DatagramPacket udpPacket = new DatagramPacket(serializedPacket, serializedPacket.length, serverAddress, serverPort);
        socket.send(udpPacket);
        sendTimes.put(packet.getSeqNo(), System.nanoTime()); // Store send time for RTT calculation

        // Handle retransmissions
        int attempts = 0;
        while (attempts < maxRetransmissions) {
            try {
                if (receiveAck(packet.getSeqNo())) {
                    break; // ACK received
                }
            } catch (SocketTimeoutException e) {
                attempts++;
                System.out.println("Retransmitting packet: " + packet.getSeqNo());
                socket.send(udpPacket); // Retransmit packet
            }
        }
        if (attempts == maxRetransmissions) {
            System.out.println("Failed to receive ACK after maximum attempts: " + packet.getSeqNo());
        }
    }


    public boolean receiveAck(int originalSeqNo) throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        Packet ackPacket = Packet.deserialize(packet.getData());
        if(ackPacket!= null && ackPacket.isAck() && ackPacket.getAckNo() == originalSeqNo) {
            long sendTime = sendTimes.remove(originalSeqNo);
            long rtt = System.nanoTime() - sendTime;
            updateRTT(rtt);
            sentPackets.remove(originalSeqNo);
            return true;
        }
        return false;
    }

    private void updateRTT(long rttSample) throws SocketException {
        if(devRTT == 0) {
            estimatedRTT = rttSample;
            devRTT = rttSample / 2;
        } else {
            devRTT = 0.75 * devRTT + 0.25 * Math.abs(rttSample - estimatedRTT);
            estimatedRTT = 0.875 * estimatedRTT + 0.125 * rttSample;
        }
        timeoutInterval = estimatedRTT + 4 * devRTT;
        updateSocketTimeout();
    }

    public void close() {

        socket.close();
    }
}
