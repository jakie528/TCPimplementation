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
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 5 + payload.length);
		buffer.putInt(seq_no);
		buffer.putInt(ack_no);
		buffer.put((byte) (ack ? 1 : 0));
		buffer.put((byte) (syn ? 1 : 0));
		buffer.put((byte) (fin ? 1 : 0));
		buffer.put(payload);
		buffer.putInt(checkSum);
		return buffer.array();
	}

	public static Packet deserialize(byte[] data)
	{
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int seq_no = buffer.getInt();
		int ack_no = buffer.getInt();
		boolean ack = buffer.get() != 0;
		boolean syn = buffer.get() != 0;
		boolean fin = buffer.get() != 0;
		byte[] payload = new byte[data.length - Integer.BYTES * 5];
		buffer.get(payload);
		int checkSum = buffer.getInt();
		Packet Packet = new Packet(seq_no, ack_no, ack, syn, fin, payload);
		if(packet.calcChecksum() == checkSum) {
			return packet;
		} else {
			return null;  // handle
		}
	}
}
