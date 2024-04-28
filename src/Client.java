import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client
{
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    public Client(String address, int port) throws Exception {
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        socket = new DatagramSocket();
    }

    public void send(Packet packet) throws Exception {
        byte[] data = packet.serialize();
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, serverPort);
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
