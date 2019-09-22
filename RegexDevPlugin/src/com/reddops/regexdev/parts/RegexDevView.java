package com.reddops.regexdev.parts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * @author Doug Redd <doug@redd.org>
 */
public class RegexDevView {

	/**
	 * Number of main container layout columns.
	 */
	private static final int MAIN_LAYOUT_COLUMNS = 4;
	/**
	 * Matched string vertical span.
	 */
	private static final int MATCH_V_SPAN = 3;
	/**
	 * Matched string horizontal span.
	 */
	private static final int MATCH_H_SPAN = 3;
	/**
	 * Regex string vertical span.
	 */
	private static final int REGEX_V_SPAN = 3;
	/**
	 * Regex string horizontal span.
	 */
	private static final int REGEX_H_SPAN = 3;
	/**
	 * Minimum width of status display string.
	 */
	private static final int CAPTURE_GROUPS_MIN_WIDTH = 200;
	/**
	 * Margin size around pattern flags buttons.
	 */
	private static final int PATTERN_FLAGS_MARGINS = 0;
	/**
	 * Pattern flags horizontal span.
	 */
	private static final int PATTERN_FLAGS_H_SPAN = 2;
	/**
	 * Columns in pattern flags group.
	 */
	private static final int PATTERN_FLAGS_COLUMNS = 3;
	/**
	 * Cached empty style range array.
	 */
	private static final StyleRange[] EMPTY_STYLERANGE_ARR = new StyleRange[0];
	/**
	 * Enum for matcher mode.
	 */
	private enum MatcherMode {
		FIND, MATCH
	}

	/**
	 * The main composite.
	 */
	private transient Composite composite;
	/**
	 * The regular expression text.
	 */
	private transient StyledText regexST;
	/**
	 * The text to be matched.
	 */
	private transient StyledText matchST;
	/**
	 * Pattern flags.
	 */
	private transient int patternFlags = 0;
	/**
	 * Capture group value display.
	 */
	private transient StyledText groupST;
	/**
	 * Matcher mode.
	 */
	private transient MatcherMode matcherMode;

	/**
	 * Update regex evaluation.
	 */
	private void update() {
		try {
			if (matchST.getStyleRanges().length > 0) {
				matchST.setStyleRanges(EMPTY_STYLERANGE_ARR);
				matchST.redraw();
				matchST.update();
			}
			if (regexST.getStyleRanges().length > 0 || regexST.getToolTipText() != null) {
				regexST.setStyleRanges(EMPTY_STYLERANGE_ARR);
				regexST.setToolTipText(null);
				regexST.redraw();
				regexST.update();
			}
			groupST.setText("");

			if (regexST.getText().length() == 0 || matchST.getText().length() == 0) {
				return;
			}

			if (this.matcherMode == MatcherMode.FIND) {
				final Matcher m = Pattern.compile(regexST.getText(), this.patternFlags).matcher(matchST.getText());
				int finds = 0;
				while (m.find()) {
					addMatch(m, finds++);
				}
			} else if (this.matcherMode == MatcherMode.MATCH) {
				final Matcher m = Pattern.compile(regexST.getText(), this.patternFlags).matcher(matchST.getText());
				if (m.matches()) {
					addMatch(m, 1);
				}
			} else {
				System.err.println(this.getClass().getCanonicalName() + ".update(): unhandled matcher mode: " + this.matcherMode);
			}
		} catch (final PatternSyntaxException pse) {
			final int idxOfError = pse.getIndex();
			final StyleRange sr = new StyleRange();
			sr.start = idxOfError - 1;
			sr.length = 1;
			sr.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
			regexST.setStyleRange(sr);
			regexST.setToolTipText(pse.getMessage());
		}
	}

	/**
	 * Add a match to the display.
	 *
	 * @param m           Matcher
	 * @param matchNumber Match number within the matched text (when using find)
	 */
	private void addMatch(final Matcher m, final int matchNumber) {
		final StyleRange sr = new StyleRange();
		sr.start = m.start();
		sr.length = m.end() - m.start();
		sr.underline = true;
		matchST.setStyleRange(sr);
		addGroups(m, matchNumber);
	}

	/**
	 * Add capture groups to the display.
	 *
	 * @param m           Matcher
	 * @param matchNumber Match number within the matched text (when using find)
	 */
	private void addGroups(final Matcher m, final int matchNumber) {
		if (m.groupCount() > 0) {
			if (groupST.getText().length() > 0) {
				groupST.append("\n");
			}
			final StringBuilder line = new StringBuilder("" + matchNumber + ":");
			for (int i = 1; i < m.groupCount() + 1; i++) {
				if (i > 1) {
					line.append(", ");
				}
				line.append(" [" + i + "] '" + m.group(i) + "'");
			}
			groupST.append(line.toString());
		}
	}

	/**
	 * Create the control.
	 *
	 * @param parent Parent composite
	 */
	@PostConstruct
	public void createPartControl(final Composite parent) {
		System.out.println("Enter in SampleE4View postConstruct");

		composite = new Composite(parent, SWT.FILL);
		final GridLayout gl = new GridLayout(MAIN_LAYOUT_COLUMNS, false);
		composite.setLayout(gl);

		final Label matchLabel = new Label(composite, SWT.NONE);
		matchLabel.setText("Text to\nbe matched:");
		final GridData mlGridData = new GridData();
		mlGridData.verticalSpan = MATCH_V_SPAN;
		matchLabel.setLayoutData(mlGridData);

		matchST = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		matchST.setAlwaysShowScrollBars(false);
		final GridData matchGridData = new GridData();
		matchGridData.horizontalAlignment = GridData.FILL;
		matchGridData.verticalAlignment = GridData.FILL;
		matchGridData.horizontalSpan = MATCH_H_SPAN;
		matchGridData.verticalSpan = MATCH_V_SPAN;
		matchST.setLayoutData(matchGridData);
		matchST.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent me) {
				update();
			}
		});

		final Label regexLabel = new Label(composite, SWT.NONE);
		regexLabel.setText("Regular\nExpression:");
		final GridData reGridData = new GridData();
		reGridData.verticalSpan = REGEX_V_SPAN;
		regexLabel.setLayoutData(reGridData);

		regexST = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		regexST.setAlwaysShowScrollBars(false);
		final GridData regexGridData = new GridData();
		regexGridData.horizontalAlignment = GridData.FILL;
		regexGridData.verticalAlignment = GridData.FILL;
		regexGridData.horizontalSpan = REGEX_H_SPAN;
		regexGridData.verticalSpan = REGEX_V_SPAN;
		regexST.setLayoutData(regexGridData);
		regexST.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent me) {
				update();
			}
		});

		final Group pattFlagsGrp = new Group(composite, SWT.NONE);
		pattFlagsGrp.setBackgroundMode(SWT.INHERIT_FORCE);

		pattFlagsGrp.setText("Pattern flags");
		final GridLayout pfGridLayout = new GridLayout(PATTERN_FLAGS_COLUMNS, false);
		pfGridLayout.horizontalSpacing = PATTERN_FLAGS_MARGINS;
		pfGridLayout.verticalSpacing = PATTERN_FLAGS_MARGINS;
		pfGridLayout.marginTop = PATTERN_FLAGS_MARGINS;
		pfGridLayout.marginRight = PATTERN_FLAGS_MARGINS;
		pfGridLayout.marginBottom = PATTERN_FLAGS_MARGINS;
		pfGridLayout.marginLeft = PATTERN_FLAGS_MARGINS;
		pattFlagsGrp.setLayout(pfGridLayout);

		final GridData pfGridData = new GridData();
		pfGridData.horizontalSpan = PATTERN_FLAGS_H_SPAN;
		pattFlagsGrp.setLayoutData(pfGridData);

		buildPatternFlagButton(pattFlagsGrp, "UNIX_LINES (?d)", Pattern.UNIX_LINES);
		buildPatternFlagButton(pattFlagsGrp, "CASE_INSENSITIVE (?i)", Pattern.CASE_INSENSITIVE);
		buildPatternFlagButton(pattFlagsGrp, "COMMENTS (?x)", Pattern.COMMENTS);
		buildPatternFlagButton(pattFlagsGrp, "MULTILINE (?m)", Pattern.MULTILINE);
		buildPatternFlagButton(pattFlagsGrp, "LITERAL", Pattern.LITERAL);
		buildPatternFlagButton(pattFlagsGrp, "DOTALL (?s)", Pattern.DOTALL);
		buildPatternFlagButton(pattFlagsGrp, "UNICODE_CASE (?u)", Pattern.UNICODE_CASE);
		buildPatternFlagButton(pattFlagsGrp, "CANON_EQ", Pattern.CANON_EQ);
		buildPatternFlagButton(pattFlagsGrp, "UNICODE_CHARACTER_CLASS (?U)", Pattern.UNICODE_CHARACTER_CLASS);

		final Group matchModeComp = new Group(composite, SWT.NONE);
		matchModeComp.setText("Matcher mode");
		matchModeComp.setLayout(new FillLayout(SWT.VERTICAL));
		final GridData mmGridData = new GridData();
		matchModeComp.setLayoutData(mmGridData);

		buildMatcherModeButton(matchModeComp, "find", true, MatcherMode.FIND);
		buildMatcherModeButton(matchModeComp, "match", false, MatcherMode.MATCH);
		this.matcherMode = MatcherMode.FIND;

		final Group captureGroups = new Group(composite, SWT.NONE);
		captureGroups.setText("Capture groups");
		captureGroups.setLayout(new FillLayout());
		final GridData cgGridData = new GridData();
		cgGridData.horizontalAlignment = GridData.FILL;
		cgGridData.verticalAlignment = GridData.FILL;
		cgGridData.grabExcessHorizontalSpace = true;
		cgGridData.minimumWidth = CAPTURE_GROUPS_MIN_WIDTH;
		captureGroups.setLayoutData(cgGridData);

		groupST = new StyledText(captureGroups, SWT.NONE);
		groupST.setEnabled(false);

		composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	/**
	 * Builds a pattern flag checkbox.
	 *
	 * @param container The parent composite for the checkbox
	 * @param label     The string to use for the checkbox label
	 * @param updateSL  The selection listener for the checkbox
	 * @return The newly built checkbox
	 */
	private Button buildPatternFlagButton(final Composite container, final String label,
			final int patternFlag) {
		final Button btn = new Button(container, SWT.CHECK);
		btn.setText(label);
		btn.addSelectionListener(new PattFlagSelectionListener(patternFlag));
		return btn;
	}

	/**
	 * Builds a matcher mode radio button.
	 *
	 * @param container The parent composite for the checkbox
	 * @param label     The string to use for the checkbox label
	 * @param selected  The selected status of the radio button
	 * @param updateSL  The selection listener for the checkbox
	 * @return The newly build radio button
	 */
	private Button buildMatcherModeButton(final Composite container, final String label, final boolean selected, final MatcherMode matcherMode) {
		final Button btn = new Button(container, SWT.RADIO);
		btn.setText(label);
		btn.setSelection(selected);
		btn.addSelectionListener(new MatcherModeSelectionListener(matcherMode));
		return btn;
	}

	/**
	 * Focuses on this control.
	 */
	@Focus
	public void setFocus() {
		composite.setFocus();
	}
	
	private class PattFlagSelectionListener implements SelectionListener {

		private transient final int patternFlag;
		private transient int invertedFlag;
		
		public PattFlagSelectionListener(final int patternFlag) {
			this.patternFlag = patternFlag;
			this.invertedFlag = ~patternFlag;
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent se) {
			if (((Button)se.getSource()).getSelection()) {
				patternFlags |= this.patternFlag; 
			} else {
				patternFlags &= this.invertedFlag;
			}
			update();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			if (((Button)se.getSource()).getSelection()) {
				patternFlags |= this.patternFlag; 
			} else {
				patternFlags &= this.invertedFlag;
			}
			update();
		}
		
	}
	
	private class MatcherModeSelectionListener implements SelectionListener {

		private transient final MatcherMode mMode;
		
		public MatcherModeSelectionListener(final MatcherMode matcherMode) {
			this.mMode = matcherMode;
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent se) {
			matcherMode = mMode;
			update();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			matcherMode = mMode;
			update();
		}
		
	}

}