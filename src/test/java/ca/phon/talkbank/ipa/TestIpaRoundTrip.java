package ca.phon.talkbank.ipa;

import ca.phon.ipa.IPATranscript;
import ca.phon.syllabifier.Syllabifier;
import ca.phon.syllabifier.SyllabifierLibrary;
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
            final Syllabifier syllabifier = SyllabifierLibrary.getInstance().getSyllabifierForLanguage("eng");
            while((line = in.readLine()) != null) {
                final String splitLine[] = line.trim().split("\\s+");
                if(splitLine.length == 2) {
                    final String ipa = splitLine[1];
                    final IPATranscript ipaTranscript = IPATranscript.parseIPATranscript(ipa);
                    syllabifier.syllabify(ipaTranscript.toList());
                    final PhoneticTranscriptionType pho = IpaUtil.ipaToPhoneticTranscription(ipaTranscript);
                    final IPATranscript ipaTranscript2 = IpaUtil.phoneticTranscriptionToIpaTranscript(pho);
                    Assert.assertEquals(ipaTranscript.toString(true), ipaTranscript2.toString(true));
                }
            }
        }
    }

}
