package org.rootio.tools.radio;

import org.rootio.activities.services.TelephonyEventNotifiable;
import org.rootio.launcher.Rootio;
import org.rootio.messaging.BroadcastReceiver;
import org.rootio.messaging.Message;
import org.rootio.messaging.MessageRouter;
import org.rootio.tools.media.Program;
import org.rootio.tools.media.ScheduleChangeNotifiable;
import org.rootio.tools.media.ScheduleNotifiable;
import org.rootio.tools.persistence.DBAgent;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

enum State {
    PLAYING, PAUSED, STOPPED
}

public class RadioRunner implements Runnable, TelephonyEventNotifiable, ScheduleNotifiable, ScheduleChangeNotifiable {
    private static RadioRunner runner;
    private ArrayList<Program> programs;
    private Integer runningProgramIndex = null;
    private State state;
    private BroadcastReceiver telephonyEventBroadcastReceiver, scheduleChangeNotificationReceiver;
    private boolean isPendingScheduleReload;

    private RadioRunner() {
        this.listenForScheduleChangeNotifications();
        this.listenForTelephonyEvents();
    }

    public static RadioRunner getInstance() {
        if (runner == null) {
            runner = new RadioRunner();
        }
        return runner;
    }

    private void listenForScheduleChangeNotifications()
    {
        this.scheduleChangeNotificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Message m) {
                boolean shouldRestart = (boolean)m.getPayLoad().get("shouldRestart");
                RadioRunner.this.notifyScheduleChange(shouldRestart);
            }
        };
        MessageRouter.getInstance().register(this.scheduleChangeNotificationReceiver, "org.rootio.services.synchronization.SCHEDULE_CHANGE_EVENT");
    }

    private void listenForTelephonyEvents()
    {
        this.telephonyEventBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Message m) {
                String event = m.getEvent();
                switch (event)
                {
                    case "RINGING":
                    case "INCALL":
                        pauseProgram();
                        break;
                    case "IDLE":
                        resumeProgram();
                        break;
                }
            }
        };
        MessageRouter.getInstance().register(this.telephonyEventBroadcastReceiver, "org.rootio.services.sip.TELEPHONY");
        MessageRouter.getInstance().register(this.telephonyEventBroadcastReceiver, "org.rootio.services.phone.TELEPHONY");
    }

    @Override
    public void run() {
        initialiseSchedule();
    }

    private void initialiseSchedule() {
        runningProgramIndex = null;
        this.programs = fetchPrograms();
        this.schedulePrograms(programs);
    }

    /**
     * Runs the program whose index is specified from the programs lined up
     *
     * @param index The index of the program to run
     */
    public synchronized void runProgram(int index) {
        if (this.isPendingScheduleReload) {
            this.isPendingScheduleReload = false;
            this.restartProgramming();
        }
        if (this.runningProgramIndex != null && !this.isExpired(index)) {
            this.stopProgram(this.runningProgramIndex);
        }
        this.runningProgramIndex = index;
        // Check to see that we are not in a phone call before launching program

        if (!Rootio.isInCall() && !Rootio.isInSIPCall()){
        this.state = State.PLAYING;
        this.programs.get(index).run();
        }
    }

    /**
     * Pauses the running program
     */
    private void pauseProgram() {
        if (this.runningProgramIndex != null) {
            this.programs.get(this.runningProgramIndex).pause();
        }
    }

    /**
     * Resumes the program that is currently playing if it was paused before
     */
    private void resumeProgram() {
        if (this.runningProgramIndex != null) {
            this.programs.get(this.runningProgramIndex).resume();
        }
    }

    /**
     * Stops the program that is currently running
     */
    public void stopProgram(Integer index) {
        if (index != null) {
            try {
                this.programs.get(this.runningProgramIndex).stop();
            } catch (NullPointerException e) {
                Logger.getLogger("RootIO").log(Level.WARNING, e.getMessage() == null ? "Null pointer[RadioRunner.stopProgram]" : e.getMessage());
            }
        }
        if (this.state != State.PAUSED) {
            this.state = State.STOPPED;
        }
    }

    public void stop() {
        this.stopProgram(this.runningProgramIndex);
    }


    /**
     * Returns all the program slots scheduled
     *
     * @return ArrayList of ProgramSlot objects each representing a scheduled
     * program
     */
    public ArrayList<Program> getPrograms() {
        return this.programs;
    }

    /**
     * Gets the running program
     *
     * @return The currently running program
     */
    public Program getRunningProgram() {
        try {
            return this.programs.get(this.runningProgramIndex);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    /**
     * Schedules the supplied programs according to their schedule information
     *
     * @param programs ArrayList of the programs to be scheduled
     */
    private void schedulePrograms(ArrayList<Program> programs) {
        // Sort the program slots by time at which they will play
        Collections.sort(programs);

        // Schedule the program slots
        for (int i = 0; i < programs.size(); i++) {
            if (i == 0 || (programs.get(i).getStartDate() != programs.get(i - 1).getStartDate())) {
                ScheduleBroadcastHandler hlr = new ScheduleBroadcastHandler(RadioRunner.this, i);
                if (programs.get(i).isLocal()) { // no point scheduling non local progs
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            hlr.run();
                        }
                    }, programs.get(i).getStartDate());
                }
            }
        }
    }

    /**
     * This clears all scheduled events
     */
    private void resetSchedule() {
        //for (Object[] pi : this.pendingIntents) {
            // this.am.cancel((PendingIntent) pi[0]);
       // }

        //this.runningProgramIndex = null;
    }

    /**
     * This clears all scheduled events
     */
    private void deleteFutureSchedule() {
       // for (Object[] pi : this.pendingIntents) {

        //}

        //this.runningProgramIndex = null;
        //this.pis = new ArrayList<>();
    }

    /**
     * Schedules the supplied programs according to their schedule information
     *
     * @param programs ArrayList of the programs to be scheduled
     */
    private void scheduleFuturePrograms(ArrayList<Program> programs) {
        // Sort the program slots by time at which they will play
        Collections.sort(programs);

        // Schedule the program slots
        for (int i = 0; i < programs.size(); i++) {
            if (programs.get(i).isLocal() && programs.get(i).getStartDate().getTime() >= Calendar.getInstance().getTimeInMillis()) { // no point scheduling non local progs
                if (i == 0 || (programs.get(i).getStartDate() != programs.get(i - 1).getStartDate())) //do not double schedule at same time.
                {
                    ScheduleBroadcastHandler hlr = new ScheduleBroadcastHandler(RadioRunner.this, i);
                    if (programs.get(i).isLocal()) { // no point scheduling non local progs
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                hlr.run();
                            }
                        }, programs.get(i).getStartDate());
                    }
                }
            }
        }
    }


    /**
     * Fetches program information as stored in the database
     *
     * @return ArrayList of Program objects each representing a database record
     */
    private ArrayList<Program> fetchPrograms() {
        String query = "select id, name, start, end, structure, program_type_id, deleted from scheduled_program where (date(start) = date(current_timestamp) or date(end) = date(current_timestamp))  and not deleted";
        List<String> args = Collections.emptyList();
        ArrayList<Program> programs = new ArrayList<>();
        List<List<Object>> data;
        try {
            data = DBAgent.getData(query, args);
            for (List<Object> row : data) {
                Program program;
                program = new Program((String)row.get(1), (Date)row.get(2), (Date)row.get(3), (String)row.get(4));
                programs.add(program);
            }
            return programs;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return programs;
    }

    @Override
    public void notifyTelephonyStatus(boolean isInCall) {
        if (isInCall) {
            //it is important to set and check state ASAP. These events may be fired more than once in quick succession
            if (this.state != State.PAUSED) {
                this.state = State.PAUSED;
                this.pauseProgram();
            }
        } else { // notification that the call has ended
            if (this.state != State.PLAYING) {
                // The program had begun, it was paused by the call
                this.state = State.PLAYING;
                this.resumeProgram();
            }
        }
    }

    @Override
    public boolean isExpired(int index) {
        Calendar referenceCalendar = Calendar.getInstance();
        //boolean isExpired = this.programs.get(index).getEndDate().compareTo(referenceCalendar.getTime()) <= 0;
        return this.programs.get(index).getEndDate().compareTo(referenceCalendar.getTime()) <= 0;
    }

    @Override
    public void notifyScheduleChange(boolean shouldRestart) {
        if (shouldRestart) {
            restartProgramming();
        } else {
            this.reloadSchedule();
            //this.isPendingScheduleReload = true;
        }
    }

    private void restartProgramming() {
        this.stopProgram(this.runningProgramIndex);
        this.runningProgramIndex = null;
        this.resetSchedule();
        this.initialiseSchedule();
    }

    private void reloadSchedule() {
        this.deleteFutureSchedule();
        this.programs = this.fetchPrograms();
        this.scheduleFuturePrograms(programs);
    }
}
