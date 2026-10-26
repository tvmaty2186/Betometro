package ar.com.betometro.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/** Al prender el teléfono: si el ajuste está activo, abre Betómetro (equipo dedicado en la caja). */
public class InicioReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(a) && !"android.intent.action.QUICKBOOT_POWERON".equals(a)) return;
        if (!c.getSharedPreferences("betometro", Context.MODE_PRIVATE).getBoolean("iniciarAlPrender", false)) return;
        Intent abrir = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 1) directo (funciona si la app tiene el permiso «mostrar encima de otras apps»)
        try { c.startActivity(abrir); } catch (Exception ignored) { }
        // 2) respaldo: aviso de pantalla completa, como un despertador
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("inicio", "Inicio automático", NotificationManager.IMPORTANCE_HIGH));
            PendingIntent pi = PendingIntent.getActivity(c, 9, abrir, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder n = new NotificationCompat.Builder(c, "inicio")
                    .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                    .setContentTitle("Betómetro").setContentText("Abriendo Betómetro…")
                    .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pi, true).setContentIntent(pi).setAutoCancel(true).setSilent(true).setTimeoutAfter(60000);
            nm.notify(4540, n.build());
        } catch (Exception ignored) { }
    }
}
