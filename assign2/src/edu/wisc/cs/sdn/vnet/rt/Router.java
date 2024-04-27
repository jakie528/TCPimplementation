package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;


/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */
		//if the type is not IPv4 drop the packet
		if(etherPacket.getEtherType() != etherPacket.TYPE_IPv4)
			return;
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
		int ipChecksum = ipPacket.getChecksum();
		ipPacket.resetChecksum();

		byte[] serializedIp = ipPacket.serialize();
	        if(ipChecksum != ipPacket.getChecksum())
	       		return;	       
		
		ipPacket.setTtl((byte)(ipPacket.getTtl() - 1));
		if(ipPacket.getTtl() == 0)
			return;

		ipPacket.resetChecksum();
		serializedIp = ipPacket.serialize();


		RouteEntry longestPrefix = this.routeTable.lookup(ipPacket.getDestinationAddress());
		if(longestPrefix == null)
			return;

		Iface outIface = interfaces.get(longestPrefix.getInterface().getName());
		if(outIface == null)
			return;
		int nextHop;
		if(longestPrefix.getGatewayAddress() == 0)
			nextHop = ipPacket.getDestinationAddress();
		else
			nextHop = longestPrefix.getGatewayAddress();
		
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
		if(arpEntry == null)
			return;

		etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
		etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());
		this.sendPacket(etherPacket, outIface);
		/********************************************************************/
		System.out.println(longestPrefix.getInterface().getName());
	}
}
