package suiryc.totp.core;

import java.nio.charset.StandardCharsets;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Base32DecoderTest {

    @Test
    public void decoder() throws Exception {
        // See https://tools.ietf.org/html/rfc4648#section-10
        assertEquals("", decode(""));
        assertEquals("f", decode("MY======"));
        assertEquals("fo", decode("MZXQ===="));
        assertEquals("foo", decode("MZXW6==="));
        assertEquals("foob", decode("MZXW6YQ="));
        assertEquals("fooba", decode("MZXW6YTB"));
        assertEquals("foobar", decode("MZXW6YTBOI======"));
    }

    private String decode(String encoded) throws Exception {
        return new String(Base32Decoder.decode(encoded), StandardCharsets.US_ASCII);
    }

}
