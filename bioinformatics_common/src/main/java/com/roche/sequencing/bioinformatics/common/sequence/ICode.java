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

package com.roche.sequencing.bioinformatics.common.sequence;

public interface ICode {
	/**
	 * @param codeToMatch
	 * @return true if this matches the codeToMatch (note match does not mean equal)
	 */
	boolean matches(ICode codeToMatch);

	/**
	 * @return all nucleotides that match this code
	 */
	NucleotideCode[] getNucleotides();

	/**
	 * @return the IUPAC nucleotide code that matches this object
	 */
	IupacNucleotideCode getIupaceNucleotideCodeEquivalent();

	/**
	 * @return the compliment to this code
	 */
	ICode getComplimentCode();
}
