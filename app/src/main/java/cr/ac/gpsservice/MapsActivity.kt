package cr.ac.gpsservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPolygon
import cr.ac.gpsservice.databinding.ActivityMapsBinding
import cr.ac.gpsservice.db.LocationDatabase
import cr.ac.gpsservice.entity.Location
import cr.ac.gpsservice.service.GpsService
import org.json.JSONObject

private lateinit var mMap: GoogleMap
private lateinit var locationDatabase: LocationDatabase
private lateinit var layer : GeoJsonLayer

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var binding: ActivityMapsBinding
    private val SOLICITAR_GPS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationDatabase=LocationDatabase.getInstance(this)
        validaPermisos()
    }

    fun definePoligono(googleMap: GoogleMap){
        val geoJsonData= JSONObject("{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {},\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Polygon\",\n" +
                "        \"coordinates\": [\n" +
                "          [\n" +
                "            [\n" +
                "              -87.945556640625,\n" +
                "              5.4519590315863455\n" +
                "            ],\n" +
                "            [\n" +
                "              -82.518310546875,\n" +
                "              5.4519590315863455\n" +
                "            ],\n" +
                "            [\n" +
                "              -82.518310546875,\n" +
                "              11.329253026617318\n" +
                "            ],\n" +
                "            [\n" +
                "              -87.945556640625,\n" +
                "              11.329253026617318\n" +
                "            ],\n" +
                "            [\n" +
                "              -87.945556640625,\n" +
                "              5.4519590315863455\n" +
                "            ]\n" +
                "          ]\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n")

        layer = GeoJsonLayer(googleMap, geoJsonData)
        layer.addLayerToMap()

    }





    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        recuperarPuntos(mMap)
        definePoligono(googleMap)
        iniciaServicio()
    }
    /**
     * Obtener los puntos almacenados en la bd y mostrarlos en el mapa
     */
    fun recuperarPuntos(googleMap:GoogleMap){
        mMap = googleMap
        for(location in locationDatabase.locationDao.query()){
            val costaRica = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(costaRica).title("Marker in Costa Rica"))
        }

    }


    /**
     *hace un filtro del broadcast GPS(cr.ac.gpsservice.GPS_EVENT)
     * E inicia el servicio(starService) GpsService
     */

    fun iniciaServicio(){
        val filter= IntentFilter()
        filter.addAction(GpsService.GPS)
        val rcv=ProgressReceiver()
        registerReceiver(rcv,filter)
        startService(Intent(this,GpsService::class.java))
    }

    /**
     *Valida si la app tiene permisos de ACCESS_FINE_LOCATION y ACCESS_COARSE_LOCATION
     * si no tiene permisos solicita al usuario permisos(requestPermission)
     */
    fun validaPermisos(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            // NO TENGO PERMISOS
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                SOLICITAR_GPS
            )
        }
    }


    /**
     * validar que se le dieron los permisos a la app, en caso contrario salir
     */

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            SOLICITAR_GPS -> {
                if ( grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    System.exit(1)
                }

            }
        }


    }

    /**
     * es la clase para recibir los mensajes de broadcast
     */
    class ProgressReceiver:BroadcastReceiver(){
        /**
         * se obtiene el parametro enviado por el servicio (Location)
         * Coloca en el mapa la localizacion
         * Mueve la camara a esa localizacion
         */
        fun getPolygon(layer: GeoJsonLayer): GeoJsonPolygon? {
            for (feature in layer.features) {
                return feature.geometry as GeoJsonPolygon
            }
            return null
        }
        override fun onReceive(context: Context, p1: Intent) {
            if(p1.action==GpsService.GPS){
                val localizacion:Location=
                    p1.getSerializableExtra("localizacion") as Location
                val punto=LatLng(localizacion.latitude,localizacion.longitude)
                mMap.addMarker(MarkerOptions().position(punto).title("Marker in Costa Rica"))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(punto))
                if(PolyUtil.containsLocation(localizacion.latitude, localizacion.longitude, getPolygon(layer)!!.outerBoundaryCoordinates, false)==false){
                    Toast.makeText(context,"El punto está fuera",Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(context,"El punto está dentro",Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

}