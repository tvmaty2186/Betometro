package ar.com.betometro.app;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BetoPlugin.class);
        super.onCreate(savedInstanceState);
        // Que el tamaño de letra del sistema no agrande la app: el diseño ya está pensado para leerse en el auto
        if (getBridge() != null && getBridge().getWebView() != null) getBridge().getWebView().getSettings().setTextZoom(100);
        // Una vez por día (con internet) se revisa si hay una versión nueva, aunque la app esté cerrada
        try {
            PeriodicWorkRequest revisar = new PeriodicWorkRequest.Builder(ActualizacionWorker.class, 24, TimeUnit.HOURS)
                    .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("buscar-actualizacion", ExistingPeriodicWorkPolicy.KEEP, revisar);
        } catch (Exception ignored) { }
    }

    /** Las teclas del pulsador (incluidos los botones de volumen) se atienden antes que el resto de la app. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        BetoPlugin p = BetoPlugin.instancia;
        if (p != null && p.manejarTecla(event)) return true;
        return super.dispatchKeyEvent(event);
    }
}
