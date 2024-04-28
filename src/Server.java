import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Server
{
    private DatagramSocket socket;

    public Server(int port) throws Exception {
        socket = new DatagramSocket(port);
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
