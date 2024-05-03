import java.nio.ByteBuffer;
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
		this.checksum = calcChecksum();
	}

	private int calcChecksum()
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
	public boolean isFin() { return fin; }
	public byte[] getPayload() { return payload; }
	public int getChecksum() { return checksum; }

	public byte[] serialize()
	{
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 5 + payload.length);
		buffer.putInt(seq_no);
		buffer.putInt(ack_no);
		buffer.putInt(checksum);
		buffer.put((byte) (ack ? 1 : 0));
		buffer.put((byte) (syn ? 1 : 0));
		buffer.put((byte) (fin ? 1 : 0));
		buffer.put(payload);
		return buffer.array();
	}

	public static Packet deserialize(byte[] data)
	{
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int seq_no = buffer.getInt();
		int ack_no = buffer.getInt();
		int checkSum = buffer.getInt();
		boolean ack = buffer.get() != 0;
		boolean syn = buffer.get() != 0;
		boolean fin = buffer.get() != 0;
		byte[] payload = new byte[data.length - Integer.BYTES * 5];
		buffer.get(payload);
		Packet packet = new Packet(seq_no, ack_no, ack, syn, fin, payload);
		if(packet.calcChecksum() == checkSum) {
			return packet;
		} else {
			
			return null;  // handle
		}
	}
	
	public void logPacket(String action, long time)
	{
		System.out.printf("%s %.3f %c %c %c %c %d %d %d\n", action, time/1e9, 
				this.isSyn() ? 'S' : '-',
				this.isAck() ? 'A' : '-',
				this.isFin() ? 'F' : '-',
				this.getPayload().length > 0 ? 'D' : '-',
				this.getSeqNo(),
				this.getPayload().length,
				this.getAckNo());
	}

}
