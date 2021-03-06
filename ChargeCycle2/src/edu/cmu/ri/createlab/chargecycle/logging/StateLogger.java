package edu.cmu.ri.createlab.chargecycle.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.cmu.ri.createlab.chargecycle.model.VehicleState;

public class StateLogger {
	private final File logFileDirectory;
	private FileWriter fw;
	private String logName;
	private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");

	public StateLogger(File logFileDirectory) {
		this.logFileDirectory = logFileDirectory;
	}

	public synchronized void startLogging() throws IOException {
		this.logFileDirectory.mkdirs();
		this.logName = "CCLog_" + this.fileDateFormat.format(Calendar.getInstance().getTime()) + ".txt";
		File log = new File(this.logFileDirectory, this.logName);
		log.createNewFile();
		this.fw = new FileWriter(log, true);
	}

	public synchronized void writeState(VehicleState vState) throws IOException {
		//no longer needed, time is stored in vehicle state
		//this.fw.write(this.logDateFormat.format(Calendar.getInstance().getTime()) + ", ");
		this.fw.write(vState.toString());
		// fw.write(System.lineSeparator());
		this.fw.write(System.getProperty("line.separator"));

	}
	
	public synchronized void flushLog() throws IOException{
		this.fw.flush();
	}

	public synchronized void writeString(String msg) throws IOException {
		//no msg writing at the time, can infer time between lines
		//this.fw.write(this.logDateFormat.format(Calendar.getInstance().getTime()) + ", ");
		this.fw.write(msg);
		// fw.write(System.lineSeparator());
		this.fw.write(System.getProperty("line.separator"));

	}

	public synchronized void stopLogging() throws IOException {
		this.fw.flush();
		this.fw.close();
	}
	
	public synchronized String getLogName(){
		return this.logName;
	}

}
