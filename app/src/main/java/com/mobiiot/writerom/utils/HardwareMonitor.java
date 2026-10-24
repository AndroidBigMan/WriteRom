package com.mobiiot.writerom.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HardwareMonitor {

    public interface HardwareListener {
        void onHardwareUpdate(float tempCelsius, int thermalLevel, @NonNull String thermalStatusLabel);
    }

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HardwareListener listener;
    private boolean isMonitoring = false;

    private float currentTempCelsius = 0.0f;
    private int currentThermalLevel = 0;
    private String currentThermalLabel = "正常";

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                currentTempCelsius = tempTenths / 10.0f;
                checkThermalStatus();
                dispatchUpdate();
            }
        }
    };

    private Object thermalListenerObj; // OnThermalStatusChangedListener for API 29+

    public HardwareMonitor(@NonNull Context context) {
        this.context = context.getApplicationContext();
        readInitialBatteryTemperature();
        checkThermalStatus();
    }

    public void setListener(@Nullable HardwareListener listener) {
        this.listener = listener;
        if (listener != null) {
            dispatchUpdate();
        }
    }

    public void startMonitoring() {
        if (isMonitoring) return;
        isMonitoring = true;

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.OnThermalStatusChangedListener pmListener = status -> {
                    updateThermalStatusFromLevel(status);
                    dispatchUpdate();
                };
                thermalListenerObj = pmListener;
                try {
                    pm.addThermalStatusListener(pmListener);
                } catch (Exception ignored) {}
            }
        }

        schedulePeriodicCheck();
    }

    public void stopMonitoring() {
        if (!isMonitoring) return;
        isMonitoring = false;

        handler.removeCallbacksAndMessages(null);

        try {
            context.unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListenerObj != null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                try {
                    pm.removeThermalStatusListener((PowerManager.OnThermalStatusChangedListener) thermalListenerObj);
                } catch (Exception ignored) {}
            }
            thermalListenerObj = null;
        }
    }

    private void schedulePeriodicCheck() {
        if (!isMonitoring) return;
        handler.postDelayed(() -> {
            if (isMonitoring) {
                readInitialBatteryTemperature();
                checkThermalStatus();
                dispatchUpdate();
                schedulePeriodicCheck();
            }
        }, 2000); // 每2秒校准一次
    }

    private void readInitialBatteryTemperature() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int tempTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                if (tempTenths > 0) {
                    currentTempCelsius = tempTenths / 10.0f;
                }
            }
        } catch (Exception ignored) {}
    }

    private void checkThermalStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                try {
                    int status = pm.getCurrentThermalStatus();
                    updateThermalStatusFromLevel(status);
                    return;
                } catch (Exception ignored) {}
            }
        }

        // API < 29 根据温度估算
        if (currentTempCelsius > 48.0f) {
            currentThermalLevel = 4;
            currentThermalLabel = "极高热保护";
        } else if (currentTempCelsius > 44.0f) {
            currentThermalLevel = 3;
            currentThermalLabel = "严重热降频";
        } else if (currentTempCelsius > 40.0f) {
            currentThermalLevel = 2;
            currentThermalLabel = "中度发热";
        } else if (currentTempCelsius > 37.0f) {
            currentThermalLevel = 1;
            currentThermalLabel = "轻微温升";
        } else {
            currentThermalLevel = 0;
            currentThermalLabel = "温度正常";
        }
    }

    private void updateThermalStatusFromLevel(int level) {
        currentThermalLevel = level;
        switch (level) {
            case PowerManager.THERMAL_STATUS_NONE:
                currentThermalLabel = "无热降频 (正常)";
                break;
            case PowerManager.THERMAL_STATUS_LIGHT:
                currentThermalLabel = "轻度温升 (性能平稳)";
                break;
            case PowerManager.THERMAL_STATUS_MODERATE:
                currentThermalLabel = "中度发热 (轻微降频)";
                break;
            case PowerManager.THERMAL_STATUS_SEVERE:
                currentThermalLabel = "严重发热 (主控降频)";
                break;
            case PowerManager.THERMAL_STATUS_CRITICAL:
                currentThermalLabel = "临界高温 (降频严重)";
                break;
            case PowerManager.THERMAL_STATUS_EMERGENCY:
                currentThermalLabel = "紧急高温保护";
                break;
            case PowerManager.THERMAL_STATUS_SHUTDOWN:
                currentThermalLabel = "高温关机保护";
                break;
            default:
                currentThermalLabel = "正常 (" + level + ")";
                break;
        }
    }

    private void dispatchUpdate() {
        if (listener != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                listener.onHardwareUpdate(currentTempCelsius, currentThermalLevel, currentThermalLabel);
            } else {
                handler.post(() -> {
                    if (listener != null) {
                        listener.onHardwareUpdate(currentTempCelsius, currentThermalLevel, currentThermalLabel);
                    }
                });
            }
        }
    }

    public float getCurrentTempCelsius() {
        return currentTempCelsius;
    }

    public int getCurrentThermalLevel() {
        return currentThermalLevel;
    }

    @NonNull
    public String getCurrentThermalLabel() {
        return currentThermalLabel;
    }
}
