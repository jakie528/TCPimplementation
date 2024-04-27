public class Packet
{
	private int seq_no, ack_no;
	private boolean ack, syn, fin;
	private byte[] payload;
	private int checksum;

	public Packet(int seq_no, int ack_no, boolean ack, boolean syn, boolean fin, byte[] payload)
	{
		this.seq_no = seq_no;
		this.ack_no = ack_no;
		this.ack = ack;
		this.syn = syn;
		this.fin = fin;
		this.payload = payload;
		this.checkSum = calcChecksum();
	}

	private short calcChecksum()
	{
		int checksum = seq_no + ack_no;
		if(ack)
			checksum++;
		if(syn)
			checksum++;
		if(fin)
			checksum++;
		for(byte b : payload)
			checksum += b;
		return checksum;
	}

	public int getSeqNo() { return seq_no; }
	public int getAckNo() { return ack_no; }
	public boolean isAck() { return ack; }
	public boolean isSyn() { return syn; }
	public boolean is Fin() { return fin; }
	public byte[] getPayload() { return payload; }
	public int getChecksum() { return checksum; }

	public byte[] serialize()
	{
		
	}

	public static Packet deserialize(byte[] data)
	{
		
	}

}
