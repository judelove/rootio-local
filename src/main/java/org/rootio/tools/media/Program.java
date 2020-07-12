package org.rootio.tools.media;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.rootio.tools.radio.ScheduleBroadcastHandler;
import org.rootio.tools.utils.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class Program implements Comparable<Program>, ScheduleNotifiable {

    private String title;
    private Date startDate, endDate;
    private int playingIndex;
    final Context parent;
    private ArrayList<ProgramAction> programActions;
    private boolean isLocal;
    private ScheduleBroadcastHandler alertHandler;

    public Program(Context parent, String title, Date start, Date end, String structure) {
        this.parent = parent;
        this.title = title;
        this.startDate = start;
        this.endDate = end;
        this.alertHandler = new ScheduleBroadcastHandler(this);
        this.loadProgramActions(structure);
    }

    public void stop() {
        try {
            this.programActions.get(this.playingIndex).stop();
            this.parent.unregisterReceiver(this.alertHandler);
        } catch (Exception ex) {

        }
    }

    public void pause() {
        this.programActions.get(this.playingIndex).pause();
    }

    public void resume() {
        this.programActions.get(this.playingIndex).resume();
    }

    private void loadProgramActions(String structure) {
        this.programActions = new ArrayList<>();
        JSONArray programStructure;
        try {
            programStructure = new JSONArray(structure);
            ArrayList<String> playlists = new ArrayList<String>();
            ArrayList<String> streams = new ArrayList<String>();
            int duration =0;
            for (int i = 0; i < programStructure.length(); i++) {
                if (programStructure.getJSONObject(i).getString("type").toLowerCase().equals("music"))//redundant, safe
                {
                    //accumulate playlists
                    playlists.add(programStructure.getJSONObject(i).getString("name"));
                    this.isLocal = true;
                }
                if (programStructure.getJSONObject(i).getString("type").toLowerCase().equals("stream"))//redundant, safe
                {
                    //accumulate playlists
                    streams.add(programStructure.getJSONObject(i).getString("stream_url"));
                    this.isLocal = true;
                }
                if(programStructure.getJSONObject(i).has("duration")) { //redundant, using optInt
                    duration = programStructure.getJSONObject(i).optInt("duration");
                }
            }

            this.programActions.add(new ProgramAction(this.parent, playlists, streams, ProgramActionType.Audio, duration));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Returns the title of this program
     *
     * @return String representation of the title of this program
     */
    public String getTitle() {
        return this.title;
    }


    public void run() {
        this.runProgram(0);
        //this.setupAlertReceiver(programActions);
    }

    public ArrayList<ProgramAction> getProgramActions() {
        return this.programActions;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public boolean isLocal()
    {
        return this.isLocal;
    }

    @Override
    public int compareTo(Program another) {
        return this.startDate.compareTo(another.getStartDate());
    }

    private void setupAlertReceiver(ArrayList<ProgramAction> programActions) {
        IntentFilter intentFilter = new IntentFilter();
        AlarmManager am = (AlarmManager) this.parent.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < programActions.size(); i++) {
            intentFilter.addAction("org.rootio.RadioRunner." + this.title + String.valueOf(i));
        }

        alertHandler = new ScheduleBroadcastHandler(this);
        this.parent.registerReceiver(alertHandler, intentFilter);
        for (int i = 0; i < programActions.size(); i++) {
            Intent intent = new Intent("org.rootio.RadioRunner." + this.title + String.valueOf(i));
            intent.putExtra("index", i);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.parent, 0, intent, 0);
            am.set(AlarmManager.RTC_WAKEUP, this.startDate.getTime(), pendingIntent);
        }
    }


    @Override
    public void runProgram(int currentIndex) {
        this.programActions.get(currentIndex).run();
    }

    @Override
    public void stopProgram(Integer index) {
        this.programActions.get(index).stop();

    }

    @Override
    public boolean isExpired(int index) {
        Calendar referenceCalendar = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, this.programActions.get(index).getDuration() - 1); //fetch the duration from the DB for each program action
        return this.endDate.compareTo(referenceCalendar.getTime()) <= 0;
    }

    @Override
    public void finalize()
    {

    }

}
