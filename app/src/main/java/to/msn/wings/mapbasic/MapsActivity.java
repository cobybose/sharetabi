package to.msn.wings.mapbasic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
//import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapsActivity extends AppCompatActivity
    implements OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private GoogleApiClient client;
    private LocationRequest request;
    private FusedLocationProviderApi api;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //パーミッションの確認＆要求
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //位置情報のリクエスト情報を取得
        request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(15);
        api = LocationServices.FusedLocationApi;
        //Google Playへの接続クライアントを生成
        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //デフォルトの表示位置をセット
        LatLng current = new LatLng(35.670292, 139.773006);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Google Playへの接続
        if(client != null) {
            client.connect();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        //位置情報リクエストの解除、及び、Google Playからの切断
        if (client != null && client.isConnected()){
            api.removeLocationUpdates(client, this);
        }
        client.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle){
        //ACCESS_FIVE_LOCATIONへのパーミッションを確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        //位置情報の監視を開始
        api.requestLocationUpdates(client, request, this);
    }

    //接続が中断された時の処理（空実装）
    @Override
    public void onConnectionSuspended(int i){
    }

    //接続が切断された時の処理（空実装）
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){
    }

    //位置情報が更新された時、カメラ位置を移動
    @Override
    public void onLocationChanged(Location location){
        if(mMap == null){ return; }
        LatLng current = new LatLng(
                location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(current).title("I am here"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
    }






}