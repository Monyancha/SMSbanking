package com.khizhny.smsbanking;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.ApplicationTestCase;
import android.util.Log;
import android.widget.Toast;

import static com.khizhny.smsbanking.MyApplication.LOG;


public class SmsStressTest extends ApplicationTestCase<Application> {

    public SmsStressTest() {
        super(Application.class);
    }

    @Override
    public void setUp(){

    }




    @Override
    public void tearDown() throws Exception{
        super.tearDown();
    }
}