package cn.vacuumflask.trycatchbyfile;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2016/9/2 0002.
 */
public class CrashHandlerTwo implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler mDefaultHandler;// 系统默认的UncaughtException处理类
    private static CrashHandlerTwo INSTANCE;// CrashHandlerTwo实例
    private Context mContext;// 程序的Context对象

    private CrashHandlerTwo() {
    }

    public static CrashHandlerTwo getInstance() {
        if (INSTANCE == null) INSTANCE = new CrashHandlerTwo();
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();// 获取系统默认的UncaughtException处理器
        Thread.setDefaultUncaughtExceptionHandler(this);// 设置该CrashHandler为程序的默认处理器
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (!handleException(throwable) && mDefaultHandler != null) {
            // 如果自定义的没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, throwable);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex * 异常信息
     * @return true 如果处理了该异常信息;否则返回false.
     */
    public boolean handleException(Throwable ex) {
        if (ex == null || mContext == null) return false;
        final String crashReport = getCrashReport(mContext, ex);
        Log.i("error", crashReport);
        new Thread() {
            public void run() {
                Looper.prepare();
                File file = save2File(crashReport);
                sendAppCrashReport(mContext, crashReport, file);
                Looper.loop();
            }
        }.start();
        return true;
    }

    private File save2File(String crashReport) {
        String fileName = "crash-" + System.currentTimeMillis() + ".txt";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "crash");
                if (!dir.exists()) dir.mkdir();
                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(crashReport.toString().getBytes());
                fos.close();
                return file;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void sendAppCrashReport(final Context context, final String crashReport, final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.myDialog));

        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle("程序出错");
        builder.setMessage("请把错误报告提交给我们，谢谢！");
        builder.setPositiveButton("提交", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    //下面部分是已文字内容形式发送错误信息
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT,
                            "云物流PDA - 错误报告");
                    intent.putExtra(Intent.EXTRA_TEXT, crashReport);
                    intent.setData(Uri
                            .parse("mailto:way.ping.li@gmail.com"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);

                    //下面是以附件形式发送邮件
//                    Intent intent = new Intent(Intent.ACTION_SEND);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    String[] tos = {"mailto:way.ping.li@gmail.com"};
//                    intent.putExtra(Intent.EXTRA_EMAIL, tos);
//
//                    intent.putExtra(Intent.EXTRA_SUBJECT,
//                            "云物流PDA - 错误报告");
//                    if (file != null) {
//                        intent.putExtra(Intent.EXTRA_STREAM,
//                                Uri.fromFile(file));
//                        intent.putExtra(Intent.EXTRA_TEXT,
//                                "请将此错误报告发送给我，以便我尽快修复此问题，谢谢合作！\n");
//                    } else {
//                        intent.putExtra(Intent.EXTRA_TEXT,
//                                "请将此错误报告发送给我，以便我尽快修复此问题，谢谢合作！\n"
//                                        + crashReport);
//                    }
//                    intent.setType("text/plain");
//                    intent.setType("message/rfc882");
//                    Intent.createChooser(intent, "Choose Email Client");
//                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context,
                            "发送失败",
                            Toast.LENGTH_SHORT).show();
                } finally {
//                    mDialog.dismiss();
                    // 退出
                    android.os.Process.killProcess(android.os.Process
                            .myPid());
                    System.exit(1);
                }

            }
        });
        builder.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // 退出
                        android.os.Process.killProcess(android.os.Process
                                .myPid());
                        System.exit(1);
                    }
                });
        AlertDialog mDialog = builder.create();
        mDialog.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    /**
     * 获取APP崩溃异常报告
     *
     * @param ex
     * @return
     */
    private String getCrashReport(Context context, Throwable ex) {
        PackageInfo pinfo = getPackageInfo(context);
        StringBuffer exceptionStr = new StringBuffer();
        exceptionStr.append("Version: " + pinfo.versionName + "("
                + pinfo.versionCode + ")\n");
        exceptionStr.append("Android: " + android.os.Build.VERSION.RELEASE
                + "(" + android.os.Build.MODEL + ")\n");
        exceptionStr.append("Exception: " + ex.getMessage() + "\n");
        StackTraceElement[] elements = ex.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            exceptionStr.append(elements[i].toString() + "\n");
        }
        return exceptionStr.toString();
    }

    /**
     * 获取App安装包信息
     *
     * @return
     */
    private PackageInfo getPackageInfo(Context context) {
        PackageInfo info = null;
        try {
            info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            // e.printStackTrace(System.err);
            // L.i("getPackageInfo err = " + e.getMessage());
        }
        if (info == null)
            info = new PackageInfo();
        return info;
    }

}
