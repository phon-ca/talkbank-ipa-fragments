package ca.phon.talkbank.ipa;

import ca.phon.ipa.IPATranscript;
import jakarta.xml.bind.JAXBElement;
import org.talkbank.ns.talkbank.ObjectFactory;
import org.talkbank.ns.talkbank.PhoneticTranscriptionType;

import java.text.ParseException;

/**
 * Utility class for converting IPA strings to XML elements and vice versa.
 */
public class IpaUtil {

    // region ipa2xml
    /**
     * Convert ipa string into an xml fragment.
     *
     * @param ipa
     * @return phonetic transcription JAXB type
     * @throws java.text.ParseException if the ipa string is invalid
     */
    public static PhoneticTranscriptionType ipaToPhoneticTranscription(String ipa) throws ParseException {
        final IPATranscript ipaTranscript = IPATranscript.parseIPATranscript(ipa);
        final IpaToXmlVisitor visitor = new IpaToXmlVisitor();
        ipaTranscript.accept(visitor);
        return visitor.getPho();
    }

    /**
     * Convert ipa string into a mod element.
     *
     * @param ipa
     * @return mod element
     * @throws java.text.ParseException if the ipa string is invalid
     */
    public static JAXBElement<PhoneticTranscriptionType> ipaToMod(String ipa) throws ParseException {
        final ObjectFactory factory = new ObjectFactory();
        final PhoneticTranscriptionType pho = ipaToPhoneticTranscription(ipa);
        return factory.createMod(pho);
    }

    /**
     * Convert ipa string into a pho element.
     *
     * @param ipa
     * @return pho element
     * @throws java.text.ParseException if the ipa string is invalid
     */
    public static JAXBElement<PhoneticTranscriptionType> ipaToPho(String ipa) throws ParseException {
        final ObjectFactory factory = new ObjectFactory();
        final PhoneticTranscriptionType pho = ipaToPhoneticTranscription(ipa);
        return factory.createPho(pho);
    }

    // endregion

    // region xml2ipa
    /**
     * Convert phonetic transcription to string ipa.
     *
     * @param pho
     * @return ipa string
     * @throws java.text.ParseException if the phonetic transcription is invalid
     */
    public static String phoneticTranscriptionToIpa(PhoneticTranscriptionType pho) throws ParseException {
        final XmlToIpaVisitor visitor = new XmlToIpaVisitor();
        pho.getPwOrPauseOrPhog().forEach(visitor::visit);
        return visitor.toIPATranscript().toString();
    }

    /**
     * Convert pho or mod element to string ipa.
     *
     * @param phoEle
     * @return ipa string
     * @throws java.text.ParseException if the mod element is invalid
     */
    public static String phoToIpa(JAXBElement<PhoneticTranscriptionType> phoEle) throws ParseException {
        return phoneticTranscriptionToIpa(phoEle.getValue());
    }
    // endregion
}
