package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{
	// A map to keep track of the MAC address table 
	// key is the MAC address, value is <interface, timestamp>
        private Map<MACAddress, Pair<Iface, Long>> macTable;

	// helper class to store the interface and timestamp as a pair
	private class Pair<A,B>
	{
		private A first;
		private B second;

		public Pair(A first, B second)
		{
			this.first = first;
			this.second = second;
		}
		public A getFirst() {return first;}
		public B getSecond() {return second;}
	}
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
		macTable = new HashMap<MACAddress, Pair<Iface, Long>>();
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
		MACAddress srcMAC = etherPacket.getSourceMAC();
		this.macTable.put(srcMAC, new Pair<>(inIface, System.currentTimeMillis()));

		long currentTime = System.currentTimeMillis();
		Iterator<Map.Entry<MACAddress, Pair<Iface, Long>>> it = this.macTable.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<MACAddress, Pair<Iface, Long>> entry = it.next();
			if(currentTime - entry.getValue().getSecond() > 15000) {
			   it.remove();
			}
		}

		MACAddress destMac = etherPacket.getDestinationMAC();
		Pair<Iface, Long> destPair = macTable.get(destMac);
		// if the destination MAC is in the MAC table
		if(destPair != null) {
			
		   // send it out the associated interface
		   sendPacket(etherPacket, destPair.getFirst());
	        } else {
		   // MAC is unknown, broadcast the packet out except the interface it comes from
		   for(Iface iface : interfaces.values()) {
			   if(!iface.equals(inIface)) {
			   sendPacket(etherPacket, iface);
			}
		   }
		}
	
	  }
	}
