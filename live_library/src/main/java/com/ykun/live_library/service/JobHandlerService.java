package com.ykun.live_library.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.ykun.live_library.config.KeepAliveConfig;
import com.ykun.live_library.utils.KeepAliveUtils;

/**
 * 定时器
 * 安卓5.0及以上
 */
@SuppressWarnings(value = {"unchecked", "deprecation"})
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class JobHandlerService extends JobService {
    private String TAG = this.getClass().getSimpleName();
    private static JobScheduler mJobScheduler;

    public static void startJob(Context context) {
        try {
            mJobScheduler = (JobScheduler) context.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(10,
                    new ComponentName(context.getPackageName(),
                            JobHandlerService.class.getName())).setPersisted(true);
            /**
             * I was having this problem and after review some blogs and the official documentation,
             * I realised that JobScheduler is having difference behavior on Android N(24 and 25).
             * JobScheduler works with a minimum periodic of 15 mins.
             *
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //7.0以上延迟1s执行
                builder.setMinimumLatency(KeepAliveConfig.JOB_TIME);
            } else {
                //每隔1s执行一次job
                builder.setPeriodic(KeepAliveConfig.JOB_TIME);
            }
            mJobScheduler.schedule(builder.build());

        } catch (Exception e) {
            Log.e("startJob->", e.getMessage());
        }
    }

    public static void stopJob() {
        if (mJobScheduler != null)
            mJobScheduler.cancelAll();
    }

    private void startService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
/*            if (KeepAliveConfig.foregroundNotification != null) {
                Intent intent2 = new Intent(this, NotificationClickReceiver.class);
                intent2.setAction(NotificationClickReceiver.CLICK_NOTIFICATION);
                Notification notification = NotificationUtils.createNotification(getApplicationContext(), KeepAliveConfig.foregroundNotification.getTitle(), KeepAliveConfig.foregroundNotification.getDescription(), KeepAliveConfig.foregroundNotification.getIconRes(), intent2);
                startForeground(13691, notification);
            }*/
        }
        try {
            Log.i(TAG, "---》启动双进程保活服务");
            //启动本地服务
            Intent localIntent = new Intent(context, LocalService.class);
            //启动守护进程
            Intent guardIntent = new Intent(context, RemoteService.class);
            startService(localIntent);
            startService(guardIntent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        try {
            Log.d("JOB-->", " Job 执行");
            //7.0以上轮询
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                startJob(this);
            }

            if (!KeepAliveUtils.isServiceRunning(getApplicationContext(), "com.ykun.live_library.service.LocalService")) {
                startService(this);
            }

            if (!KeepAliveUtils.isRunningTaskExist(getApplicationContext(), getPackageName() + ":remote")) {
                startService(this);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("JOB-->", " Job 停止");
        if (!KeepAliveUtils.isServiceRunning(getApplicationContext(), "com.ykun.live_library.service.LocalService:local") || !KeepAliveUtils.isRunningTaskExist(getApplicationContext(), getPackageName() + ":remote")) {
            startService(this);
        }
        return false;
    }

}
