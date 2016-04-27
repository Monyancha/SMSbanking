package com.khizhny.smsbanking;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
//*
//* Just a bulk Service to be able to make app SMS dafault.
//*
public class HeadlessSmsSendService extends android.app.Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
