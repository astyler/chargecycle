package edu.cmu.ri.createlab.chargecycle;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import edu.cmu.ri.createlab.chargecycle.comm.CommunicationsThread;
import edu.cmu.ri.createlab.chargecycle.comm.Communicator;
import edu.cmu.ri.createlab.chargecycle.logging.EventLogger;
import edu.cmu.ri.createlab.chargecycle.logging.StateLogger;
import edu.cmu.ri.createlab.chargecycle.model.State;
import edu.cmu.ri.createlab.chargecycle.model.VehicleState;
import edu.cmu.ri.createlab.chargecycle.view.ViewThread;

public class ChargeCycle {
	public static void main(String[] args) {
		File logFileDirectory = new File(args[0]);

		State state = new State();
		EventLogger eventLogger = new EventLogger(new File(logFileDirectory, "CCEventLog.txt"), false);
		Communicator comms = new Communicator(eventLogger, state);
		StateLogger stateLogger = new StateLogger(logFileDirectory);

		SwingUtilities.invokeLater(new ViewThread(state, eventLogger, logFileDirectory));

		SwingWorker<Boolean, Void> commThread = new CommunicationsThread(state, comms, eventLogger);
		commThread.execute();

		// state will be alive until window is closed, comms fail to establish,
		// or comms establish and then the key is turned off
		// VehicleState prevState = state.getVehicleState();
		try {
			stateLogger.startLogging();
		} catch (IOException e) {			
			String msg = "Problem creating state logger file: "+stateLogger.getLogName();
			eventLogger.logEvent(msg);
			eventLogger.logException(e);
			System.err.println(msg);
		}
		int loopValue = 0;
		int logFlush = 0;
		
		while (state.isAlive()) {
			try {
				loopValue++;
				VehicleState currState = state.getVehicleState();

				if (loopValue % 5 == 0) {
					if(currState != null){
						if(!currState.isBatteryCharging() || loopValue % 100 == 0){
							//if not charging, record state. or if we are charging, record less frequently
							stateLogger.writeState(currState);
						}
					} else stateLogger.writeString("Null state");
					
				}
				// if(currState != prevState && currState != null){
				/*
				if (currState != null) {					
					// set state logging frequency based on state:
					// if charging, every 100 loops, or about 10 seconds
					// if not, every 5 loops, or about half a second
					int recordingFrequency = currState.isBatteryCharging() ? 100 : 5;
					if (loopValue % recordingFrequency == 0) {
						stateLogger.writeState(currState);
						logFlush++;
						if(logFlush % 10 == 0){
							stateLogger.flushLog();
							logFlush = 0;
						}
						
					}
				}
				*/
				// eventLogger.flushLog();
				// do main thread stuff
				Thread.sleep(100);
				if (loopValue == 100 && (currState == null || comms.getConnected() == false) && commThread.isDone()) {
					eventLogger.logEvent("Retrying bike connect...");
					commThread = new CommunicationsThread(state, comms, eventLogger);
					commThread.execute();
					loopValue = 0;
				}
			} catch (InterruptedException e) {
				System.err.println("Main thread interrupted");
				e.printStackTrace();
			} catch (IOException e) {
				eventLogger.logEvent("Problem writing vehicle state");
				eventLogger.logException(e);
				e.printStackTrace();
			}
		}
		// prevState = null;
		eventLogger.logEvent("Killing communications...");
		commThread.cancel(true);
		if (comms.getConnected()) {
			comms.disconnect();
		}
		try {
			eventLogger.logEvent("Writing log.");
			eventLogger.flushLog();
			stateLogger.stopLogging();
			System.out.println("Log successfully written");
		} catch (IOException e) {
			System.err.println("Error writing event log file");
			e.printStackTrace();
		}

		VehicleState vState = state.getVehicleState();
		if (vState != null && vState.isKey() == false && vState.isBatteryCharging() == false) {
			try {
				Runtime.getRuntime().exec("pmset sleepnow");
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
		System.exit(0);
	}

}
