package org.rootio.services;

import org.json.JSONObject;
import org.rootio.tools.diagnostics.DiagnosticAgent;
import org.rootio.tools.persistence.DBAgent;
import org.rootio.tools.utils.EventAction;
import org.rootio.tools.utils.EventCategory;
import org.rootio.tools.utils.Utils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiagnosticsService implements RootioService {

    private boolean isRunning;
    private int serviceId = 3;
    private Thread runnerThread;


    public boolean start() {
        Utils.logEvent(EventCategory.SERVICES, EventAction.START, "Diagnostics Service");
        if (!this.isRunning) {
            long delay = this.getDelay();
            delay = delay > 0 ? this.getMillisToSleep("seconds", delay) : 10000; // 10000 default
            DiagnosticsRunner diagnosticsRunner = new DiagnosticsRunner(delay);
            runnerThread = new Thread(diagnosticsRunner);
            runnerThread.start();
            this.isRunning = true;
        }
        return isRunning;
    }

    @Override
    public void stop() {
        Utils.logEvent(EventCategory.SERVICES, EventAction.STOP, "Diagnostics Service");
        try
        {
            this.shutDownService();
        }catch(Exception e)
        {
            Logger.getLogger("RootIO").log(Level.INFO, e.getMessage() == null ? "Null pointer[DiagnosticsService.stop]" : e.getMessage());
        }
        new ServiceState(3,"Diagnostic", 0).save();
    }

    private void shutDownService() {
                 this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    private long getDelay() {
        try {
            JSONObject frequencies = new JSONObject((String)Utils.loadPreferencesFile("frequencies",String.class, this));
            return frequencies.getJSONObject("diagnostics").getInt("interval");
        } catch (Exception ex) {
            return 180; // default to 3 mins
        }
    }

    /**
     * Get the time in milliseconds for which to sleep given the unit and
     * quantity
     *
     * @param units   The measure of the interval supplied by the cloud. This will always be seconds hence this is redundant
     * @param quantity The quantity of units to be used in measuring time
     * @return The amount of time in milliseconds
     */
    private long getMillisToSleep(String units, long quantity) {
        switch (units) {
            case "hours":
                return quantity * 3600 * 1000;
            case "minutes":
                return quantity * 60 * 1000;
            case "seconds":
                return quantity * 1000;
            default:
                return this.getMillisToSleep("minutes", quantity);
        }
    }

    class DiagnosticsRunner implements Runnable {
        private DiagnosticAgent diagnosticAgent;
        private long delay;

        DiagnosticsRunner(long delay) {
            this.diagnosticAgent = new DiagnosticAgent();
            this.delay = delay;
        }

        @Override
        public void run() {
            while (isRunning) {
                diagnosticAgent.runDiagnostics();
                this.logToDB();
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Logger.getLogger("RootIO").log(Level.INFO, e.getMessage() == null ? "Null pointer[DiagnosticsRunner.run]" : e.getMessage());
                }
            }
        }

        /**
         * Saves the diagnostics gathered to the database
         */
        private void logToDB() {
            String tableName = "diagnostic";
            HashMap<String, Object> values = new HashMap<>();
            values.put("batterylevel", diagnosticAgent.getBatteryLevel());
            values.put("memoryutilization", diagnosticAgent.getMemoryStatus());
            values.put("storageutilization", diagnosticAgent.getStorageStatus());
            values.put("CPUutilization", diagnosticAgent.getCPUUtilization());
            values.put("wificonnected", diagnosticAgent.isConnectedToWifi());
            values.put("firstmobilenetworkname", diagnosticAgent.getTelecomOperatorName());
            values.put("firstmobilenetworkconnected", diagnosticAgent.isConnectedToMobileNetwork());
            values.put("firstmobilenetworkstrength", diagnosticAgent.getMobileSignalStrength());
            values.put("firstmobilenetworktype", diagnosticAgent.getMobileNetworkType());
            values.put("latitude", diagnosticAgent.getLatitude());
            values.put("longitude", diagnosticAgent.getLongitude());
            try {
                DBAgent.saveData(tableName,  values);
            } catch (SQLException e) {
                Logger.getLogger("RootIO").log(Level.SEVERE, e.getMessage() == null ? "Null pointer[DiagnosticsRunner.run]" : e.getMessage());
            }
        }
    }

}
