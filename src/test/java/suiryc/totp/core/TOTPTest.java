package suiryc.totp.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TOTPTest {

    @Test
    public void generator() throws Exception {
        // base32("12345678901234567890")
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        // base32("12345678901234567890123456789012")
        String secret256 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA====";
        // base32("1234567890123456789012345678901234567890123456789012345678901234")
        String secret512 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA=";

        // See https://tools.ietf.org/html/rfc4226#page-32
        TOTP totp = new TOTP("SHA1", secret);
        assertEquals("755224", totp.generate(0));
        assertEquals("287082", totp.generate(1));
        assertEquals("359152", totp.generate(2));
        assertEquals("969429", totp.generate(3));
        assertEquals("338314", totp.generate(4));
        assertEquals("254676", totp.generate(5));
        assertEquals("287922", totp.generate(6));
        assertEquals("162583", totp.generate(7));
        assertEquals("399871", totp.generate(8));
        assertEquals("520489", totp.generate(9));
        // Some more values to check '0' padding on the left.
        assertEquals("026920", totp.generate(30));
        assertEquals("003784", totp.generate(36));

        // See https://tools.ietf.org/html/rfc6238#appendix-B
        // See https://www.rfc-editor.org/errata_search.php?rfc=6238
        totp = new TOTP("SHA1", secret, "HMACSHA1", 8, new TimeInterval(30));
        assertEquals("94287082", totp.generate(1));
        assertEquals("07081804", totp.generate(37037036));
        assertEquals("14050471", totp.generate(37037037));
        assertEquals("89005924", totp.generate(41152263));
        assertEquals("69279037", totp.generate(66666666));
        assertEquals("65353130", totp.generate(666666666));

        totp = new TOTP("SHA256", secret256, "HMACSHA256", 8, new TimeInterval(30));
        assertEquals("46119246", totp.generate(1));
        assertEquals("68084774", totp.generate(37037036));
        assertEquals("67062674", totp.generate(37037037));
        assertEquals("91819424", totp.generate(41152263));
        assertEquals("90698825", totp.generate(66666666));
        assertEquals("77737706", totp.generate(666666666));

        totp = new TOTP("SHA512", secret512, "HMACSHA512", 8, new TimeInterval(30));
        assertEquals("90693936", totp.generate(1));
        assertEquals("25091201", totp.generate(37037036));
        assertEquals("99943326", totp.generate(37037037));
        assertEquals("93441116", totp.generate(41152263));
        assertEquals("38618901", totp.generate(66666666));
        assertEquals("47863826", totp.generate(666666666));
    }

}
