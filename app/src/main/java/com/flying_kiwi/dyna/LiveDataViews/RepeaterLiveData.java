package com.flying_kiwi.dyna.LiveDataViews;

import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
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
    private float countdownSec = 0;
    private ToneGenerator toneGen;

    // Phase types for the session timeline
    enum PhaseType {
        COUNTDOWN,
        WORK,
        REST,
        PAUSE,
        DONE,
    }

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
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState
    ) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(
            R.layout.repeater_live_data_fragment,
            container,
            false
        );

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

        initializeStartStopSaveExportButtons(
            btnStart,
            btnStop,
            btnSave,
            btnExport
        );
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
        ((TextView) view.findViewById(R.id.txtCriticalCurrent)).setText(
            session.getLatest().toString()
        );
        ((TextView) view.findViewById(R.id.txtCriticalRepNum)).setText(
            (repNum + 1) + "/" + session.getNumReps()
        );
        ((TextView) view.findViewById(R.id.txtRepeaterSetNum)).setText(
            (setNum + 1) + "/" + session.getNumSets()
        );
        if (isSessionDone) {
            ((TextView) view.findViewById(R.id.txtPullRest)).setText("Done");
        } else {
            ((TextView) view.findViewById(R.id.txtCountdown)).setText(
                String.format("%.1f", countdownSec)
            );
            if (isWorkPhase) {
                ((TextView) view.findViewById(R.id.txtPullRest)).setText(
                    "Pull"
                );
                TextView txtWorkTime = view.findViewById(R.id.txtWorkTime);
                txtWorkTime.setText(
                    String.format(
                        "%.1fs/%ds",
                        accumulatedWorkMs / 1000f,
                        session.getWorkTime()
                    )
                );
            } else {
                ((TextView) view.findViewById(R.id.txtPullRest)).setText(
                    "Rest"
                );
                ((TextView) view.findViewById(R.id.txtWorkTime)).setText(
                    "0/" + session.getWorkTime() + "s"
                );
            }
        }
    }

    @Override
    int getSaveButtonId() {
        return R.id.btnRepeaterSave;
    }

    int elapsedMs = 0;
    boolean isWorkPhase = false;
    private android.os.Handler tickHandler;
    private int accumulatedWorkMs = 0;
    private long collectionStartTime = 0;
    private boolean isSessionDone = false;
    private boolean warningBeepsPlayed = false;

    private void startTimer() {
        elapsedMs = 0;
        setNum = 0;
        repNum = 0;
        accumulatedWorkMs = 0;
        isWorkPhase = false;
        isSessionDone = false;
        warningBeepsPlayed = false;

        // Build phase timeline
        final List<Phase> phases = new ArrayList<>();
        phases.add(
            new Phase(PhaseType.COUNTDOWN, session.getCountdown(), 0, 0)
        );
        for (int s = 0; s < session.getNumSets(); s++) {
            for (int r = 0; r < session.getNumReps(); r++) {
                phases.add(
                    new Phase(PhaseType.WORK, session.getWorkTime(), s, r)
                );
                boolean isLastRepOfLastSet =
                    (s == session.getNumSets() - 1) &&
                    (r == session.getNumReps() - 1);
                if (!isLastRepOfLastSet) {
                    phases.add(
                        new Phase(PhaseType.REST, session.getRestTime(), s, r)
                    );
                }
            }
            if (s < session.getNumSets() - 1) {
                phases.add(
                    new Phase(PhaseType.PAUSE, session.getPauseTime(), s, -1)
                );
            }
        }
        phases.add(new Phase(PhaseType.DONE, 0, 0, 0));

        tickHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final int[] phaseIndex = { 0 };

        tickHandler.post(
            new Runnable() {
                @Override
                public void run() {
                    if (tickHandler == null) return;
                    tickHandler.postDelayed(this, 100);

                    Phase currentPhase = phases.get(phaseIndex[0]);
                    int phaseStartMs = 0;
                    for (int i = 0; i < phaseIndex[0]; i++) {
                        phaseStartMs += phases.get(i).durationSec * 1000;
                    }

                    // During work phase, only advance time when pulling force >= min margin
                    if (currentPhase.type == PhaseType.WORK) {
                        com.flying_kiwi.dyna.TimestampedWeight latest =
                            session.getLatest();
                        float minThreshold =
                            session.getTargetWeight() -
                            session.getTargetMarginMin();
                        if (
                            latest.getWeight() >= minThreshold &&
                            latest.getTimestamp() >= collectionStartTime
                        ) {
                            elapsedMs += 100;
                        }
                        accumulatedWorkMs = elapsedMs - phaseStartMs;
                    } else if (currentPhase.type != PhaseType.DONE) {
                        elapsedMs += 100;
                    }

                    int phaseElapsed = elapsedMs - phaseStartMs;

                    if (
                        phaseElapsed >= currentPhase.durationSec * 1000 &&
                        currentPhase.type != PhaseType.DONE
                    ) {
                        phaseIndex[0]++;
                        currentPhase = phases.get(phaseIndex[0]);
                        playTone();

                        switch (currentPhase.type) {
                            case WORK:
                                isWorkPhase = true;
                                setNum = currentPhase.setNum;
                                repNum = currentPhase.repNum;
                                accumulatedWorkMs = 0;
                                warningBeepsPlayed = false;
                                collectionStartTime =
                                    System.currentTimeMillis();
                                dc.startCollecting();
                                break;
                            case REST:
                            case PAUSE:
                                isWorkPhase = false;
                                accumulatedWorkMs = 0;
                                new android.os.Handler(
                                    android.os.Looper.getMainLooper()
                                ).postDelayed(
                                    () -> {
                                        if (!isWorkPhase) dc.stopCollecting();
                                    },
                                    1500
                                );
                                break;
                            case COUNTDOWN:
                                isWorkPhase = false;
                                accumulatedWorkMs = 0;
                                dc.stopCollecting();
                                break;
                            case DONE:
                                isWorkPhase = false;
                                isSessionDone = true;
                                accumulatedWorkMs = 0;
                                view
                                    .findViewById(R.id.btnRepeaterSave)
                                    .setEnabled(true);
                                view
                                    .findViewById(R.id.btnRepeaterExport)
                                    .setEnabled(true);
                                dc.stopScanning();
                                if (session.isSound() && toneGen != null) {
                                    toneGen.startTone(
                                        ToneGenerator.TONE_PROP_BEEP
                                    );
                                }
                                tickHandler.removeCallbacksAndMessages(null);
                                tickHandler = null;
                                break;
                        }
                    }

                    // Update countdown display
                    countdownSec =
                        (currentPhase.durationSec * 1000 -
                            (elapsedMs - phaseStartMs)) /
                        1000f;
                    if (countdownSec < 0) countdownSec = 0;

                    // Warning beeps on last second of pull
                    if (
                        currentPhase.type == PhaseType.WORK &&
                        countdownSec <= 1.0 &&
                        countdownSec > 0 &&
                        !warningBeepsPlayed
                    ) {
                        warningBeepsPlayed = true;
                        if (session.isSound() && toneGen != null) {
                            toneGen.startTone(
                                ToneGenerator.TONE_PROP_BEEP,
                                200
                            );
                            new android.os.Handler(
                                android.os.Looper.getMainLooper()
                            ).postDelayed(
                                () -> {
                                    if (toneGen != null) toneGen.startTone(
                                        ToneGenerator.TONE_PROP_BEEP,
                                        200
                                    );
                                },
                                250
                            );
                        }
                    }

                    if (countdownSec <= 3 && countdownSec > 0) {
                        (
                            (MaterialTextView) view.findViewById(
                                R.id.txtCountdown
                            )
                        ).setTextColor(Color.RED);
                    } else {
                        int nightMode =
                            getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
                        boolean isDarkMode = (nightMode ==
                            Configuration.UI_MODE_NIGHT_YES);
                        (
                            (MaterialTextView) view.findViewById(
                                R.id.txtCountdown
                            )
                        ).setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                    }
                    updateStats();
                }
            }
        );
    }

    private void stopTimer() {
        if (tickHandler != null) {
            tickHandler.removeCallbacksAndMessages(null);
            tickHandler = null;
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
        if (tickHandler != null) {
            tickHandler.removeCallbacksAndMessages(null);
            tickHandler = null;
        }
        if (dc != null) {
            dc.stopScanning();
        }
    }
}
