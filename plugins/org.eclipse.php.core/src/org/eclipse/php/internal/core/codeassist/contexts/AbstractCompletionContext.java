/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.core.codeassist.contexts;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.CompletionRequestor;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.php.core.codeassist.ICompletionContext;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.PHPVersion;
import org.eclipse.php.internal.core.codeassist.CompletionCompanion;
import org.eclipse.php.internal.core.codeassist.IPHPCompletionRequestor;
import org.eclipse.php.internal.core.documentModel.parser.PHPRegionContext;
import org.eclipse.php.internal.core.documentModel.parser.regions.IPhpScriptRegion;
import org.eclipse.php.internal.core.documentModel.parser.regions.PHPRegionTypes;
import org.eclipse.php.internal.core.documentModel.partitioner.PHPPartitionTypes;
import org.eclipse.php.internal.core.project.ProjectOptions;
import org.eclipse.php.internal.core.util.text.PHPTextSequenceUtilities;
import org.eclipse.php.internal.core.util.text.TextSequence;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.exceptions.ResourceAlreadyExists;
import org.eclipse.wst.sse.core.internal.provisional.text.*;

/**
 * This is an abstract completion context containing all common utilities.
 * 
 * @author michael
 */
public abstract class AbstractCompletionContext implements ICompletionContext {

	private CompletionCompanion companion;
	private CompletionRequestor requestor;
	private ISourceModule sourceModule;
	private int offset;
	private PHPVersion phpVersion;
	private IStructuredDocument document;
	private IStructuredDocumentRegion structuredDocumentRegion;
	private ITextRegionCollection regionCollection;
	private IPhpScriptRegion phpScriptRegion;
	private String partitionType;

	public void init(CompletionCompanion companion) {
		this.companion = companion;
	}

	protected CompletionCompanion getCompanion() {
		return companion;
	}

	public boolean isValid(ISourceModule sourceModule, int offset,
			CompletionRequestor requestor) {
		if (sourceModule == null) {
			throw new IllegalArgumentException();
		}

		this.requestor = requestor;
		this.sourceModule = sourceModule;
		this.offset = offset;
		this.phpVersion = ProjectOptions.getPhpVersion(sourceModule
				.getScriptProject().getProject());

		try {
			this.document = determineDocument(sourceModule, requestor);
			if (this.document != null) {

				structuredDocumentRegion = determineStructuredDocumentRegion(
						document, offset);
				if (structuredDocumentRegion != null) {

					regionCollection = determineRegionCollection(document,
							structuredDocumentRegion, offset);
					if (regionCollection != null) {

						phpScriptRegion = determinePhpRegion(document,
								regionCollection, offset);
						if (phpScriptRegion != null) {

							partitionType = determinePartitionType(
									regionCollection, phpScriptRegion, offset);
							if (partitionType != null) {

								String prefix = getPrefix();
								if (prefix.length() > 0
										&& !Character
												.isJavaIdentifierStart(prefix
														.charAt(0))) {
									return false;
								}
								return true;
							}
						}
					}
				}
			}

		} catch (Exception e) {
			PHPCorePlugin.log(e);
		}
		return false;
	}

	public boolean isExclusive() {
		return false;
	}

	/**
	 * Determines the structured document region of the place in PHP code where
	 * completion was requested
	 * 
	 * @return structured document region or <code>null</code> in case it could
	 *         not be determined
	 */
	protected IStructuredDocumentRegion determineStructuredDocumentRegion(
			IStructuredDocument document, int offset) {

		IStructuredDocumentRegion sdRegion = null;

		int lastOffset = offset;
		// find the structured document region:
		while (sdRegion == null && lastOffset >= 0) {
			sdRegion = document.getRegionAtCharacterOffset(lastOffset);
			lastOffset--;
		}

		return sdRegion;
	}

	/**
	 * Determines the relevant region collection of the place in PHP code where
	 * completion was requested
	 * 
	 * @return text region collection or <code>null</code> in case it could not
	 *         be determined
	 */
	protected ITextRegionCollection determineRegionCollection(
			IStructuredDocument document, IStructuredDocumentRegion sdRegion,
			int offset) {
		ITextRegionCollection regionCollection = sdRegion;

		ITextRegion textRegion = determineTextRegion(document, sdRegion, offset);
		if (textRegion instanceof ITextRegionContainer) {
			regionCollection = (ITextRegionContainer) textRegion;
		}
		return regionCollection;
	}

	/**
	 * Determines the text region from the text region collection and offset
	 * 
	 * @param regionCollection
	 * @param offset
	 */
	protected ITextRegion determineTextRegion(IStructuredDocument document,
			ITextRegionCollection regionCollection, int offset) {
		ITextRegion textRegion;
		// in case we are at the end of the document, asking for completion
		if (offset == document.getLength()) {
			textRegion = regionCollection.getLastRegion();
		} else {
			textRegion = regionCollection.getRegionAtCharacterOffset(offset);
		}
		return textRegion;
	}

	/**
	 * Determines the PHP script region of PHP code where completion was
	 * requested
	 * 
	 * @return php script region or <code>null</code> in case it could not be
	 *         determined
	 */
	protected IPhpScriptRegion determinePhpRegion(IStructuredDocument document,
			ITextRegionCollection regionCollection, int offset) {
		ITextRegion textRegion = determineTextRegion(document,
				regionCollection, offset);
		IPhpScriptRegion phpScriptRegion = null;

		if (textRegion != null) {
			if (textRegion.getType() == PHPRegionContext.PHP_OPEN) {
				return null;
			} else if (textRegion.getType() == PHPRegionContext.PHP_CLOSE) {
				if (regionCollection.getStartOffset(textRegion) == offset) {
					textRegion = regionCollection
							.getRegionAtCharacterOffset(offset - 1);
				} else {
					return null;
				}
			}
		}

		if (textRegion instanceof IPhpScriptRegion) {
			phpScriptRegion = (IPhpScriptRegion) textRegion;
		}

		return phpScriptRegion;
	}

	/**
	 * Determines the partition type of the code where cursor is located.
	 * 
	 * @param regionCollection
	 *            Text region collection
	 * @param phpScriptRegion
	 *            PHP script region
	 * @param offset
	 * @return partition type (see {@link PHPRegionTypes})
	 * @throws BadLocationException
	 */
	protected String determinePartitionType(
			ITextRegionCollection regionCollection,
			IPhpScriptRegion phpScriptRegion, int offset)
			throws BadLocationException {

		int internalOffset = offset - regionCollection.getStartOffset()
				- phpScriptRegion.getStart() - 1;
		String partitionType = phpScriptRegion.getPartition(internalOffset);

		// if we are at the begining of multi-line comment or docBlock then we
		// should get completion.
		if (partitionType == PHPPartitionTypes.PHP_MULTI_LINE_COMMENT
				|| partitionType == PHPPartitionTypes.PHP_DOC) {
			String regionType = phpScriptRegion.getPhpToken(internalOffset)
					.getType();
			if (regionType == PHPRegionTypes.PHP_COMMENT_START
					|| regionType == PHPRegionTypes.PHPDOC_COMMENT_START) {
				if (phpScriptRegion.getPhpToken(internalOffset).getStart() == internalOffset) {
					partitionType = phpScriptRegion
							.getPartition(internalOffset - 1);
				}
			}
		}
		return partitionType;
	}

	/**
	 * Determines the document associated with the editor where code assist has
	 * been invoked.
	 * 
	 * @param module
	 *            Source module ({@link ISourceModule})
	 * @param requestor
	 *            Completion requestor ({@link CompletionRequestor})
	 * @return structured document or <code>null</code> if it couldn't be found
	 * @throws CoreException
	 * @throws IOException
	 * @throws ResourceAlreadyExists
	 */
	protected IStructuredDocument determineDocument(ISourceModule module,
			CompletionRequestor requestor) throws ResourceAlreadyExists,
			IOException, CoreException {
		IStructuredDocument document = null;

		if (requestor instanceof IPHPCompletionRequestor) {
			IDocument d = ((IPHPCompletionRequestor) requestor).getDocument();
			if (d instanceof IStructuredDocument) {
				document = (IStructuredDocument) d;
			}
		}
		if (document == null) {
			IStructuredModel structuredModel = null;
			try {
				IFile file = (IFile) module.getResource();
				if (file != null) {
					if (file.exists()) {
						structuredModel = StructuredModelManager
								.getModelManager()
								.getExistingModelForRead(file);
						if (structuredModel != null) {
							document = structuredModel.getStructuredDocument();
						} else {
							document = StructuredModelManager.getModelManager()
									.createStructuredDocumentFor(file);
						}
					} else {
						document = StructuredModelManager.getModelManager()
								.createNewStructuredDocumentFor(file);
						document.set(module.getSource());
					}
				}
			} finally {
				if (structuredModel != null) {
					structuredModel.releaseFromRead();
				}
			}
		}

		return document;
	}

	/**
	 * Returns PHP version of the file where code assist was requested
	 * 
	 * @return PHP version
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public PHPVersion getPhpVersion() {
		return phpVersion;
	}

	/**
	 * Returns the file where code assist was requested
	 * 
	 * @return source module
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public ISourceModule getSourceModule() {
		return sourceModule;
	}

	/**
	 * Returns document associated with the editor where code assist was
	 * requested
	 * 
	 * @return document
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public IStructuredDocument getDocument() {
		return document;
	}

	/**
	 * Returns the relevant region collection of the place in PHP code where
	 * completion was requested
	 * 
	 * @return text region collection
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public ITextRegionCollection getRegionCollection() {
		return regionCollection;
	}

	/**
	 * Returns the PHP script region of PHP code where completion was requested
	 * 
	 * @return php script region (see {@link IPhpScriptRegion})
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public IPhpScriptRegion getPhpScriptRegion() {
		return phpScriptRegion;
	}

	/**
	 * Returns partition type of the code where cursor is located.
	 * 
	 * @return partition type (see {@link PHPRegionTypes})
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public String getPartitionType() {
		return partitionType;
	}

	/**
	 * Returns the statement text that is before the cursor
	 * 
	 * @return statement text
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public TextSequence getStatementText() {
		return PHPTextSequenceUtilities.getStatement(offset,
				structuredDocumentRegion, true);
	}

	/**
	 * Returns whether there are whitespace characters before the cursor where
	 * code assist was being invoked
	 * 
	 * @return <code>true</code> if there are whitespace characters before the
	 *         cursor
	 */
	public boolean hasWhitespaceBeforeCursor() {
		TextSequence statementText = getStatementText();

		// determine whether there are whitespaces before the cursor
		int statementLength = statementText.length();
		int statementEnd = PHPTextSequenceUtilities.readBackwardSpaces(
				statementText, statementLength);
		return statementLength != statementEnd;
	}

	/**
	 * Returns completion requestor
	 * 
	 * @return completion requestor (see {@link CompletionRequestor})
	 * @see #isValid(ISourceModule, int, CompletionRequestor)
	 */
	public CompletionRequestor getCompletionRequestor() {
		return requestor;
	}

	/**
	 * Returns offset of the cursor position when code assist was invoked
	 * 
	 * @return offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Returns previous word before the cursor position
	 * 
	 * @throws BadLocationException
	 */
	public String getPreviousWord() throws BadLocationException {
		TextSequence statementText = getStatementText();

		int statementLength = statementText.length();
		int wordEnd = PHPTextSequenceUtilities.readBackwardSpaces(
				statementText, statementLength); // read whitespace
		int wordStart = PHPTextSequenceUtilities.readIdentifierStartIndex(
				phpVersion, statementText, wordEnd, true);
		if (wordStart < 0 || wordEnd < 0 || wordStart > wordEnd) {
			return "";
		}
		String previousWord = statementText.subSequence(wordStart, wordEnd)
				.toString();

		if (hasWhitespaceBeforeCursor()) {
			return previousWord;
		}

		wordEnd = PHPTextSequenceUtilities.readBackwardSpaces(statementText,
				wordStart - 1); // read whitespace
		wordStart = PHPTextSequenceUtilities.readIdentifierStartIndex(
				phpVersion, statementText, wordEnd, true);
		if (wordStart < 0 || wordEnd < 0 || wordStart > wordEnd) {
			return "";
		}
		previousWord = statementText.subSequence(wordStart, wordEnd).toString();

		return previousWord;
	}

	/**
	 * Returns PHP token under offset
	 * 
	 * @return PHP token
	 * @throws BadLocationException
	 */
	public ITextRegion getPHPToken() throws BadLocationException {
		return phpScriptRegion.getPhpToken(offset
				- regionCollection.getStartOffset()
				- phpScriptRegion.getStart() - 1);
	}

	/**
	 * Returns the word on which code assist was invoked
	 * 
	 * @return prefix
	 * @throws BadLocationException
	 */
	public String getPrefix() throws BadLocationException {
		if (hasWhitespaceBeforeCursor()) {
			return ""; //$NON-NLS-1$
		}
		TextSequence statementText = getStatementText();
		int statementLength = statementText.length();
		int prefixEnd = PHPTextSequenceUtilities.readBackwardSpaces(
				statementText, statementLength); // read whitespace
		int prefixStart = PHPTextSequenceUtilities.readIdentifierStartIndex(
				phpVersion, statementText, prefixEnd, true);
		return statementText.subSequence(prefixStart, prefixEnd).toString();
	}

	/**
	 * Returns the end of the word on which code assist was invoked
	 * 
	 * @return
	 * @throws BadLocationException
	 */
	public int getPrefixEnd() throws BadLocationException {
		ITextRegion phpToken = getPHPToken();
		int endOffset = regionCollection.getStartOffset()
				+ phpScriptRegion.getStart() + phpToken.getTextEnd();
		if (phpToken.getType() == PHPRegionTypes.PHP_CONSTANT_ENCAPSED_STRING) {
			--endOffset;
		}
		return endOffset;
	}

	/**
	 * Returns next PHP token after offset
	 * 
	 * @return PHP token
	 * @throws BadLocationException
	 */
	public ITextRegion getNextPHPToken() throws BadLocationException {
		ITextRegion phpToken = getPHPToken();
		do {
			phpToken = phpScriptRegion.getPhpToken(phpToken.getEnd());
			if (!PHPPartitionTypes.isPHPCommentState(phpToken.getType())
					&& phpToken.getType() != PHPRegionTypes.WHITESPACE) {
				break;
			}
		} while (phpToken.getEnd() < phpScriptRegion.getLength());

		return phpToken;
	}

	/**
	 * Returns next word after the cursor position
	 * 
	 * @throws BadLocationException
	 */
	public String getNextWord() throws BadLocationException {
		ITextRegion nextPHPToken = getNextPHPToken();
		return document.get(regionCollection.getStartOffset()
				+ phpScriptRegion.getStart() + nextPHPToken.getStart(),
				nextPHPToken.getTextLength());
	}
}