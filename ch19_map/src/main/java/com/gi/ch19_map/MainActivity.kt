package com.gi.ch19_map

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

        //위치 정보를 가져오는 클라이언트
    lateinit var providerClient: FusedLocationProviderClient

    //Google API와 연결을 관리하는 클라이언트
    lateinit var apiClient: GoogleApiClient
    //실제 지도 객체를 담은 변수
    var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //요청 다이얼로그 표시
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ){
            if(it.all { permission -> permission.value == true}){
                apiClient.connect()
            }else {
                Toast.makeText(this, "권한 거부", Toast.LENGTH_SHORT).show()
            }
        }

        //xml에 있는 프래그먼트를 SupportMapFragment로 캐스팅 하고 비동기로 지도 준비 요청 -> 준비되면 onMapReady() 호출됨
        (supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment)!!.getMapAsync(this)

        //위치 클라이언트 초기화
        providerClient = LocationServices.getFusedLocationProviderClient(this)

        //Google API 클라이언트 빌더 패턴으로 생성 -> 위치 제공자 있을때, 없을때,
        apiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            !== PackageManager.PERMISSION_GRANTED){
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }else {
            //이미 권한이 있으면 바로 Google API 연결
            apiClient.connect()
        }
    }

    override fun onConnected(p0: Bundle?) {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            === PackageManager.PERMISSION_GRANTED){
            //마지막 위치를 불러옴, 만약 새폰이라면 null반환하므로 그때는 실시간 위치 요청으로 바꿔야 함.
            providerClient.lastLocation.addOnSuccessListener(
                this@MainActivity,
                object : OnSuccessListener<Location> {
                    override fun onSuccess(p0: Location?) {
                        p0?.let {
                            val latitude = p0.latitude
                            val longitude = p0.longitude
                            moveMap(latitude, longitude)
                        }
                    }
                }
            )
            apiClient.disconnect()
        }
    }

    private fun moveMap(latitude: Double, longitude: Double){
        val latLng = LatLng(latitude, longitude)
        val position: CameraPosition = CameraPosition.Builder()
            .target(latLng)
            .zoom(16f)
            .build()
        googleMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(position))
        val markerOption = MarkerOptions()
        markerOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        markerOption.position(latLng)
        markerOption.title("MyLocation")
        googleMap?.addMarker(markerOption)
    }

    //위치 제공자를 사용할 수 없을 때 -> 일시적 문제 -> 네트워크가 갑자기 끊김, 곧 복구될 가능성이 높음
    override fun onConnectionSuspended(p0: Int) {

    }

    //사용할 수 있는 위치 제공자가 없을 때 -> 연결 실패
    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0

    }
}