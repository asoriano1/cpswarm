/*
 * Copyright (C) 2009 Istvan Fehervari
 * Original project page: http://www.frevotool.tk
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License Version 3 as published
 * by the Free Software Foundation http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * There is no warranty for this free software. The GPL requires that 
 * modified versions be marked as changed, so that their problems will
 * not be attributed erroneously to authors of pious versions.
 */

package utils;

import java.sql.Timestamp;

/** Simple logging class with custom logging levels and adding timestamp as an option.
 * 
 *  @author Istvan Fehervari */
public class Log {
	
	/** Indicates the current global level of logging */
	public static int LOGLEVEL = 0;
	
	/** Prints the text to the standard output if the current logging level ( <code>{@link #LOGLEVEL}</code>) is smaller or equal than the given loglevel parameter.
	 * @param text  The <code>String</code> to be printed.
	 * @param loglevel The logging level of this output.
	 * */
	public static void print(String text, int loglevel) {
		if (loglevel >= LOGLEVEL) {
			System.out.print (text);
		}
	}
	
	/** Prints the text to the standard output then terminates the line if the current logging level ( <code>{@link #LOGLEVEL}</code>) is smaller or equal than the given loglevel parameter.
	 * @param text  The <code>String</code> to be printed.
	 * @param loglevel The logging level of this output.
	 * */
	public static void println(String text, int loglevel) {
		if (loglevel >= LOGLEVEL) {
			System.out.println (text);
		}
	}
	
	/** Prints the text to the standard output if the current logging level ( <code>{@link #LOGLEVEL}</code>) is smaller or equal than the given loglevel parameter.
	 * Can also add a timestamp to the output.
	 * @param text  The <code>String</code> to be printed.
	 * @param loglevel The logging level of this output.
	 * @param withTimestamp adds a timestamp to the output
	 * */
	public static void print(String text, int loglevel, boolean withTimestamp) {
		if (withTimestamp) {
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			Log.print(ts + " " + text, loglevel);
		} else {
			Log.print(text, loglevel);
		}			
		
	}
	
	/** Prints the text to the standard output then terminates the line if the current logging level ( <code>{@link #LOGLEVEL}</code>) is smaller or equal than the given loglevel parameter.
	 * Can also add a timestamp to the output.
	 * @param text  The <code>String</code> to be printed.
	 * @param loglevel The logging level of this output.
	 * @param withTimestamp adds a timestamp to the output
	 * */
	public static void println(String text, int loglevel, boolean withTimestamp) {
		if (withTimestamp) {
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			Log.println(ts + " " + text, loglevel);
		} else {
			Log.println(text, loglevel);
		}			
		
	}
	
	//TODO add output to file

}
