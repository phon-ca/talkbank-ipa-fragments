package ca.phon.talkbank.ipa;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.talkbank.ns.talkbank.PhoneticTranscriptionType;

import java.io.*;
import java.text.ParseException;

@RunWith(JUnit4.class)
public class TestIpaRoundTrip {

    private final String TESTFILE = "testipa.txt";

    @Test
    public void testRoundTrip() throws IOException, ParseException {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(TESTFILE)))) {
            String line = null;
            while((line = in.readLine()) != null) {
                final String ipa = line.trim();
                final PhoneticTranscriptionType pho = IpaUtil.ipaToPhoneticTranscription(ipa);
                final String ipa2 = IpaUtil.phoneticTranscriptionToIpa(pho);
                Assert.assertEquals(ipa, ipa2);
            }
        }
    }

}
