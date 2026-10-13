package com.fittrack.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TrackingService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    // Service for background tracking
}
