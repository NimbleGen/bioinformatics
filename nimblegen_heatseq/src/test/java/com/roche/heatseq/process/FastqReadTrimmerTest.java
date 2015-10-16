/**
 *   Copyright 2013 Roche NimbleGen
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.roche.heatseq.process;

import java.io.IOException;

import net.sf.picard.fastq.FastqRecord;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.roche.heatseq.objects.ParsedProbeFile;
import com.roche.heatseq.objects.Probe;
import com.roche.heatseq.process.FastqReadTrimmer.ProbeInfoStats;
import com.roche.sequencing.bioinformatics.common.sequence.IupacNucleotideCodeSequence;
import com.roche.sequencing.bioinformatics.common.sequence.Strand;

public class FastqReadTrimmerTest {

	@Test(groups = { "unit" })
	public void trimOneTest() {
		FastqRecord record = new FastqRecord("pref", "AAAAAGGGGGTTTTTCCCCC", "qualHeader", "AAAAAGGGGGTTTTTCCCCC");
		FastqRecord trimmedRecord = FastqReadTrimmer.trim(record, 5, 14, true);
		Assert.assertEquals(trimmedRecord.getReadString().length(), 10);
		Assert.assertEquals(trimmedRecord.getBaseQualityString().length(), 10);
	}

	@Test(groups = { "unit" })
	public void probeInfoStatsTest() throws IOException {
		ParsedProbeFile probes = new ParsedProbeFile();
		probes.addProbe("chr1", new Probe("probe1", "chr1", 1, 10, new IupacNucleotideCodeSequence("ACGTACGTAC"), 21, 30, new IupacNucleotideCodeSequence("ACGTACGTAC"), 11, 20,
				new IupacNucleotideCodeSequence("ACGTACGTAC"), Strand.FORWARD, ""));
		probes.addProbe("chr1", new Probe("probe2", "chr1", 1, 9, new IupacNucleotideCodeSequence("ACGTACGTA"), 19, 27, new IupacNucleotideCodeSequence("ACGTACGTA"), 10, 18,
				new IupacNucleotideCodeSequence("ACGTACGTA"), Strand.FORWARD, ""));
		ProbeInfoStats stats = FastqReadTrimmer.collectStatsFromProbeInformation(probes);
		Assert.assertEquals(stats.getMinLigationPrimerLength(), 9);
		Assert.assertEquals(stats.getMinExtensionPrimerLength(), 9);
		Assert.assertEquals(stats.getMinCaptureTargetLength(), 9);
		Assert.assertEquals(stats.getMaxLigationPrimerLength(), 10);
		Assert.assertEquals(stats.getMaxExtensionPrimerLength(), 10);
		Assert.assertEquals(stats.getMaxCaptureTargetLength(), 10);
	}

}
