package ar.com.betometro.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Una vez por día, con la app cerrada: si hay una versión publicada más nueva, avisa con una notificación (una sola vez por versión). */
public class ActualizacionWorker extends Worker {

    static final String CANAL = "actualizaciones";

    public ActualizacionWorker(@NonNull Context c, @NonNull WorkerParameters p) { super(c, p); }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://api.github.com/repos/tvmaty2186/betometro/releases/latest").openConnection();
            con.setRequestProperty("Accept", "application/vnd.github+json");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            if (con.getResponseCode() != 200) return Result.retry();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = con.getInputStream()) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n); }
            con.disconnect();
            String nueva = new JSONObject(out.toString(StandardCharsets.UTF_8.name())).optString("tag_name", "").replaceFirst("^v", "");
            String actual = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            if (nueva.isEmpty() || actual == null || comparar(nueva, actual) <= 0) return Result.success();
            SharedPreferences sp = c.getSharedPreferences("betometro", Context.MODE_PRIVATE);
            if (nueva.equals(sp.getString("avisada", ""))) return Result.success();
            if (notificar(c, nueva)) sp.edit().putString("avisada", nueva).apply();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    static int comparar(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int p = i < x.length ? entero(x[i]) : 0, q = i < y.length ? entero(y[i]) : 0;
            if (p != q) return p - q;
        }
        return 0;
    }

    private static int entero(String s) { try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; } }

    private static boolean notificar(Context c, String version) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(new NotificationChannel(CANAL, "Actualizaciones de Betómetro", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent abrir = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, abrir, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder n = new NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Hay una versión nueva de Betómetro")
                .setContentText("Versión " + version + ": tocá para abrir la app e instalarla.")
                .setContentIntent(pi)
                .setAutoCancel(true);
        try { NotificationManagerCompat.from(c).notify(4521, n.build()); return true; }
        catch (SecurityException e) { return false; }
    }
}
