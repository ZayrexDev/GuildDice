package guilddice.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZLibUtil {
    public static String decompressString(byte[] data) throws DataFormatException, IOException {
        byte[] result;

        Inflater inflater = new Inflater();
        inflater.reset();
        inflater.setInput(data);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];

            while (!inflater.finished()) {
                int i = inflater.inflate(buf);
                o.write(buf, 0, i);
            }

            result = o.toByteArray();
        }

        inflater.end();
        return new String(result);
    }
}
