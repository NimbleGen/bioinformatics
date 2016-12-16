package com.roche.sequencing.bioinformatics.common.text.viewer;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An extension of JRadioButtonMenuItem that doesn't close the menu when selected.
 *
 * SOURCE: http://www.camick.com/java/source/StayOpenRadioButtonMenuItem.java
 */
public class StayOpenRadioButtonMenuItem extends JRadioButtonMenuItem {

	private static final long serialVersionUID = 1L;
	private MenuElement[] path;

	{
		getModel().addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (getModel().isArmed() && isShowing()) {
					path = MenuSelectionManager.defaultManager().getSelectedPath();
				}
			}
		});
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem()
	 */
	public StayOpenRadioButtonMenuItem() {
		super();
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(Action)
	 */
	public StayOpenRadioButtonMenuItem(Action a) {
		super();
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(Icon)
	 */
	public StayOpenRadioButtonMenuItem(Icon icon) {
		super(icon);
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(Icon, boolean)
	 */
	public StayOpenRadioButtonMenuItem(Icon icon, boolean selected) {
		super(icon, selected);
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(String)
	 */
	public StayOpenRadioButtonMenuItem(String text) {
		super(text);
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(String, boolean)
	 */
	public StayOpenRadioButtonMenuItem(String text, boolean selected) {
		super(text, selected);
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(String, Icon)
	 */
	public StayOpenRadioButtonMenuItem(String text, Icon icon) {
		super(text, icon);
	}

	/**
	 * @see JRadioButtonMenuItem#JRadioButtonMenuItem(String, Icon, boolean)
	 */
	public StayOpenRadioButtonMenuItem(String text, Icon icon, boolean selected) {
		super(text, icon, selected);
	}

	/**
	 * Overridden to reopen the menu.
	 *
	 * @param pressTime
	 *            the time to "hold down" the button, in milliseconds
	 */
	@Override
	public void doClick(int pressTime) {
		super.doClick(pressTime);
		MenuSelectionManager.defaultManager().setSelectedPath(path);
	}
}
