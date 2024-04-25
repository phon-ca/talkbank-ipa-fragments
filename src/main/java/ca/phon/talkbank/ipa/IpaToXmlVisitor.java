/*
 * Copyright (C) 2005-2020 Gregory Hedlund & Yvan Rose
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.phon.talkbank.ipa;

import ca.phon.ipa.*;
import ca.phon.ipa.Linker;
import ca.phon.ipa.Pause;
import ca.phon.syllable.SyllabificationInfo;
import ca.phon.syllable.SyllableConstituentType;
import ca.phon.visitor.VisitorAdapter;
import ca.phon.visitor.annotation.Visits;
import org.talkbank.ns.talkbank.*;
import org.talkbank.ns.talkbank.StressType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 */
public class IpaToXmlVisitor extends VisitorAdapter<IPAElement> {
	
	private final ObjectFactory factory = new ObjectFactory();
	
	private PhoneticTranscriptionType pho;

	private PhoGroupType phoGroup;

	private PhoneticWord currentWord;

	private int currentIndex = 0;
	
	public IpaToXmlVisitor() {
		this.pho = factory.createPhoneticTranscriptionType();
		this.currentWord = factory.createPhoneticWord();
	}

	@Visits
	public void visitPhone(Phone phone) {
		final PhoneType phoneType = factory.createPhoneType();
		if(phone.getPrefixDiacritics().length > 0)
			Arrays.stream(phone.getPrefixDiacritics()).map(Diacritic::getText).forEach(phoneType.getPrefix()::add);
		phoneType.setBase(phone.getBase());
		if(phone.getCombiningDiacritics().length > 0)
			Arrays.stream(phone.getCombiningDiacritics()).map(Diacritic::getText).forEach(phoneType.getCombining()::add);
		String phLen = Arrays.stream(phone.getLengthDiacritics()).map(Diacritic::getText).collect(Collectors.joining());
		if(phLen.length() > 0) phoneType.setPhlen(phLen);
		String toneNum = Arrays.stream(phone.getToneNumberDiacritics()).map(Diacritic::getText).collect(Collectors.joining());
		if(toneNum.length() > 0) phoneType.setToneNumber(toneNum);
		final List<Diacritic> filteredSuffixDiacritics =
				Arrays.stream(phone.getSuffixDiacritics()).filter(d -> d.getType() == DiacriticType.SUFFIX).toList();
		if(filteredSuffixDiacritics.size() > 0)
			filteredSuffixDiacritics.stream().map(Diacritic::getText).forEach(phoneType.getSuffix()::add);
		if(phone.getScType() != SyllableConstituentType.UNKNOWN) {
			org.talkbank.ns.talkbank.SyllableConstituentType scType = switch (phone.getScType()) {
				case AMBISYLLABIC -> org.talkbank.ns.talkbank.SyllableConstituentType.AMBISYLLABIC;
				case CODA -> org.talkbank.ns.talkbank.SyllableConstituentType.CODA;
				case LEFTAPPENDIX -> org.talkbank.ns.talkbank.SyllableConstituentType.LEFT_APPENDIX;
				case NUCLEUS -> org.talkbank.ns.talkbank.SyllableConstituentType.NUCLEUS;
				case OEHS -> org.talkbank.ns.talkbank.SyllableConstituentType.OEHS;
				case ONSET -> org.talkbank.ns.talkbank.SyllableConstituentType.ONSET;
				case RIGHTAPPENDIX -> org.talkbank.ns.talkbank.SyllableConstituentType.RIGHT_APPENDIX;
				case UNKNOWN, WORDBOUNDARYMARKER, SYLLABLESTRESSMARKER, SYLLABLEBOUNDARYMARKER -> null;
			};
			if(scType == org.talkbank.ns.talkbank.SyllableConstituentType.NUCLEUS) {
				final SyllabificationInfo info = phone.getExtension(SyllabificationInfo.class);
				if(info.isDiphthongMember())
					scType = org.talkbank.ns.talkbank.SyllableConstituentType.DIPHTHONG;
			}
			phoneType.setScType(scType);
		}
		this.currentWord.getStressOrPhOrCmph().add(phoneType);
	}

	@Visits
	public void visitCompoundPhone(CompoundPhone cmpPhone) {
		final CompoundPhoneType CompoundPhoneType = factory.createCompoundPhoneType();
		visit(cmpPhone.getFirstPhone());
		final Object firstPhoneType = this.currentWord.getStressOrPhOrCmph().remove(this.currentWord.getStressOrPhOrCmph().size()-1);
		visit(cmpPhone.getSecondPhone());
		final Object secondPhoneType = this.currentWord.getStressOrPhOrCmph().remove(this.currentWord.getStressOrPhOrCmph().size()-1);

		if(firstPhoneType instanceof PhoneType) {
			CompoundPhoneType.getContent().add(factory.createPh(((PhoneType) firstPhoneType)));
		} else {
			CompoundPhoneType.getContent().add(factory.createCmph(((CompoundPhoneType) firstPhoneType)));
		}

		LigatureTypeType ligType = switch (cmpPhone.getLigature()) {
			case '\u035c' -> LigatureTypeType.BREVE_BELOW;
			case '\u0362' -> LigatureTypeType.RIGHT_ARROW_BELOW;
			default -> LigatureTypeType.BREVE;
		};
		final LigatureType lig = factory.createLigatureType();
		lig.setType(ligType);
		CompoundPhoneType.getContent().add(factory.createLig(lig));

		CompoundPhoneType.getContent().add(factory.createPh(((PhoneType) secondPhoneType)));
		this.currentWord.getStressOrPhOrCmph().add(CompoundPhoneType);
	}

	@Visits
	public void visitStress(StressMarker stressMarker) {
		final StressType stressType = factory.createStressType();
		final StressTypeType stt = switch (stressMarker.getType()) {
			case PRIMARY -> StressTypeType.PRIMARY;
			case SECONDARY -> StressTypeType.SECONDARY;
		};
		stressType.setType(stt);
		this.currentWord.getStressOrPhOrCmph().add(stressType);
	}

	@Visits
	public void visitIntraWordPause(IntraWordPause pause) {
		final PhoneticProsodyType pp = factory.createPhoneticProsodyType();
		if(this.currentWord.getStressOrPhOrCmph().size() == 0)
			pp.setType(PhoneticProsodyTypeType.BLOCKING.BLOCKING);
		else
			pp.setType(PhoneticProsodyTypeType.PAUSE.PAUSE);
		this.currentWord.getStressOrPhOrCmph().add(pp);
	}

	@Visits
	public void visitContraction(Contraction sandhi) {
		final SandhiType sandhiType = factory.createSandhiType();
		sandhiType.setType(SandhiTypeType.CONTRACTION);
		this.currentWord.getStressOrPhOrCmph().add(sandhiType);
	}

	@Visits
	public void visitLinker(Linker linker) {
		final SandhiType sandhiType = factory.createSandhiType();
		sandhiType.setType(SandhiTypeType.LINKER);
		this.currentWord.getStressOrPhOrCmph().add(sandhiType);
	}

	@Visits
	public void visitIntonationGroup(IntonationGroup ig) {
		final PhoneticProsodyType pp = factory.createPhoneticProsodyType();
		final PhoneticProsodyTypeType ptt = switch (ig.getType()) {
			case MAJOR -> PhoneticProsodyTypeType.MAJOR_INTONATION_GROUP;
			case MINOR -> PhoneticProsodyTypeType.MINOR_INTONATION_GROUP;
		};
		this.currentWord.getStressOrPhOrCmph().add(pp);
	}

	@Visits
	public void visitSyllableBoundary(SyllableBoundary sb) {
		final PhoneticProsodyType pp = factory.createPhoneticProsodyType();
		pp.setType(PhoneticProsodyTypeType.SYLLABLE_BREAK);
		this.currentWord.getStressOrPhOrCmph().add(pp);
	}

	@Visits
	public void visitPause(Pause pause) {
		final PauseSymbolicLengthType type = switch (pause.getType()) {
			case SIMPLE, NUMERIC -> PauseSymbolicLengthType.SIMPLE;
			case LONG -> PauseSymbolicLengthType.LONG;
			case VERY_LONG -> PauseSymbolicLengthType.VERY_LONG;
		};
		final org.talkbank.ns.talkbank.Pause Pause = factory.createPause();
		if(pause.getType() == PauseLength.NUMERIC) {
			Pause.setLength(BigDecimal.valueOf(pause.getLength()).setScale(3, RoundingMode.HALF_UP));
		}
		Pause.setSymbolicLength(type);
		addCurrentWord();
		this.pho.getPwOrPauseOrPhog().add(Pause);
	}

	@Visits
	public void visitWordBoundary(WordBoundary wb) {
		addCurrentWord();
	}

	@Visits
	public void visitPhoneticGroupMarker(PhoneticGroupMarker pgm) {
		if(pgm.getType() == PhoneticGroupMarkerType.BEGIN) {
			this.phoGroup = factory.createPhoGroupType();
		} else {
			if(this.phoGroup != null && !this.phoGroup.getPwOrPause().isEmpty()) {
				addCurrentWord();
				this.pho.getPwOrPauseOrPhog().add(phoGroup);
				this.phoGroup = null;
			}
		}
	}

	@Override
	public void fallbackVisit(IPAElement obj) {
	}

	private void addCurrentWord() {
		if(!this.currentWord.getStressOrPhOrCmph().isEmpty()) {
			if(this.phoGroup != null) {
				this.phoGroup.getPwOrPause().add(this.currentWord);
			} else {
				this.pho.getPwOrPauseOrPhog().add(this.currentWord);
			}
			this.currentWord = factory.createPhoneticWord();
		}
	}

	public PhoneticTranscriptionType getPho() {
		addCurrentWord();
		return this.pho;
	}

}
