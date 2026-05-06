package com.flying_kiwi.dyna.LiveDataViews;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import com.flying_kiwi.dyna.R;
import com.flying_kiwi.dyna.Utils.DataCollector;
import com.flying_kiwi.dyna.Utils.FileManager;
import com.flying_kiwi.dyna.Session;
import com.flying_kiwi.dyna.TimestampedWeight;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class BaseLiveDataView extends Fragment {

    Session session;
    LineChart lineChart;
    long timeLimit = 30000;
    boolean isHistorical = false;
    DataCollector dc;
    View view;
    TextView connectionIndicator;
    private long lastDataTime = 0;
    private android.os.Handler connectionTimeoutHandler;
    private Runnable connectionTimeoutRunnable;

    Consumer<TimestampedWeight> callback = tsw -> {
        if (session != null) {
            session.addWeight(tsw);
        }
        if (view != null) {
            lastDataTime = System.currentTimeMillis();
            updateConnectionIndicator(true);
            updateStats();
            displayChart();
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;
        BluetoothManager bluetoothMgr = (BluetoothManager) activity.getSystemService(BLUETOOTH_SERVICE);

        assert getArguments() != null;
        session = (Session)getArguments().get("session");
        isHistorical = getArguments().getBoolean("historical", false);

        if(!isHistorical) dc = new DataCollector(bluetoothMgr, callback);
        return super.onCreateView(inflater, container, savedInstanceState);

    }

    protected void initConnectionIndicator(View root, Button btnStart) {
        connectionIndicator = root.findViewById(R.id.txtConnection);
        connectionTimeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        connectionTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isHistorical && lastDataTime > 0 && System.currentTimeMillis() - lastDataTime > 2000) {
                    updateConnectionIndicator(false);
                }
                connectionTimeoutHandler.postDelayed(this, 500);
            }
        };
        if (connectionIndicator != null && !isHistorical) {
            updateConnectionIndicator(false);
            final Button startBtn = btnStart;
            if (startBtn != null) {
                startBtn.setEnabled(false);
            }
            if (dc != null) {
                dc.setOnDeviceFoundCallback(() -> {
                    if (view != null) {
                        view.post(() -> {
                            updateConnectionIndicator(true);
                            lastDataTime = System.currentTimeMillis();
                            if (startBtn != null) {
                                startBtn.setEnabled(true);
                            }
                            connectionTimeoutHandler.post(connectionTimeoutRunnable);
                        });
                    }
                });
            }
        }
    }

    private void updateConnectionIndicator(boolean isConnected) {
        if (connectionIndicator == null) return;
        if (isConnected) {
            connectionIndicator.setText("\u25CF");
            connectionIndicator.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            connectionIndicator.setText("\u25CF");
            connectionIndicator.setTextColor(Color.parseColor("#F44336"));
        }
    }

    public void setLineLimits(){
        if (lineChart == null) {
            return;
        }
        if (session.getTargetWeight() <= 0) {
            return;
        }
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMaximum(session.getPlotMax() + 10f);
        leftAxis.setAxisMinimum(0f);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setAxisMaximum(session.getPlotMax() + 10f);
        rightAxis.setAxisMinimum(0f);

        LimitLine llPlotMin = new LimitLine(session.getPlotMin(), "Min");
        LimitLine llPlotMax = new LimitLine(session.getPlotMax(), "Max");

        llPlotMin.setLineWidth(2f);
        llPlotMin.setLineColor(Color.GREEN);
        llPlotMin.enableDashedLine(10f, 10f, 0f);

        llPlotMax.setLineWidth(2f);
        llPlotMax.setLineColor(Color.RED);
        llPlotMax.enableDashedLine(10f, 10f, 0f);

        leftAxis.addLimitLine(llPlotMin);
        leftAxis.addLimitLine(llPlotMax);
    }
    public abstract void updateStats();

    ArrayList<Entry> lineChartDataPoints = new ArrayList<>();

    public void displayChart(){
        if (lineChart == null) {
            return;
        }
        if(isHistorical){
            displayHistoricalChart();
            return;
        }
        if (session.getWeights().isEmpty()) {
            LineData emptyData = new LineData();
            lineChart.setData(emptyData);
            lineChart.getLegend().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            applyThemeColors(lineChart);
            lineChart.invalidate();
            return;
        }

        lineChartDataPoints.clear();
        long startTime = session.getWeights().get(0).getTimestamp();
        for (TimestampedWeight w : session.getWeights()) {
            lineChartDataPoints.add(new Entry((float) (w.getTimestamp() - startTime) / 1000, w.getWeight()));
        }

        LineDataSet lineDataSet = new LineDataSet(lineChartDataPoints, null);
        lineDataSet.setCircleRadius(2f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawValues(false);
        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        applyThemeColors(lineChart);

        float latestX = lineChartDataPoints.get(lineChartDataPoints.size() - 1).getX();
        float visibleRange = Math.max(30f, latestX + 2f);
        if (visibleRange > 30f) {
            float maxVisible = 30f;
            lineChart.setVisibleXRangeMaximum(maxVisible);
            lineChart.moveViewToX(latestX + 1f);
        }

        lineChart.invalidate();
    }

    public void displayHistoricalChart(){
        if (lineChart == null) {
            return;
        }
        LineData lineData;
        LineDataSet lineDataSet;
        if(!session.getWeights().isEmpty()) {
            long startTime = session.getWeights().get(0).getTimestamp();
            for (TimestampedWeight weight : session.getWeights()) {
                lineChartDataPoints.add(new Entry((float) (weight.getTimestamp() - startTime) / 1000, weight.getWeight()));
            }
            lineDataSet = new LineDataSet(lineChartDataPoints, null);
            lineDataSet.setCircleRadius(2f);
            lineData = new LineData(lineDataSet);
            lineChart.setData(lineData);
            lineChart.getLegend().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            applyThemeColors(lineChart);
            lineChart.invalidate();
        }
    }

    private void applyThemeColors(LineChart chart) {
        int textColor;
        int gridColor;
        int lineColor;
        int circleColor;

        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = (nightMode == Configuration.UI_MODE_NIGHT_YES);

        if (isDarkMode) {
            textColor = Color.WHITE;
            gridColor = Color.argb(80, 255, 255, 255);
            lineColor = Color.argb(255, 110, 171, 113);
            circleColor = Color.WHITE;
        } else {
            textColor = Color.BLACK;
            gridColor = Color.argb(80, 0, 0, 0);
            lineColor = Color.argb(255, 110, 171, 113);
            circleColor = Color.BLACK;
        }

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(textColor);
        xAxis.setGridColor(gridColor);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setGridColor(gridColor);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setTextColor(textColor);
        rightAxis.setGridColor(gridColor);

        if (chart.getData() != null) {
            LineData data = chart.getData();
            if (data.getDataSetCount() > 0) {
                LineDataSet dataSet = (LineDataSet) data.getDataSetByIndex(0);
                dataSet.setColor(lineColor);
                dataSet.setCircleColor(circleColor);
                dataSet.setDrawCircles(true);
            }
        }

        chart.setBackgroundColor(isDarkMode ? Color.argb(255, 30, 30, 30) : Color.TRANSPARENT);
    }

    void showSaveSessionDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Enter session name");
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 51, getResources().getDisplayMetrics());
        input.setHeight(height);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        input.setPadding(padding,0,padding,0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Save Session")
                .setMessage("Please enter a name for the session")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String fileName = input.getText().toString().replace('/','_').trim();

                    if (!fileName.isEmpty()) {
                        FileManager fm = new FileManager(requireContext());
                        session.setName(fileName);
                        fm.saveSession(session);
                        requireActivity().findViewById(getSaveButtonId()).setEnabled(false);
                    } else {
                        Toast.makeText(requireContext(), "Session name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    abstract int getSaveButtonId();
    private void saveFileWithName(String fileName) {
        FileManager fm = new FileManager(requireContext());
        fm.saveSession(session);
        Toast.makeText(requireContext(), "File '" + fileName + "' saved", Toast.LENGTH_SHORT).show();
    }

    public void initializeStartStopSaveExportButtons(Button btnStart, Button btnStop, Button btnSave, Button btnExport) {
        if(!isHistorical) {
            btnStart.setOnClickListener(v -> {
                btnExport.setEnabled(false);
                btnSave.setEnabled(false);
                btnStop.setEnabled(true);
                v.setEnabled(false);
                dc.startCollecting();
            });
            btnStop.setOnClickListener(v -> {
                Log.d("stop", "scan stopped");
                btnExport.setEnabled(true);
                btnSave.setEnabled(true);
                v.setEnabled(false);
                dc.stopScanning();
            });

            btnSave.setOnClickListener(v -> {
                Log.d("Save", "Saving");
                showSaveSessionDialog();
                if(session.getName() != null){
                    v.setEnabled(false);
                }
            });

        }else {
            btnStart.setVisibility(View.INVISIBLE);
            btnStop.setVisibility(View.INVISIBLE);
            btnSave.setVisibility(View.INVISIBLE);
            btnExport.setEnabled(true);
        }
        btnExport.setOnClickListener(v -> {
            FileManager fm = new FileManager(requireContext());
            fm.exportSessionToCSV(session);
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(dc != null){
            dc.stopScanning();
        }
        if (connectionTimeoutHandler != null) {
            connectionTimeoutHandler.removeCallbacksAndMessages(null);
        }
    }

}
