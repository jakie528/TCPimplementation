import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.*;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;

public class Client
{
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int port;
    private File file;

    private int mtu;
    private int sws; // Sliding window size
    private Map<Integer, Packet> sentPackets;
    private Map<Integer, Long> sendTimes; // To store send times for RTT calculation
    private int maxRetransmissions = 16;

    // RTT and Timeout Management
    private double estimatedRTT = 5000000000.0; // Initial RTT (5 seconds in nanoseconds)
    private double devRTT = 0.0;
    private double timeoutInterval = estimatedRTT * 2;

    private int numDataReceived = 0;
    private int numDataTransferred = 0;
    private int numPacketsSent = 0;
    private int numPacketsReceived = 0;
    private int numOutOfSeqPackets = 0;
    private int numChecksumPackets = 0;
    private int numRetransmissions = 0;
    private int numDuplicateAcks = 0;

    private int ackNo = 0;

    public Client(String address, int serverPort, int port, int mtu, int sws, String file) throws Exception {
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = serverPort;
	this.port = port;
        this.mtu = mtu;
        this.sws = sws;
        this.sentPackets = new HashMap<>();
        this.sendTimes = new HashMap<>();
	this.file = new File(file);
        socket = new DatagramSocket(port);
        updateSocketTimeout();
    }
    public void startHandshake() throws Exception {
        Packet synPacket = new Packet(0,0,false,true,false,new byte[0],0);
        sendPacket(synPacket);
        // wait for SYN-ACK
        // Send ACK
        Packet ackPacket = new Packet(1,1,true, false, false, new byte[0], 0);
        sendPacket(ackPacket);
    }



    public boolean receiveSynAck() throws Exception {
        DatagramPacket packet = new DatagramPacket(new byte[mtu], mtu);
        try {
            socket.receive(packet);
            Packet receivedPaket = Packet.deserialize(packet.getData());
	    receivedPaket.logPacket("rcv", System.nanoTime());
            if(receivedPaket != null && receivedPaket.isAck() && receivedPaket.isSyn()) {
		    ackNo = receivedPaket.getAckNo();
		    numPacketsReceived++;
                return true;
            }
        } catch (SocketTimeoutException e) {
            return false;
        }
        return false;
}

    private void updateSocketTimeout() throws SocketException {
        socket.setSoTimeout((int) (timeoutInterval / 1000000)); // Convert nanoseconds to milliseconds
    }

    public int send(byte[] data) throws Exception {
        int sequenceNumber = 0;
        for(int i = 0; i < data.length; i += mtu) {
            int end = Math.min(data.length, i+ mtu);
            byte[] payload = Arrays.copyOfRange(data, i, end);
	    sequenceNumber++;
            Packet packet = new Packet(sequenceNumber, ackNo, true, false, false, payload, payload.length);
            sendPacket(packet);
            sequenceNumber += payload.length;

        }
	return sequenceNumber;
    }

    public void sendFile() throws Exception
    {
	FileInputStream fis = new FileInputStream(file);
	byte[] buffer = new byte[mtu];
	int bytesRead;
	int sequenceNumber = -1;
	startHandshake();
	while((bytesRead = fis.read(buffer)) > 0)
	{
		sequenceNumber = send(Arrays.copyOf(buffer, bytesRead));
	}
	//Packet finPacket = new Packet(sequenceNumber, 0, false, false, true, new byte[0]);
	//sendPacket(finPacket);
	fis.close();
    }	    

    public void sendPacket(Packet packet) throws Exception {
        sentPackets.put(packet.getSeqNo(), packet);
        byte[] serializedPacket = packet.serialize();
	/*if(serializedPacket.length > mtu)
	{
		splitPacket(packet);
		return;
	}
	else*/
		sendData(packet);
        // Handle retransmissions
		if(packet.isAck() && packet.getPayload().length == 0)
			return ;
        int attempts = 0;
        while (attempts < maxRetransmissions) {
            try {
		if(packet.isSyn())
		{
        		if(receiveSynAck()) 
				break;
		}
		
                if (receiveAck(packet.getSeqNo())) {
                    break; // ACK received
                }
            } catch (SocketTimeoutException e) {
                attempts++;
		        sendData(packet);
		}
        }
        if (attempts == maxRetransmissions) {
            System.out.println("Failed to receive ACK after maximum attempts: " + packet.getSeqNo());
	    System.exit(1);
        }
	numRetransmissions += attempts;
	
    }

	private void splitPacket(Packet packet) throws Exception
	{
		byte[] payload = packet.getPayload();
		int header = 20;
		int maxSize = mtu - header;
		int length = payload.length;
		int offset = 0;


		while(offset < length)
		{
			int chunkLength = Math.min(maxSize, length - offset);
			byte[] packetChunk = new byte[chunkLength];
			System.arraycopy(payload, offset, packetChunk, 0, chunkLength);
			offset += chunkLength;

			Packet chunk = new Packet(packet.getSeqNo() + offset, 0, false, false ,false, packetChunk, chunkLength); 
			sendData(chunk);
		}
	}
    public boolean receiveAck(int originalSeqNo) throws Exception {
        byte[] buffer = new byte[mtu];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        Packet ackPacket = Packet.deserialize(packet.getData());
	if(ackPacket == null)
	{
		numChecksumPackets++;
		return false;
	}
        if(ackPacket!= null && ackPacket.isAck() && ackPacket.getAckNo() == originalSeqNo) {
	    ackPacket.logPacket("rcv", sendTimes.get(ackPacket.getSeqNo()));
            long sendTime = sendTimes.remove(originalSeqNo);
            long rtt = System.nanoTime() - sendTime;
            updateRTT(rtt);
            sentPackets.remove(originalSeqNo);
	    ackNo = ackPacket.getAckNo();
	    numPacketsReceived++;
            return true;
        }
	numDuplicateAcks++;
        return false;
    }

    public void sendData(Packet packet) throws Exception
    {
	byte[] serialized = packet.serialize();
	DatagramPacket udpPacket = new DatagramPacket(serialized, serialized.length, serverAddress, serverPort);
	socket.send(udpPacket);
	sendTimes.put(packet.getSeqNo(), System.nanoTime()); // Store send time for RTT calculation
	packet.logPacket("snd", sendTimes.get(packet.getSeqNo()));
	numDataTransferred += packet.getPayload().length;
	numPacketsSent++;
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
	System.out.println("Amount of Data transferred: " + numDataTransferred + 
			"\nAmount of Data received: " + numDataReceived + 
			"\nAmount of packets sent: " + numPacketsSent + 
			"\nAmount of packets received: " + numPacketsReceived + 
			"\nNumber of out-of-sequence packets discarded: " + numOutOfSeqPackets + 
			"\nNumber of packets discarded due to incorrect checksum: " + numChecksumPackets + 
			"\nNumber of retransmissions: " + numRetransmissions + 
			"\nNumber of duplicate acknowledgement: " + numDuplicateAcks);
        socket.close();

    }
}
