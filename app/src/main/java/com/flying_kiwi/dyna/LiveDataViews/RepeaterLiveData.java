package com.flying_kiwi.dyna.LiveDataViews;

import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.flying_kiwi.dyna.R;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class RepeaterLiveData extends BaseLiveDataView {
    int setNum = 0;
    int repNum = 0;
    int countdownLeft = 0;
    private ToneGenerator toneGen;

    // Phase types for the session timeline
    enum PhaseType { COUNTDOWN, WORK, REST, PAUSE, DONE }

    static class Phase {
        PhaseType type;
        int durationSec;
        int setNum;
        int repNum;
        Phase(PhaseType type, int durationSec, int setNum, int repNum) {
            this.type = type;
            this.durationSec = durationSec;
            this.setNum = setNum;
            this.repNum = repNum;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.repeater_live_data_fragment, container, false);

        toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        lineChart = view.findViewById(R.id.lineChartRepeater);
        Button btnStart = view.findViewById(R.id.btnRepeaterStart);
        Button btnStop = view.findViewById(R.id.btnRepeaterStop);
        Button btnSave = view.findViewById(R.id.btnRepeaterSave);
        Button btnExport = view.findViewById(R.id.btnRepeaterExport);
        initConnectionIndicator(view, btnStart);

        if (isHistorical) {
            view.findViewById(R.id.txtPullRest).setVisibility(View.GONE);
            view.findViewById(R.id.txtCountdown).setVisibility(View.GONE);
            view.findViewById(R.id.txtWorkTime).setVisibility(View.GONE);
            view.findViewById(R.id.txtRepeaterSetNum).setVisibility(View.GONE);
            view.findViewById(R.id.txtCriticalRepNum).setVisibility(View.GONE);
            displayChart();
            updateStats();
        } else {
            timeLimit = session.getWorkTime();
            displayChart();
            updateStats();
            TextView txtWorkTime = view.findViewById(R.id.txtWorkTime);
            txtWorkTime.setText("0/" + session.getWorkTime() + "s");
        }

        initializeStartStopSaveExportButtons(btnStart, btnStop, btnSave, btnExport);
        btnStart.setOnClickListener(v -> {
            btnStop.setEnabled(true);
            btnSave.setEnabled(false);
            btnExport.setEnabled(false);
            v.setEnabled(false);
            startTimer();
        });
        btnStop.setOnClickListener(v -> {
            btnExport.setEnabled(true);
            btnSave.setEnabled(true);
            v.setEnabled(false);
            btnStart.setEnabled(true);
            stopTimer();
        });

        if (session.getTargetWeight() > 0) {
            setLineLimits();
        }
        return view;
    }

    private void playTone() {
        if (session.isSound() && toneGen != null) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
    }

    @Override
    public void updateStats() {
        if (view == null) {
            return;
        }
        ((TextView) view.findViewById(R.id.txtCriticalCurrent)).setText(session.getLatest().toString());
        ((TextView) view.findViewById(R.id.txtCriticalRepNum)).setText((repNum + 1) + "/" + session.getNumReps());
        ((TextView) view.findViewById(R.id.txtRepeaterSetNum)).setText((setNum + 1) + "/" + session.getNumSets());
        ((TextView) view.findViewById(R.id.txtCountdown)).setText(String.valueOf(countdownLeft));
        if (isWorkPhase) {
            ((TextView) view.findViewById(R.id.txtPullRest)).setText("Pull");
            TextView txtWorkTime = view.findViewById(R.id.txtWorkTime);
            int requiredWorkTime = session.getWorkTime();
            txtWorkTime.setText(String.format("%ds/%ds", accumulatedWorkTime, requiredWorkTime));
        } else {
            ((TextView) view.findViewById(R.id.txtPullRest)).setText("Rest");
            ((TextView) view.findViewById(R.id.txtWorkTime)).setText("0/" + session.getWorkTime() + "s");
        }
    }

    @Override
    int getSaveButtonId() {
        return R.id.btnRepeaterSave;
    }

    int elapsedSeconds = 0;
    boolean isWorkPhase = false;
    CountDownTimer countDownTimer;
    private int accumulatedWorkTime = 0;
    private long lastTickTimestamp = 0;
    private boolean isTransitioningToRest = false;

    private void startTimer() {
        elapsedSeconds = 0;
        setNum = 0;
        repNum = 0;
        accumulatedWorkTime = 0;
        isWorkPhase = false;
        isTransitioningToRest = false;
        lastTickTimestamp = System.currentTimeMillis();

        // Build phase timeline
        final List<Phase> phases = new ArrayList<>();
        phases.add(new Phase(PhaseType.COUNTDOWN, session.getCountdown(), 0, 0));
        for (int s = 0; s < session.getNumSets(); s++) {
            for (int r = 0; r < session.getNumReps(); r++) {
                phases.add(new Phase(PhaseType.WORK, session.getWorkTime(), s, r));
                boolean isLastRepOfLastSet = (s == session.getNumSets() - 1) && (r == session.getNumReps() - 1);
                if (!isLastRepOfLastSet) {
                    phases.add(new Phase(PhaseType.REST, session.getRestTime(), s, r));
                }
            }
            if (s < session.getNumSets() - 1) {
                phases.add(new Phase(PhaseType.PAUSE, session.getPauseTime(), s, -1));
            }
        }
        phases.add(new Phase(PhaseType.DONE, 0, 0, 0));

        int totalDuration = 0;
        for (Phase p : phases) {
            totalDuration += p.durationSec;
        }

        countDownTimer = new CountDownTimer(totalDuration * 1000L, 1000) {
            int phaseIndex = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                long now = System.currentTimeMillis();
                long deltaMs = now - lastTickTimestamp;
                lastTickTimestamp = now;

                Phase currentPhase = phases.get(phaseIndex);
                int phaseStartSec = 0;
                for (int i = 0; i < phaseIndex; i++) {
                    phaseStartSec += phases.get(i).durationSec;
                }
                int phaseElapsed = elapsedSeconds - phaseStartSec;

                if (phaseElapsed >= currentPhase.durationSec && currentPhase.type != PhaseType.DONE) {
                    phaseIndex++;
                    currentPhase = phases.get(phaseIndex);
                    playTone();

                    // Update state for new phase
                    switch (currentPhase.type) {
                        case WORK:
                            isWorkPhase = true;
                            isTransitioningToRest = false;
                            setNum = currentPhase.setNum;
                            repNum = currentPhase.repNum;
                            accumulatedWorkTime = 0;
                            dc.startCollecting();
                            break;
                        case REST:
                        case PAUSE:
                            isWorkPhase = false;
                            isTransitioningToRest = false;
                            accumulatedWorkTime = 0;
                            // Delay stopCollecting to capture transition curve
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                if (!isWorkPhase) dc.stopCollecting();
                            }, 1500);
                            break;
                        case COUNTDOWN:
                            isWorkPhase = false;
                            isTransitioningToRest = false;
                            accumulatedWorkTime = 0;
                            dc.stopCollecting();
                            break;
                        case DONE:
                            isWorkPhase = false;
                            isTransitioningToRest = false;
                            dc.stopScanning();
                            break;
                    }
                }

                // During work phase, only advance time when actually pulling
                if (currentPhase.type == PhaseType.WORK && !isTransitioningToRest) {
                    if (deltaMs >= 900) {
                        float latestWeight = session.getLatest().getWeight();
                        float minThreshold = session.getTargetWeight() - session.getTargetMarginMin();
                        if (latestWeight >= minThreshold) {
                            elapsedSeconds++;
                            accumulatedWorkTime++;
                            int requiredWorkTime = session.getWorkTime();
                            TextView txtWorkTime = view.findViewById(R.id.txtWorkTime);
                            txtWorkTime.setText(String.format("%ds/%ds", accumulatedWorkTime, requiredWorkTime));

                            // If accumulated work time reaches required work time, force transition
                            if (accumulatedWorkTime >= requiredWorkTime) {
                                isTransitioningToRest = true;
                                // Advance elapsed to trigger phase transition on next tick
                                int ps = 0;
                                for (int i = 0; i <= phaseIndex; i++) {
                                    ps += phases.get(i).durationSec;
                                }
                                elapsedSeconds = ps;
                            }
                        }
                    }
                } else {
                    // During countdown/rest/pause, time always advances
                    if (deltaMs >= 900) {
                        elapsedSeconds++;
                    }
                }

                // Update countdown display
                int phaseStartSec2 = 0;
                for (int i = 0; i < phaseIndex; i++) {
                    phaseStartSec2 += phases.get(i).durationSec;
                }
                countdownLeft = currentPhase.durationSec - (elapsedSeconds - phaseStartSec2);
                if (countdownLeft < 0) countdownLeft = 0;

                // Color countdown red when low
                if (countdownLeft <= 3 && countdownLeft > 0) {
                    ((MaterialTextView) view.findViewById(R.id.txtCountdown)).setTextColor(Color.RED);
                } else {
                    int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean isDarkMode = (nightMode == Configuration.UI_MODE_NIGHT_YES);
                    ((MaterialTextView) view.findViewById(R.id.txtCountdown)).setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                }

                updateStats();
            }

            @Override
            public void onFinish() {
                dc.stopScanning();
                isWorkPhase = false;
                if (session.isSound() && toneGen != null) {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);
                }
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        dc.stopScanning();
        isWorkPhase = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (dc != null) {
            dc.stopScanning();
        }
    }
}
