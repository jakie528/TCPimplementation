import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class Server
{
    private DatagramSocket socket;
    private int mtu;
    private int sws;
    private File file;
    private Map<Integer, Integer> clientStates; // Maps client identifier to the next seq_no
    private FileOutputStream fos;

    public Server(int port, int mtu, int sws, String fileName) throws Exception {
        socket = new DatagramSocket(port);
        clientStates = new HashMap<>();
	this.mtu = mtu;
	this.sws = sws;
	this.file = new File(fileName);
	this.fos = new FileOutputStream(fileName, true);
    }
    private void handlePacket(DatagramPacket packet) throws Exception {
        Packet receivePacket = Packet.deserialize(packet.getData());
        if (receivePacket == null) return;

        int clientKey = getClientKey(packet.getAddress(), packet.getPort());
        int expectedSeqNo = clientStates.getOrDefault(clientKey, 0);

        if (receivePacket.isSyn() && !receivePacket.isAck()) {
            System.out.println("SYN received, sending SYN-ACK...");
            sendSynAck(packet.getAddress(), packet.getPort());
        } else if (receivePacket.isAck()) {
            System.out.println("ACK received, handshake complete.");
            clientStates.put(clientKey, receivePacket.getSeqNo() + 1); // Update the sequence number
        } else if (receivePacket.getSeqNo() >= expectedSeqNo) {
            clientStates.put(clientKey, receivePacket.getSeqNo() + receivePacket.getPayload().length);
            processPayload(receivePacket.getPayload());
            receivePacket.logPacket("rcv", System.nanoTime());
            fos.write(packet.getData(), 0, packet.getLength());
            sendAck(packet.getAddress(), packet.getPort(), expectedSeqNo);
        }
    }

    private void sendSynAck(InetAddress address, int port) throws Exception {
        Packet synAckPacket = new Packet(0, 0, true, true, false, new byte[0], 0);
        send(synAckPacket, address, port);
    }

    public void listen() throws Exception {
	try
	{
        	byte[] buffer = new byte[mtu];  // assuming a buffer size
        	while(true) {
        	    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        	    socket.receive(packet);
        	    handlePacket(packet);
        	}
	}
	finally
	{
		if(fos != null)
		{
			fos.close();
		}
	}
    }



    private void sendAck(InetAddress clientAddress, int clientPort, int ackNo) throws Exception {
        Packet ackPacket = new Packet(0, ackNo, true, false, false, new byte[0], 0);
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
        byte[] data = new byte[mtu];
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        socket.receive(receivePacket);
        Packet packet = Packet.deserialize(receivePacket.getData());
	if(packet != null)
		packet.logPacket("rcv", System.nanoTime());
	return packet;
    }


    public void close() {

        socket.close();
    }

}
