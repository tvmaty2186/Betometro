package ar.com.betometro.app;

import android.os.Bundle;
import android.view.KeyEvent;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BetoPlugin.class);
        super.onCreate(savedInstanceState);
        // Que el tamaño de letra del sistema no agrande la app: el diseño ya está pensado para leerse en el auto
        if (getBridge() != null && getBridge().getWebView() != null) getBridge().getWebView().getSettings().setTextZoom(100);
    }

    /** Las teclas del pulsador (incluidos los botones de volumen) se atienden antes que el resto de la app. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        BetoPlugin p = BetoPlugin.instancia;
        if (p != null && p.manejarTecla(event)) return true;
        return super.dispatchKeyEvent(event);
    }
}
