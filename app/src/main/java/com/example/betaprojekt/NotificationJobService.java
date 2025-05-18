package com.example.betaprojekt;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class NotificationJobService extends JobService {


    @Override
    public boolean onStartJob(JobParameters params) {
        new NotificationHandler(getApplicationContext())
                .send("Nézz körül az ajánlataink között!");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
