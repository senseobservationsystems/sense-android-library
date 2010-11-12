package nl.sense_os.service.external_sensors;

//The generator is x^8 + x^2 + x + 1
//The only method is checksum which takes a byte array terminated
//by a null byte and returns the checksum of the array.

class CRC8   {
	public static byte checksum(byte[] data)   {
		short _register = 0;
		short bitMask = 0;
		short poly = 0;
		_register = data[0];
		
		for (int i=1; i<data.length; i++)  {
			_register = (short)((_register << 8) | (data[i] & 0x00ff));
			poly = (short)(0x0107 << 7);
			bitMask = (short)0x8000;

			while (bitMask != 0x0080)  {
				if ((_register & bitMask) != 0) {
					_register ^= poly;
				}
				poly = (short) ((poly&0x0000ffff) >>> 1);
				bitMask = (short)((bitMask&0x0000ffff) >>> 1);
			}  //end while
		}  //end for
		return (byte)_register;
	}
	
	public static byte checksum2(byte[] data)   {
		int crc = 0;
		
		for (int i = 0; i < data.length; i++)  {
			crc ^= data[i];
			
			for(int x = 0; x < 8; x++)
			{
				if((crc & 1) != 0)
				{
					crc = (crc >> 1) ^ 0x8c;
				}
				else
					crc = (crc >> 1);
			}
			
		}  //end for
		return (byte)crc;
	}
}