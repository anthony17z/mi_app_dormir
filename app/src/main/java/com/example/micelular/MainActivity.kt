package com.example.micelular

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "celular_channel"

    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        crearCanal()

        // Pide permiso automáticamente en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val tvAlarmSet = findViewById<TextView>(R.id.tvAlarmSet)
        val tvSleepSet = findViewById<TextView>(R.id.tvSleepSet)

        findViewById<Button>(R.id.btnSetAlarm).setOnClickListener {
            val hora = timePicker.hour
            val minuto = timePicker.minute
            programarAlarma(hora, minuto, 1, "Hora límite", "Es momento de dejar el celular.")
            val ampm = if (hora < 12) "AM" else "PM"
            val h12 = if (hora % 12 == 0) 12 else hora % 12
            tvAlarmSet.setText("Aviso programado a las $h12:${"%02d".format(minuto)} $ampm")
        }

        findViewById<Button>(R.id.btnSleepAlarm).setOnClickListener {
            programarAlarma(0, 0, 2, "Hora de dormir", "Apaga el celular y descansa.")
            tvSleepSet.setText("Alarma de sueño activada a las 12:00 AM")
            Toast.makeText(this, "Alarma de dormir activada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun programarAlarma(hora: Int, minuto: Int, reqCode: Int, titulo: String, mensaje: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("titulo", titulo)
            putExtra("mensaje", mensaje)
            putExtra("reqCode", reqCode)
        }
        val pending = PendingIntent.getBroadcast(
            this, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CHANNEL_ID, "Avisos celular", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
        }
    }

    class AlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val titulo  = intent.getStringExtra("titulo") ?: ""
            val mensaje = intent.getStringExtra("mensaje") ?: ""
            val reqCode = intent.getIntExtra("reqCode", 0)
            val notification = NotificationCompat.Builder(context, "celular_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(reqCode, notification)
            }
        }
    }
}