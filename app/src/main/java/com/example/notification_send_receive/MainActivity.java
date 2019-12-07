package com.example.notification_send_receive;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.notification_send_receive.ui.Send.SendFragment;
import com.example.notification_send_receive.ui.client;
import com.example.notification_send_receive.ui.home.HomeFragment;
import com.example.notification_send_receive.ui.notifications.NotificationsFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static com.google.android.gms.common.internal.safeparcel.SafeParcelable.NULL;

public class MainActivity extends AppCompatActivity {
    MqttAndroidClient client;
    private RequestQueue requestQueue;
    private Receiver Receiver;
    MqttConnectOptions options = new MqttConnectOptions();

    ArrayAdapter adapter;
    ArrayList<String> isSubTopic = new ArrayList<String>();

    //MQTT
    private EditText in_message;
    //private String sub_topic = "Firebase_MQTT";
    private TextView tokenView;
    private TextView MQTTtokenView;
    TextView MQTTtoken;

    //Location
    Location location;
    private String currentLocation;
    boolean getService = false;     //是否已開啟定位服務
    private LocationManager lms;
    private String bestProvider = LocationManager.GPS_PROVIDER;

    client currentClient = new client();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        in_message = (EditText) findViewById(R.id.message);

        //Location 取得系統定位服務
        LocationManager status = (LocationManager) (this.getSystemService(Context.LOCATION_SERVICE));
        if(status.isProviderEnabled(LocationManager.GPS_PROVIDER)|| status.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            //如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
            currentLocation = locationServiceInitial();
            lms.requestLocationUpdates(bestProvider, 10, 100, locationListener);
            //Toast.makeText(MainActivity.this, "currentLocation: "+currentLocation, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please turn on location service", Toast.LENGTH_LONG).show();
            getService = true; //確認開啟定位服務
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); //開啟設定頁面
        }
        // onCreate


        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1);
        tokenView = findViewById(R.id.tokenView);
//        MQTTtokenView = findViewById(R.id.MQTTtokenView);

        Receiver=new Receiver();

//        getTokenFirebase();
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        String token = task.getResult().getToken();
                        tokenView.setText(token);
//                        currentClient.setClientId(token);
//                        MQTTtokenView.setText(token);
//                        currentClient.setClientId(token);
                    }
                });


        //receive from Firebase
//        getTokenFirebase();
        if (Receiver != null) {
            IntentFilter intentFilter = new IntentFilter("NOW");
            LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver( Receiver, intentFilter);
        }

        //final String clientId = MqttClient.generateClientId();
//        final String clientId = "ExampleAndroidClient1";
//        final String clientId = currentClient.getClientId();
        final String clientId = tokenView.getText().toString();
        adapter.add("clientId: "+ clientId);
        final String url = "tcp://192.168.0.100:1883";
        Toast.makeText(MainActivity.this, clientId, Toast.LENGTH_LONG).show();
        client =
                new MqttAndroidClient(MainActivity.this, url, clientId);

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(MainActivity.this, "Connect MQTT Successfully", Toast.LENGTH_LONG).show();
//                    MQTTtokenView.setText(clientId);
                    subscribe("Firebase_MQTT");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(MainActivity.this, "Failed to Connect MQTT", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_send, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


        //Receive notification from MQTT
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(MainActivity.this, "lost conncetion", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                MQTTtokenView = findViewById(R.id.MQTTtokenView);
//                String currentToken = MQTTtokenView.getText().toString();
                String currentToken = tokenView.getText().toString();

                String msg = new String(message.getPayload());
                JSONObject temp_jsonMessage = new JSONObject(msg);
                JSONObject jsonMessage = new JSONObject();
                String receiverToken = temp_jsonMessage.get("to").toString();

                jsonMessage = temp_jsonMessage.getJSONObject("data");
                JSONObject jsonParam = jsonMessage.getJSONObject("params");
                JSONObject jsonToken = jsonMessage.getJSONObject("Token");
                String senderToken = jsonToken.get("SenderToken").toString();

                if(receiverToken.equals(currentToken)){
                    if(!senderToken.equals(currentToken)){
//                        jsonMessage = temp_jsonMessage.getJSONObject("data");
                        String title = jsonParam.get("title").toString();
                        String description = jsonParam.get("description").toString();
                        String final_msg = "Title: " + title +"\n" + "Description: " + description /*+"\n" + "Latitude: " + latitude +"\n" + "Longitude: " + longitude*/;
                        adapter.add(final_msg);

                        //send back Location
                        temp_jsonMessage.put("Location", currentLocation);
                        temp_jsonMessage.put("to", senderToken);
                        String sendBackData = temp_jsonMessage.toString();
                        sendMQTT(sendBackData);
                    }
                    //senderToken == currentToken
                    else{
                        String Location = temp_jsonMessage.get("Location").toString();
                        String fireBase_msg = /*"Send notification to: " + receiver_token + "\n" +*/ "Receiver Location: " + Location +"\n";
                        adapter.add(fireBase_msg);
                    }
                }
                else{

                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    //Receive notification from FireBase
    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("FirebaseData");
            //Receiver(phone 2) receiving post request from phone 1 & send back location information
            try {
                JSONObject jsonData = new JSONObject(message);

                JSONObject jsonParam = jsonData.getJSONObject("params");
                String title = jsonParam.get("title").toString();
                String description = jsonParam.get("description").toString();
                String fireBase_msg = "Title: " + title +"\n" + "Description: " + description /*+"\n" + "Latitude: " + latitude +"\n" + "Longitude: " + longitude*/;
                adapter.add(fireBase_msg);

                JSONObject locationJson = new JSONObject();
                JSONObject locationJson2 = new JSONObject();
                JSONObject sendBackJson = new JSONObject();
                //Toast.makeText(MainActivity.this, "currentLocation: "+currentLocation, Toast.LENGTH_SHORT).show();

                JSONObject jsonToken = jsonData.getJSONObject("Token");
                String SenderToken = jsonToken.get("SenderToken").toString();
                locationJson.put("Location", currentLocation);
                locationJson2.put("SendBackLocation", locationJson);
                sendBackJson.put("to", SenderToken);
                sendBackJson.put("data", locationJson2);
                sendBackJson.put("target", "Firebase");
                String sendBackData = sendBackJson.toString();
                SEND(sendBackData);

            } catch (JSONException e) {
                //Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }

            //sender(phone 1) receiving location information form phone 2
            try {
                JSONObject jsonData = new JSONObject(message);
                JSONObject jsonLocation = jsonData.getJSONObject("SendBackLocation");
                String Location = jsonLocation.get("Location").toString();
                String fireBase_msg = /*"Send notification to: " + receiver_token + "\n" +*/ "Receiver Location: " + Location +"\n";
                adapter.add(fireBase_msg);

            }catch (JSONException e) {
                //Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    //Firebase Get Token
    private void getTokenFirebase() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        String token = task.getResult().getToken();
                        tokenView.setText(token);
//                        currentClient.setClientId(token);
//                        MQTTtokenView.setText(token);
                        currentClient.setClientId(token);
                    }
                });
    }

    //FireBase : SEND
    public void SEND(String data)
    {
        final String savedata= data;
        String URL="https://fcm.googleapis.com/fcm/send";

        requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject objres=new JSONObject(response);
                    Toast.makeText(MainActivity.this,"Firebase Send Successfully ",Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this,"Server Error",Toast.LENGTH_LONG).show();

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "key=AAAAc0zpQtE:APA91bGU3-3v7u8E89ADDrahkoimeghnpnUprGsNvxABOgUB1UTUEXbK61FAZwjqhE4YW5ZlGH5yq3-UTfFLDDo9J2lMBUgaOWWfckbp4PbOwY0y2Rrz_lMmVz3YoXNmGtMTYg4AJwCg");
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return savedata == null ? null : (savedata).getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    //Log.v("Unsupported Encoding while trying to get the bytes", data);
                    return null;
                }
            }

        };
        requestQueue.add(stringRequest);
    }

    //MQTT: SEND
    public void sendMQTT(String message){
//        MQTTtoken = findViewById(R.id.MQTTtokenView);
        byte[] encodedMessage = new byte[0];
        String strMessage;
        try {
            try{
                JSONObject inMessage = new JSONObject(message);
                strMessage = inMessage.toString();
            }
            catch (JSONException e){
                strMessage = "";
            }
            encodedMessage = strMessage.getBytes("UTF-8");
            MqttMessage fianl_message = new MqttMessage(encodedMessage);
            //fianl_message.setRetained(true);//Store last message
            client.publish("Firebase_MQTT", fianl_message);
            Toast.makeText(MainActivity.this, "Send to MQTT Successfully", Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to Send", Toast.LENGTH_SHORT).show();
        }
    }

    //Subscribe for MQTT
    private void subscribe(String in_topic)
    {
        String topic = in_topic;
        int qos = 1;
        if(isSubTopic.contains(topic) == false){
            try {
                IMqttToken subToken = client.subscribe(topic, qos);
                subToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // The message was published
                        //Toast.makeText(MainActivity.this, "Subscribe Successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        // The subscription could not be performed, maybe the user was not
                        // authorized to subscribe on the specified topic e.g. using wildcards
                        Toast.makeText(MainActivity.this, "Failed to Subscribe", Toast.LENGTH_SHORT).show();

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
            isSubTopic.add(topic);
        }
    }

    //Location

    private String locationServiceInitial() {
        lms = (LocationManager) getSystemService(LOCATION_SERVICE); //取得系統定位服務
         /*做法一,由程式判斷用GPS_provider
           if (lms.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
               location = lms.getLastKnownLocation(LocationManager.GPS_PROVIDER);  //使用GPS定位座標
         }
         else if ( lms.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
         { location = lms.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); //使用GPS定位座標
         }
         else {}*/
        // 做法二,由Criteria物件判斷提供最準確的資訊
        Criteria criteria = new Criteria();  //資訊提供者選取標準
        bestProvider = lms.getBestProvider(criteria, true);    //選擇精準度最高的提供者
        location = lms.getLastKnownLocation(bestProvider);

        String InitLocation = getLocation(location);
        return InitLocation;
    }

    private String getLocation(Location location) {
        String GETLocation;
        if(location != null) {
            Double longitude = location.getLongitude();
            Double latitude = location.getLatitude();
            currentLocation = "(" + latitude +", " + longitude + ")";
            GETLocation = "(" + latitude +", " + longitude + ")";
            Toast.makeText(MainActivity.this, "GET Location: "+ GETLocation, Toast.LENGTH_SHORT).show();
        }
        else {
            GETLocation ="NULL";
            Toast.makeText(this, "Unable to locate coordinates", Toast.LENGTH_LONG).show();
        }
        return GETLocation;
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onLocationChanged(Location location) {
            // New Location return here
            Toast.makeText(MainActivity.this, "onLocationChanged", Toast.LENGTH_SHORT).show();
            getLocation(location);
        }
    };



    public MqttAndroidClient getClient() {return client;}

    public ArrayAdapter getAdapter() {return adapter;}

}
