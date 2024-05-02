import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;



public class TCPend 
{
	public static void main(String[] args)
	{
		if(args.length == 12)
		{
			int port = 40000;
			String remoteIP = "";
			int remotePort = 0;
			String fileName = "";
			int mtu = 0;
			int sws = 0;  // sliding window size
			System.out.println("Client");

			for(int i = 0; i < args.length; i++)
			{
				if(args[i].equals("-p"))
					port = Integer.parseInt(args[++i]);
				else if(args[i].equals("-s"))
					remoteIP = args[++i];
				else if(args[i].equals("-a"))
					remotePort = Integer.parseInt(args[++i]);
				else if(args[i].equals("-f"))
					fileName = args[++i];
				else if(args[i].equals("-m"))
					mtu = Integer.parseInt(args[++i]);
				else if(args[i].equals("-c"))
					sws = Integer.parseInt(args[++i]);
				else
				{
					System.out.println("Invalid argument " + args[i] + " at " + ++i);
					return;
				}
			}
			try
			{
				Client client = new Client(remoteIP, remotePort, port, mtu, sws, fileName);
				client.sendFile();
				client.close();
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
		else if(args.length == 8)
		{
			System.out.println("Server");
			int port = 40000;
			int mtu = 0;
			int sws = 0;
			String fileName = "";
			for(int i = 0; i < args.length; i++)
			{
				if(args[i].equals("-p"))
					port = Integer.parseInt(args[++i]);
				else if(args[i].equals("-m"))
					mtu = Integer.parseInt(args[++i]);
				else if(args[i].equals("-c"))
					sws = Integer.parseInt(args[++i]);
				else if(args[i].equals("-f"))
					fileName = args[++i];
				else
				{
					System.out.println("Invalid argument " + args[i] + " at " + ++i);
					return;
				}
			}
			try
			{
				Server server = new Server(port, mtu, sws, fileName);
				server.listen();
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
		else
		{
			System.out.println("Usage:\n\tClient: java TCPend -p <port> -s <remote_IP> -a <remote_port> -f <file_name> -m <mtu> -c <sws>\n\tServer: java TCPend -p <port> -m <mtu> -c <sws> -f <file_name>");
		}
	}

}
