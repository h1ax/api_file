package com.capturer.deptraivcl;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.capturer.deptraivcl.HelloJni;
import java.io.File;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Environment;
import android.os.StrictMode;


public class Login extends AsyncTask<String, Void, String> {
    static {
        System.loadLibrary("hello-jni");
    }

    private native String postData(String url, String body);
    private native String setData(Context context, String key);

    private void downloadAndInstallAPK(final String apkUrl, final ProgressBar progressBar, final TextView percentTextView) {
        final DownloadManager downloadManager = (DownloadManager) HelloJni.getAc().getSystemService(Context.DOWNLOAD_SERVICE);
        final Uri uri = Uri.parse(apkUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        // Lưu file APK vào thư mục Download của thiết bị, nơi đã được cấp quyền truy cập
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        final File apkFile = new File(downloadDir, "capturer.apk");  // Đặt tên file APK
        request.setDestinationUri(Uri.fromFile(apkFile));  // Lưu file tại thư mục Download

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        final long downloadId = downloadManager.enqueue(request);

        // Lắng nghe sự kiện tải xong
        HelloJni.getAc().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Khi file tải xong, tắt ProgressBar và cài đặt ứng dụng
                    progressBar.setVisibility(View.GONE);  // Tắt ProgressBar
                    percentTextView.setVisibility(View.GONE);  // Tắt phần trăm tải

                    Uri fileUri = Uri.fromFile(apkFile);  // Lấy URI của file đã tải
                    if (fileUri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                            StrictMode.setVmPolicy(builder.build());
                        }
                        
                        // Tiến hành cài đặt APK
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (!HelloJni.getAc().getPackageManager().canRequestPackageInstalls()) {
                                Intent intent1 = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                                intent1.setData(Uri.parse("package:" + HelloJni.getAc().getPackageName()));
                                HelloJni.getAc().startActivity(intent1);
                                //installAPK(fileUri);
                            } else {
                                installAPK(fileUri);
                            }
                        } else {
                            installAPK(fileUri);
                        }
                    } else {
                        Toast.makeText(HelloJni.getAc(), "File APK không tồn tại", Toast.LENGTH_SHORT).show();
                    }
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Cập nhật ProgressBar và phần trăm tải
        new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean downloading = true;
                    while (downloading) {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        Cursor cursor = downloadManager.query(query);
                        if (cursor != null && cursor.moveToFirst()) {
                            int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            if (bytesTotal > 0) {
                                final int downloadProgress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                                progressBar.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressBar.setProgress(downloadProgress);
                                            percentTextView.setText(downloadProgress + "%");
                                        }
                                    });
                            }

                            if (bytesDownloaded == bytesTotal) {
                                downloading = false;
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }).start();
    }
    

    private void installAPK(Uri uri) {
        
        Toast.makeText(HelloJni.getAc(), "Start Install", Toast.LENGTH_SHORT).show();
        // Kiểm tra xem file có phải là APK hợp lệ không
        PackageManager packageManager = HelloJni.getAc().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Cấp quyền tạm thời cho ứng dụng cài đặt APK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Kiểm tra nếu có ứng dụng cài đặt APK hay không
        Toast.makeText(HelloJni.getAc(), "Check", Toast.LENGTH_SHORT).show();
        
        HelloJni.getAc().startActivity(intent);
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        if (activities != null && !activities.isEmpty()) {
            // Nếu có ứng dụng cài đặt APK, thực hiện cài đặtCheck
            Toast.makeText(HelloJni.getAc(), "Installing", Toast.LENGTH_SHORT).show();
            
        } else {
            // Nếu không có ứng dụng cài đặt APK, thông báo lỗi
            Toast.makeText(HelloJni.getAc(), "APK không hợp lệ hoặc không thể cài đặt (" + uri.getPath() + ")", Toast.LENGTH_SHORT).show();
        }
    }
    


    @Override
    protected String doInBackground(String... params) {
        String result = "";
        try {
            String url = params[0];
            String body = params[1];

            // Gọi hàm native setData để xử lý dữ liệu
            setData(HelloJni.getAc(), body);

            // Gọi hàm native postData để thực hiện gửi yêu cầu HTTP
            result = postData(url, body);
        } catch (Exception e) {
            e.printStackTrace();
            result = "Failed to post data";
        }
        return result;
    }

    private TextView textView;
    private LinearLayout layout; 

    public Login(TextView textView, LinearLayout layout) {
        this.textView = textView;
        this.layout = layout;
    }

    @Override
    protected void onPostExecute(String result) {
        if (layout == null) {
            Toast.makeText(HelloJni.getAc(), "Layout is null", Toast.LENGTH_SHORT).show();
            return;
        }

        layout.removeAllViews();

        try {
            JSONObject jsonResponse = new JSONObject(result);
            int status = jsonResponse.getInt("status");

            if (status == 1) {
                long timeRemaining = jsonResponse.getLong("time") * 1000;
                final String key = jsonResponse.getString("key");

                new CountDownTimer(timeRemaining, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long days = millisUntilFinished / (1000 * 60 * 60 * 24);
                        long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                        long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                        long seconds = (millisUntilFinished / 1000) % 60;

                        textView.setText(String.format("Time Remaining: %02d days %02d:%02d:%02d\nKey: %s",
                                                       days, hours, minutes, seconds, key));
                    }

                    @Override
                    public void onFinish() {
                        textView.setText("Time's up! Key: " + key);
                    }
                }.start();

                JSONArray appList = jsonResponse.getJSONArray("appList");
                for (int i = 0; i < appList.length(); i++) {
                    JSONObject app = appList.getJSONObject(i);
                    String appName = app.getString("appName");
                    String author = app.getString("author");
                    final String link = app.getString("link");
                    final String packageName = app.getString("pkg");

                    TextView appInfo = new TextView(HelloJni.getAc());
                    appInfo.setText("App: " + appName + "\nAuthor: " + author);
                    appInfo.setTextSize(18);
                    appInfo.setPadding(10, 10, 10, 10);

                    // Nút "Tải"
                    Button btnDownload = new Button(HelloJni.getAc());
                    btnDownload.setText("Tải");
                    btnDownload.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Kiểm tra xem đường link tải có hợp lệ không
                                if (link == null || link.isEmpty()) {
                                    Toast.makeText(HelloJni.getAc(), "Đường link tải không hợp lệ", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Kiểm tra URL có hợp lệ không
                                try {
                                    Uri uri = Uri.parse(link);
                                    if (!uri.isAbsolute()) {
                                        Toast.makeText(HelloJni.getAc(), "Đường link không hợp lệ", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(HelloJni.getAc(), "Đường link không hợp lệ", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Tạo ProgressBar và TextView để hiển thị tiến độ
                                ProgressBar progressBar = new ProgressBar(HelloJni.getAc());
                                TextView percentTextView = new TextView(HelloJni.getAc());

                                // Thêm ProgressBar và TextView vào layout của ứng dụng
                                layout.addView(progressBar);
                                layout.addView(percentTextView);

                                // Tải file APK từ URL và cài đặt
                                downloadAndInstallAPK(link, progressBar, percentTextView);
                            }
                        });

// Nút "Mở Game"
                    Button btnOpenGame = new Button(HelloJni.getAc());
                    btnOpenGame.setText("Mở Game");
                    btnOpenGame.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Kiểm tra xem package name có hợp lệ không
                                if (packageName == null || packageName.isEmpty()) {
                                    Toast.makeText(HelloJni.getAc(), "Package name không hợp lệ", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Kiểm tra nếu ứng dụng đã được cài đặt
                                Intent intent = HelloJni.getAc().getPackageManager().getLaunchIntentForPackage(packageName);

                                if (intent != null) {
                                    // Nếu ứng dụng đã cài đặt, mở ứng dụng
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // Đảm bảo mở trong một Activity mới
                                    HelloJni.getAc().startActivity(intent);
                                } else {
                                    // Nếu ứng dụng chưa cài đặt, mở Play Store
                                    Toast.makeText(HelloJni.getAc(), "Ứng dụng chưa được cài đặt. Mở Play Store.", Toast.LENGTH_SHORT).show();
                                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    HelloJni.getAc().startActivity(playStoreIntent);
                                }
                            }
                        });
                    

                    LinearLayout appLayout = new LinearLayout(HelloJni.getAc());
                    appLayout.setOrientation(LinearLayout.VERTICAL);
                    appLayout.setPadding(20, 20, 20, 20);
                    appLayout.addView(appInfo);
                    appLayout.addView(btnDownload);
                    appLayout.addView(btnOpenGame);

                    layout.addView(appLayout);
                }
            } else {
                String message = jsonResponse.getString("message");
                Toast.makeText(HelloJni.getAc(), message, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(HelloJni.getAc(), "Error parsing response", Toast.LENGTH_SHORT).show();
        }
    }
}

