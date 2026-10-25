package ar.com.betometro.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.app.NotificationManager;
import android.view.Window;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.InputDevice;
import android.view.KeyEvent;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Código nativo de Betómetro:
 *  - Voz sin demora: los números se sintetizan una vez a archivos y se reproducen con SoundPool.
 *  - Pulsador: captura de teclas (incluidos los botones de volumen) y aviso de desconexión.
 *  - Actualizador: descarga el APK de la última versión y abre el instalador de Android.
 *  - Sonido fuerte: canal de alarma o multimedia, volumen al máximo durante la carrera, pitidos generados acá.
 *  - Sensor de luz para el modo sol automático.
 */
@CapacitorPlugin(name = "Beto")
public class BetoPlugin extends Plugin implements InputManager.InputDeviceListener {

    static BetoPlugin instancia;

    // ---------- Voz ----------
    private TextToSpeech tts;
    private boolean ttsListo = false;
    private String idioma = "";
    private SoundPool pool;
    private final Map<String, Integer> sonidos = new HashMap<>();   // texto -> id en SoundPool (cargado)
    private final Map<Integer, String> cargando = new HashMap<>();  // id SoundPool -> texto
    private final Map<String, File> archivos = new HashMap<>();     // utteranceId -> archivo
    private final Map<String, String> rutas = new HashMap<>();      // texto -> archivo listo (para recargar al cambiar de canal)
    private int total = 0;
    private float velocidadVoz = 1.15f;
    private String canal = "alarma";
    private int volPrevio = -1, streamPrevio = AudioManager.STREAM_ALARM;

    // ---------- Sensor de luz ----------
    private SensorManager sensores;
    private long ultimaLuz = 0;
    private final SensorEventListener oyenteLuz = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent e) {
            long t = SystemClock.uptimeMillis();
            if (t - ultimaLuz < 1000) return;
            ultimaLuz = t;
            notifyListeners("luz", new JSObject().put("lux", e.values[0]));
        }
        @Override public void onAccuracyChanged(Sensor s, int a) { }
    };

    // ---------- Teclas ----------
    private final Set<Integer> teclas = new HashSet<>();
    private boolean aprender = false;
    private final Map<Integer, String> dispositivos = new HashMap<>();

    @Override
    public void load() {
        instancia = this;
        teclas.add(KeyEvent.KEYCODE_ENTER);
        teclas.add(KeyEvent.KEYCODE_NUMPAD_ENTER);
        teclas.add(KeyEvent.KEYCODE_SPACE);
        teclas.add(KeyEvent.KEYCODE_F9);

        crearPool();
        try {   // pitidos agudos con armónicos: se distinguen mejor del ruido grave del motor
            rutas.put("#corto", generarPitido("pitido_corto", 1400, 110).getAbsolutePath());
            rutas.put("#largo", generarPitido("pitido_largo", 1900, 480).getAbsolutePath());
            rutas.put("#tic", generarPitido("pitido_tic", 1000, 70).getAbsolutePath());
            cargar("#corto"); cargar("#largo"); cargar("#tic");
        } catch (IOException ignored) { }

        InputManager im = (InputManager) getContext().getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            for (int id : im.getInputDeviceIds()) registrarDispositivo(id);
            im.registerInputDeviceListener(this, new Handler(Looper.getMainLooper()));
        }
    }

    // ======================= UBICACIÓN PRECISA =======================
    @PluginMethod
    public void ubicacionPrecisa(PluginCall call) {
        boolean fina = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean aprox = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        call.resolve(new JSObject().put("fina", fina).put("aproximada", aprox));
    }

    /** Pide la ubicación precisa; con ajustes=true abre la pantalla de permisos de la app (si Android ya no muestra el pedido). */
    @PluginMethod
    public void pedirUbicacionPrecisa(PluginCall call) {
        if (call.getBoolean("ajustes", false)) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, 4521);
        }
        call.resolve();
    }

    /** Android 13 o más: permiso para mostrar notificaciones (aviso de versión nueva). */
    @PluginMethod
    public void pedirNotificaciones(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.POST_NOTIFICATIONS }, 4522);
        call.resolve();
    }

    // ======================= MODO CARRERA =======================
    private int filtroPrevio = -1;
    private NotificationManager.Policy politicaPrevia = null;

    private NotificationManager notificaciones() { return (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE); }

    @PluginMethod
    public void estadoNoMolestar(PluginCall call) {
        NotificationManager nm = notificaciones();
        call.resolve(new JSObject().put("permitido", nm != null && nm.isNotificationPolicyAccessGranted()));
    }

    @PluginMethod
    public void pedirNoMolestar(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    /** Al largar: pantalla completa y No molestar (solo suenan las alarmas, que es el canal de Betómetro). Al terminar: todo como estaba. */
    @PluginMethod
    public void modoCarrera(PluginCall call) {
        final boolean activo = call.getBoolean("activo", false);
        getActivity().runOnUiThread(() -> {
            Window w = getActivity().getWindow();
            WindowInsetsControllerCompat c = WindowCompat.getInsetsController(w, w.getDecorView());
            if (activo) {
                c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                c.hide(WindowInsetsCompat.Type.systemBars());
            } else {
                c.show(WindowInsetsCompat.Type.systemBars());
            }
        });
        boolean ok = activo ? activarNoMolestar() : restaurarNoMolestar();
        call.resolve(new JSObject().put("noMolestar", ok));
    }

    private boolean activarNoMolestar() {
        NotificationManager nm = notificaciones();
        if (nm == null || !nm.isNotificationPolicyAccessGranted()) return false;
        try {
            if (filtroPrevio < 0) { filtroPrevio = nm.getCurrentInterruptionFilter(); politicaPrevia = nm.getNotificationPolicy(); }
            int efectos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                efectos = NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT | NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS
                        | NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK | NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR
                        | NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE | NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT
                        | NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;
            } else {
                efectos = NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON | NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
            }
            nm.setNotificationPolicy(new NotificationManager.Policy(0, 0, 0, efectos));   // nadie interrumpe: ni llamadas ni mensajes
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean restaurarNoMolestar() {
        NotificationManager nm = notificaciones();
        if (nm == null || filtroPrevio < 0 || !nm.isNotificationPolicyAccessGranted()) return false;
        try {
            if (politicaPrevia != null) nm.setNotificationPolicy(politicaPrevia);
            nm.setInterruptionFilter(filtroPrevio);
        } catch (Exception ignored) { }
        filtroPrevio = -1; politicaPrevia = null;
        return true;
    }

    @Override
    protected void handleOnDestroy() {
        restaurarNoMolestar();
        restaurarVolumen();
        if (sensores != null) sensores.unregisterListener(oyenteLuz);
        if (tts != null) tts.shutdown();
        if (pool != null) pool.release();
        instancia = null;
    }

    // ======================= SONIDO =======================

    private AudioAttributes atributos() {
        boolean alarma = "alarma".equals(canal);
        return new AudioAttributes.Builder()
                .setUsage(alarma ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    private void crearPool() {
        pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(atributos()).build();
        pool.setOnLoadCompleteListener((sp, id, status) -> {
            String texto = cargando.remove(id);
            if (texto != null && status == 0) sonidos.put(texto, id);
            emitirEstadoVoz();
        });
    }

    private void cargar(String texto) {
        String ruta = rutas.get(texto);
        if (ruta == null) return;
        int sid = pool.load(ruta, 1);
        cargando.put(sid, texto);
    }

    private File dirVoz() {
        File dir = new File(getContext().getCacheDir(), "voz");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Genera un WAV de 16 bits con la nota y dos armónicos, levemente saturado para que corte más. */
    private File generarPitido(String nombre, double freq, int ms) throws IOException {
        int sr = 44100, n = sr * ms / 1000, ataque = sr / 200;
        File f = new File(dirVoz(), nombre + "_" + (int) freq + "_" + ms + ".wav");
        if (f.exists()) return f;
        ByteBuffer b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()); b.putInt(36 + n * 2); b.put("WAVE".getBytes());
        b.put("fmt ".getBytes()); b.putInt(16); b.putShort((short) 1); b.putShort((short) 1);
        b.putInt(sr); b.putInt(sr * 2); b.putShort((short) 2); b.putShort((short) 16);
        b.put("data".getBytes()); b.putInt(n * 2);
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double s = (Math.sin(2 * Math.PI * freq * t) + 0.5 * Math.sin(4 * Math.PI * freq * t) + 0.3 * Math.sin(6 * Math.PI * freq * t)) / 1.8;
            double env = Math.min(1.0, Math.min(i / (double) ataque, (n - i) / (double) ataque));
            b.putShort((short) (Math.max(-1.0, Math.min(1.0, s * 1.3)) * env * 32000));
        }
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(b.array()); }
        return f;
    }

    @PluginMethod
    public void configurarAudio(PluginCall call) {
        canal = call.getString("canal", "alarma");
        SoundPool viejo = pool;
        sonidos.clear(); cargando.clear();
        crearPool();
        for (String t : rutas.keySet()) cargar(t);
        if (viejo != null) viejo.release();
        if (tts != null) tts.setAudioAttributes(atributos());
        call.resolve();
    }

    @PluginMethod
    public void volumenCarrera(PluginCall call) {
        boolean activo = call.getBoolean("activo", false);
        if (call.getString("canal") != null) canal = call.getString("canal");
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (am == null) { call.resolve(); return; }
        try {
            if (activo) {
                int st = "alarma".equals(canal) ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC;
                if (volPrevio < 0) { volPrevio = am.getStreamVolume(st); streamPrevio = st; }
                am.setStreamVolume(st, am.getStreamMaxVolume(st), 0);
            } else {
                restaurarVolumen();
            }
            call.resolve();
        } catch (SecurityException e) {
            call.reject("Android no dejó cambiar el volumen (¿modo No molestar?)");
        }
    }

    private void restaurarVolumen() {
        if (volPrevio < 0) return;
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        try { if (am != null) am.setStreamVolume(streamPrevio, volPrevio, 0); } catch (SecurityException ignored) { }
        volPrevio = -1;
    }

    // ======================= SENSOR DE LUZ =======================

    @PluginMethod
    public void sensorLuz(PluginCall call) {
        boolean activo = call.getBoolean("activo", false);
        if (sensores == null) sensores = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensores == null) { call.resolve(new JSObject().put("disponible", false)); return; }
        Sensor luz = sensores.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensores.unregisterListener(oyenteLuz);
        if (activo && luz != null) sensores.registerListener(oyenteLuz, luz, SensorManager.SENSOR_DELAY_NORMAL);
        call.resolve(new JSObject().put("disponible", luz != null));
    }

    // ======================= VOZ =======================

    @PluginMethod
    public void prepararVoz(PluginCall call) {
        JSArray lista = call.getArray("textos");
        velocidadVoz = call.getFloat("velocidad", 1.15f);
        String c = call.getString("canal");
        if (c != null && !c.equals(canal)) { canal = c; SoundPool viejo = pool; sonidos.clear(); cargando.clear(); crearPool(); for (String t : rutas.keySet()) cargar(t); if (viejo != null) viejo.release(); }
        final String[] textos;
        try {
            textos = new String[lista.length()];
            for (int i = 0; i < lista.length(); i++) textos[i] = lista.getString(i);
        } catch (Exception e) {
            call.reject("Lista de textos inválida");
            return;
        }
        if (tts != null) {
            sintetizar(textos);
            call.resolve(estadoVozObj());
            return;
        }
        tts = new TextToSpeech(getContext(), status -> {
            if (status != TextToSpeech.SUCCESS) { ttsListo = false; emitirEstadoVoz(); return; }
            Locale[] candidatos = { new Locale("es", "AR"), new Locale("es", "US"), new Locale("es", "MX"), new Locale("es", "ES"), new Locale("es") };
            for (Locale l : candidatos) {
                int r = tts.setLanguage(l);
                if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) { idioma = l.toString(); break; }
            }
            tts.setSpeechRate(velocidadVoz);
            tts.setAudioAttributes(atributos());
            ttsListo = true;
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onError(String id) { archivos.remove(id); }
                @Override public void onDone(String id) {
                    File f = archivos.remove(id);
                    if (f == null || !id.startsWith("arch:")) return;
                    String texto = id.substring(5);
                    rutas.put(texto, f.getAbsolutePath());
                    cargar(texto);
                }
            });
            sintetizar(textos);
        });
        call.resolve(estadoVozObj());
    }

    private void sintetizar(String[] textos) {
        if (!ttsListo) return;
        File dir = dirVoz();
        total = textos.length;
        for (String t : textos) {
            if (sonidos.containsKey(t)) continue;
            File f = new File(dir, "v_" + Integer.toHexString(t.hashCode()) + "_" + (int) (velocidadVoz * 100) + ".wav");
            String uid = "arch:" + t;
            archivos.put(uid, f);
            Bundle p = new Bundle();
            tts.synthesizeToFile(t, p, f, uid);
        }
    }

    @PluginMethod
    public void reproducir(PluginCall call) {
        String texto = call.getString("texto", "");
        Integer id = sonidos.get(texto);
        if (id != null) {
            pool.play(id, 1f, 1f, 1, 0, 1f);
            call.resolve(new JSObject().put("modo", "audio"));
        } else if (ttsListo && !texto.startsWith("#")) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "vivo");
            call.resolve(new JSObject().put("modo", "voz"));
        } else {
            call.resolve(new JSObject().put("modo", "nada"));
        }
    }

    @PluginMethod
    public void hablar(PluginCall call) {
        String texto = call.getString("texto", "");
        if (ttsListo) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "vivo");
        call.resolve();
    }

    @PluginMethod
    public void estadoVoz(PluginCall call) { call.resolve(estadoVozObj()); }

    private JSObject estadoVozObj() {
        JSObject o = new JSObject();
        o.put("motor", ttsListo);
        o.put("idioma", idioma);
        int listos = 0;
        for (String k : sonidos.keySet()) if (!k.startsWith("#")) listos++;
        o.put("listos", listos);
        o.put("total", total);
        return o;
    }

    private void emitirEstadoVoz() { notifyListeners("voz", estadoVozObj()); }

    // ======================= TECLAS =======================

    @PluginMethod
    public void configurarTeclas(PluginCall call) {
        JSArray lista = call.getArray("teclas", null);
        if (lista != null) {
            teclas.clear();
            try { for (int i = 0; i < lista.length(); i++) teclas.add(lista.getInt(i)); } catch (Exception ignored) { }
        }
        aprender = call.getBoolean("aprender", false);
        call.resolve();
    }

    /** Llamado desde MainActivity.dispatchKeyEvent. Devuelve true si la tecla se consume. */
    boolean manejarTecla(KeyEvent e) {
        int code = e.getKeyCode();
        boolean esNuestra = aprender || teclas.contains(code);
        if (!esNuestra) return false;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            JSObject o = new JSObject();
            o.put("codigo", code);
            o.put("nombre", KeyEvent.keyCodeToString(code).replace("KEYCODE_", ""));
            InputDevice d = e.getDevice();
            o.put("dispositivo", d != null ? d.getName() : "");
            o.put("edadMs", SystemClock.uptimeMillis() - e.getEventTime());   // cuánto hace que se apretó
            if (aprender) {
                aprender = false;
                notifyListeners("aprendida", o);
            } else {
                notifyListeners("marca", o);
            }
        }
        return true;
    }

    // ======================= DISPOSITIVOS =======================

    private void registrarDispositivo(int id) {
        InputDevice d = InputDevice.getDevice(id);
        if (d != null && !d.isVirtual()) dispositivos.put(id, d.getName());
    }

    @Override public void onInputDeviceAdded(int id) {
        registrarDispositivo(id);
        JSObject o = new JSObject();
        o.put("evento", "conectado");
        o.put("nombre", dispositivos.containsKey(id) ? dispositivos.get(id) : "");
        notifyListeners("dispositivo", o);
    }

    @Override public void onInputDeviceRemoved(int id) {
        JSObject o = new JSObject();
        o.put("evento", "desconectado");
        o.put("nombre", dispositivos.containsKey(id) ? dispositivos.remove(id) : "");
        notifyListeners("dispositivo", o);
    }

    @Override public void onInputDeviceChanged(int id) { registrarDispositivo(id); }

    @PluginMethod
    public void listarDispositivos(PluginCall call) {
        JSArray a = new JSArray();
        for (String n : dispositivos.values()) a.put(n);
        call.resolve(new JSObject().put("dispositivos", a));
    }

    // ======================= ACTUALIZADOR =======================

    @PluginMethod
    public void puedeInstalar(PluginCall call) {
        boolean ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getContext().getPackageManager().canRequestPackageInstalls();
        call.resolve(new JSObject().put("permitido", ok));
    }

    @PluginMethod
    public void pedirPermisoInstalar(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        }
        call.resolve();
    }

    @PluginMethod
    public void descargarEInstalar(PluginCall call) {
        final String url = call.getString("url");
        if (url == null) { call.reject("Falta la dirección de descarga"); return; }
        new Thread(() -> {
            try {
                File dir = new File(getContext().getExternalFilesDir(null), "actualizaciones");
                if (!dir.exists()) dir.mkdirs();
                File apk = new File(dir, "betometro.apk");
                if (apk.exists()) apk.delete();

                String actual = url;
                HttpURLConnection con = null;
                for (int saltos = 0; saltos < 6; saltos++) {   // GitHub redirige a otro dominio: se siguen a mano
                    con = (HttpURLConnection) new URL(actual).openConnection();
                    con.setInstanceFollowRedirects(false);
                    con.setConnectTimeout(15000);
                    con.setReadTimeout(30000);
                    int code = con.getResponseCode();
                    if (code >= 300 && code < 400) { actual = con.getHeaderField("Location"); con.disconnect(); continue; }
                    if (code != 200) throw new Exception("El servidor respondió " + code);
                    break;
                }
                long largo = con.getContentLengthLong();
                try (InputStream in = con.getInputStream(); FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buf = new byte[64 * 1024];
                    long leido = 0; int n; int ultimoPct = -1;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        leido += n;
                        if (largo > 0) {
                            int pct = (int) (leido * 100 / largo);
                            if (pct != ultimoPct) { ultimoPct = pct; notifyListeners("descarga", new JSObject().put("porcentaje", pct)); }
                        }
                    }
                }
                con.disconnect();

                Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", apk);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(uri, "application/vnd.android.package-archive");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
                call.resolve();
            } catch (Exception e) {
                call.reject("No se pudo descargar la actualización: " + e.getMessage());
            }
        }).start();
    }
}
