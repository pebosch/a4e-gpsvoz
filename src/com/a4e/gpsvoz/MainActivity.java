/**
 * Título: Android4Education Gpsvoz
 * Licencia Pública General de GNU (GPL) versión 3 
 * Código Fuente: https://github.com/pebosch/a4e-gpsvoz
 * Autor: Pedro Fernández Bosch
 * Fecha de la última modificación: 08/02/2015
 */

package com.a4e.gpsvoz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.RecognizerIntent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.a4e.gpsvoz.R;
import java.lang.Math;
import java.util.ArrayList;

/**
 * Declaración de variables
 */
public class MainActivity extends Activity implements SensorEventListener {
	
	TextView txtAngle;
	private ImageView imgCompass;
	private float currentDegree = 0f; // Angulo (grado) actual del compass
	private SensorManager mSensorManager; // El sensor manager del dispositivo
	
	// Los dos sensores que son necesarios (TYPE_ORINETATION está deprecated)
	Sensor accelerometer; 
	Sensor magnetometer;
	
	float degree;
	float azimut;
	float[] mGravity;
	float[] mGeomagnetic;
	
	//Posicion de origen (GPS)
	int sem_ob = 0;
	float latitud_o = 0; 
	float longitud_o = 0;
	
	//Posicion de destino (voz)
	float latitud_d = 0; 
	float longitud_d = 0;
	
	int cuadrante = 0; //Cuadrante del punto destino (latitud_d, longitud_d)
	
	// Captura de coordenadas por voz
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1; 
	private Button bt_start1;
	private Button bt_start2;
	int opcion = 0; // Opcion=1 Latitud ; Opcion=2 Longitud  
	
	/**
	 * OnCreate Method Override 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Variables que almacenan los elementos del layout
		imgCompass = (ImageView) findViewById(R.id.imgViewCompass); 
		txtAngle = (TextView) findViewById(R.id.txtAngle);
		
		// Inicialización de los sensores del dispositivo android para la brujula
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
	    accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    
	    mGravity = null;
	    mGeomagnetic = null;
	    
	    // Inicialización de los sensores del dispositivo android para el GPS
    	LocationManager milocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    	LocationListener milocListener = new MiLocationListener();
    	milocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, milocListener);
    	
    	// Inicialización de los sensores del dispositivo android para el reconocimiento de voz
	    bt_start1 = (Button)findViewById(R.id.button1); 
	    bt_start1.setOnClickListener(new View.OnClickListener() {
	    	public void onClick(View v) {
	    		opcion = 1;
	    		startVoiceRecognitionActivity(); // Lanzamiento del reconoimiento de voz
	    	}
	    });
	    bt_start2 = (Button)findViewById(R.id.button2); 
	    bt_start2.setOnClickListener(new View.OnClickListener() {
	    	public void onClick(View v) {
	    		opcion = 2;
	    		startVoiceRecognitionActivity(); // Lanzamiento del reconoimiento de voz
	    	}
	    });
	}

	/**
	 * Registro de un listener para los sensores del accelerometer y el magnetometer
	 */	
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
	    mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * Detención del listener para no malgastar la bateria
	 */	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	/**
	 * Calculo de posicionamiento del azimut
	 */	
	@Override
	public void onSensorChanged(SensorEvent event) {		
		// Se comprueba que tipo de sensor está activo en cada momento
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				mGravity = event.values.clone();
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				mGeomagnetic = event.values.clone();
				break;
		}
				
		// Calcular el cuadrante del punto de destino
		if(latitud_o <= latitud_d && longitud_o < longitud_d){
			cuadrante = 3;
		}
		else if(latitud_o <= latitud_d && longitud_o > longitud_d){
			cuadrante = 1;
		}
		else if(latitud_o > latitud_d && longitud_o <= longitud_d){
			cuadrante = 4;
		}
		else if(latitud_o > latitud_d && longitud_o >= longitud_d){
			cuadrante = 2;
		}
		
		// Calculo de vectores
		float x = (longitud_d - longitud_o);
		float y = (latitud_d - latitud_o);
		
		if(x < 0) x = x*(-1);
		if(y < 0) y = y*(-1);
		
		// Calculo del ángulo (mediante la arco tangente)
		double anguloRadianes = Math.atan(x/y);
		double angulo = Math.toDegrees(anguloRadianes);
		float rb_degree = (float) angulo;
		
		// Posicionamiento del angulo resultante en el cuadrante adecuado
		if(cuadrante == 3)
			rb_degree = 0 + rb_degree;
		else if(cuadrante == 1)
			rb_degree = 0 - rb_degree;
		else if(cuadrante == 2)
			rb_degree = -180+rb_degree;
		else if(cuadrante == 4)
			rb_degree =  180-rb_degree;
		
		// Direccionamiento del azimut
		if ((mGravity != null) && (mGeomagnetic != null)) {
		  	float RotationMatrix[] = new float[16];
		   	boolean success = SensorManager.getRotationMatrix(RotationMatrix, null, mGravity, mGeomagnetic);
		   	if (success) {
		   		float orientation[] = new float[3];
		   		SensorManager.getOrientation(RotationMatrix, orientation);
		   		azimut = orientation[0] * (180 / (float) Math.PI);		   				   		
		   	}
        }
		degree = azimut;
		
		// Mostrar por pantalla el ángulo entre 0º y 360º
		double degree_aux = degree;
		if (degree_aux < 0)
			degree_aux = degree_aux * (-1);
		else if (degree_aux > 0)
			degree_aux = ((degree_aux - 180) * (-1)) + 180;
			
		if (longitud_o == 0 && latitud_o == 0)
			txtAngle.setText("Obteniendo coordenadas GPS");
		else
		txtAngle.setText("�?ngulo: " + Double.toString(Math.round(degree_aux*100.0)/100.0) + "º");
		
		// Animación de la rottación (se revierte el giro en grados, negativo)
		RotateAnimation ra = new RotateAnimation(
				currentDegree+rb_degree, 
				degree+rb_degree,
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF,
				0.5f);
		// Tiempo durante el cual la animación se llevará a cabo
		ra.setDuration(1000);
		
		// Establecer la animación después del final de la estado de reserva
		ra.setFillAfter(true);
		
		// Inicio de la animacion
		imgCompass.startAnimation(ra);
		
		currentDegree = -degree;
	}

	/**
	 * Calculo de cambios en la precision
	 */	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// No se usa
	}
	
	/**
	 * Obtención de coordenadas de origen a traves de escucha del GPS
	 */	
	public class MiLocationListener implements LocationListener {
		public void onLocationChanged(Location loc) {
			
			// Obtencion de la posicion de origen con el GPS(mi posicion)
			latitud_o = (float) loc.getLatitude();
			longitud_o = (float) loc.getLongitude();
			
			String coordenadas = "(GPS) Latitud = " + latitud_o + " " + "Longitud = " + longitud_o;
			/*Toast.makeText( getApplicationContext(),coordenadas,Toast.LENGTH_LONG).show();*/
			TextView resultadosGPS = (TextView)findViewById(R.id.resultadosGPS);
			resultadosGPS.setText(String.valueOf(coordenadas));
		}
		public void onProviderDisabled(String provider){
			Toast.makeText( getApplicationContext(),"Gps Desactivado",Toast.LENGTH_SHORT ).show();
		}
		public void onProviderEnabled(String provider){
			Toast.makeText( getApplicationContext(),"Gps Activo",Toast.LENGTH_SHORT ).show();
		}
		public void onStatusChanged(String provider, int status, Bundle extras){}
	}
	
	/**
	 * Inicio de la recogida de voz
	 */	
	private void startVoiceRecognitionActivity() {
		  // Definición del intent para realizar en analisis del mensaje
		  Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		  
		  // Indicación del modelo de lenguaje para el intent
		  intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
		  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		  
		  // Definición del mensaje indicativo¡ 
		  intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Diga sus coordenadas...");
		  
		  // Lanzamiento de la actividad esperando resultados
		  startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}
	
	/**
	 * Tratamiento del reconocimiento de voz
	 */	
	@Override
	 // Recepción de los resultados del reconocimiento de voz
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		 // Si el reconocimiento a sido positivo
		 if(requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK){
			 // El intent envia un ArrayList
			 ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			 
			 // Separación del texto en palabras.
			 String [ ] palabras = matches.get(0).toString().split(" ");

			 if(opcion == 1){
				 // Latitud
				 TextView latitud = (TextView)findViewById(R.id.txtLatitud);
				 String latitud_d_aux = ""; // Inicializacion del valor de las coordenadas cada vez que se pulsa el boton
				 for(int i=0;i<palabras.length;i++){
					 if(palabras[i].equals("menos")) // Interpretacion de la palabra menos como simbolo
						 latitud_d_aux = latitud_d_aux + "-";
					 else if(palabras[i].equals("punto")) // Interpretacion de la palabra punto como simbolo
						 latitud_d_aux = latitud_d_aux + ".";
					 else
						 latitud_d_aux = latitud_d_aux + palabras[i];
				 }
				 
				 // Comprobación del casting a numero real
				 String s = new String (latitud_d_aux);
				 try{
					 latitud.setText(String.valueOf(Float.parseFloat(s)));
					 latitud_d = Float.parseFloat(s);
				 }
				 catch(Exception e){
					 latitud.setText(String.valueOf("ERROR"));
				 }

			 }else{
				 //Longitud
				 TextView latitud = (TextView)findViewById(R.id.txtLongitud);
				 String longitud_d_aux = "";
				 for(int i=0;i<palabras.length;i++){
					 if(palabras[i].equals("menos"))
						 longitud_d_aux = longitud_d_aux + "-";
					 else if(palabras[i].equals("punto"))
						 longitud_d_aux = longitud_d_aux + ".";
					 else
						 longitud_d_aux = longitud_d_aux + palabras[i];
				 }
				
				 // Comprobación del casting a numero real
				 String s = new String (longitud_d_aux);
				 try{
					 latitud.setText(String.valueOf(Float.parseFloat(s)));
					 longitud_d = Float.parseFloat(s);
				 }
				 catch(Exception e){
					 latitud.setText(String.valueOf("ERROR"));
				 }
			 }
		 }
	 }

	/**
	 * Definición del menu de opciones de la aplicación
	 */	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Funcionalidad de los ítems del menu
	 */	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.optionLicencia:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.gnu.org/licenses/gpl.html")));
			break;
		case R.id.optionCodigo:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/pebosch/a4e-gpsvoz")));
			break;
		case R.id.optionAyuda:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.a4e.gpsvoz")));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}