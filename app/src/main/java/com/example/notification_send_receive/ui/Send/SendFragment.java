package com.example.notification_send_receive.ui.Send;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.notification_send_receive.MainActivity;
import com.example.notification_send_receive.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class SendFragment extends Fragment {

    private EditText in_message;
    //private EditText topic;
    private Button btn_send;
    //private Button btn_subscribe;
    //private Button btn_unsubscribe;
    private SwitchCompat mSwitchCompat;
    private TextView token;
    private TextView MQTTtoken;

    MqttAndroidClient client;
    //ArrayList<String> isSubTopic;//
    private RequestQueue requestQueue;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_send, container, false);
        in_message = (EditText) root.findViewById(R.id.message);
        //topic = (EditText) root.findViewById(R.id.topic);
        btn_send = (Button) root.findViewById(R.id.btn_send);
        //btn_subscribe = (Button) root.findViewById(R.id.btn_subscribe);
        //btn_unsubscribe = (Button) root.findViewById(R.id.btn_unsubscribe);
        token = getActivity().findViewById(R.id.tokenView);
        MQTTtoken = getActivity().findViewById(R.id.MQTTtokenView);

        client = ((MainActivity)getActivity()).getClient();
        //isSubTopic = ((MainActivity)getActivity()).getIsSubTopic();

        //Button Switch
        mSwitchCompat = root.findViewById(R.id.s_switch);
        mSwitchCompat.setChecked(false);

        //default for firebase
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String SenderToken = token.getText().toString();//
                String data = in_message.getText().toString();
                try{
                    JSONObject inData = new JSONObject(data);
                    JSONObject oldData = inData.getJSONObject("data");
                    //put token in
                    JSONObject extraToken = new JSONObject();
                    extraToken.put("SenderToken", SenderToken);
                    oldData.put("Token", extraToken);

                    inData.put("data", oldData);
                    String strData = inData.toString();
                    //Toast.makeText(getActivity(), strData, Toast.LENGTH_LONG).show();
                    ((MainActivity)getActivity()).SEND(strData);

                }
                catch (JSONException e){

                }
            }
        });

        mSwitchCompat.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            //MQTT
                            btn_send.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    String message = in_message.getText().toString();
                                    String SenderToken = MQTTtoken.getText().toString();
                                    //String sub_topic = topic.getText().toString();
                                    ((MainActivity)getActivity()).sendMQTT(message, SenderToken);
                                }
                            });
//                            btn_subscribe.setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    String sub_topic = topic.getText().toString();
//                                    subscribe(sub_topic);
//                                }
//                            });
//                            btn_unsubscribe.setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    String sub_topic = topic.getText().toString();
//                                    unsubscribe(sub_topic);
//                                }
//                            });
                        } else {
                            //Firebase
                            btn_send.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String SenderToken = token.getText().toString();//
                                    String data = in_message.getText().toString();
                                    try{
                                        JSONObject inData = new JSONObject(data);
                                        JSONObject oldData = inData.getJSONObject("data");
                                        //put token in
                                        JSONObject extraToken = new JSONObject();
                                        extraToken.put("SenderToken", SenderToken);
                                        oldData.put("Token", extraToken);

                                        inData.put("data", oldData);
                                        String strData = inData.toString();
                                        //Toast.makeText(getActivity(), strData, Toast.LENGTH_LONG).show();
                                        ((MainActivity)getActivity()).SEND(strData);

                                    }
                                    catch (JSONException e){

                                    }
                                }
                            });
                        }
                    }
                }
        );

    return root;

    }

    //Subscribe for MQTT
//    private void subscribe(String in_topic)
//    {
//        String topic = in_topic;
//        int qos = 1;
//        if(isSubTopic.contains(topic) == false){
//            try {
//                IMqttToken subToken = client.subscribe(topic, qos);
//                subToken.setActionCallback(new IMqttActionListener() {
//                    @Override
//                    public void onSuccess(IMqttToken asyncActionToken) {
//                        // The message was published
//                        Toast.makeText(getActivity(), "Subscribe Successfully", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFailure(IMqttToken asyncActionToken,
//                                          Throwable exception) {
//                        // The subscription could not be performed, maybe the user was not
//                        // authorized to subscribe on the specified topic e.g. using wildcards
//                        Toast.makeText(getActivity(), "Failed to Subscribe", Toast.LENGTH_SHORT).show();
//
//                    }
//                });
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//            isSubTopic.add(topic);
//        }
//    }
//
//    private void unsubscribe(String in_topic)
//    {
//        String topic = in_topic;
//        int qos = 1;
//        if(isSubTopic.contains(topic) == false){
//            try {
//                IMqttToken unsubToken = client.unsubscribe(topic);
//                unsubToken.setActionCallback(new IMqttActionListener() {
//                    @Override
//                    public void onSuccess(IMqttToken asyncActionToken) {
//                        // The subscription could successfully be removed from the client
//                        Toast.makeText(getActivity(), "Unsubscribe Successfully", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFailure(IMqttToken asyncActionToken,
//                                          Throwable exception) {
//                        // some error occurred, this is very unlikely as even if the client
//                        // did not had a subscription to the topic the unsubscribe action
//                        // will be successfully
//                        Toast.makeText(getActivity(), "Failed to Unsubscribe", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
//            isSubTopic.remove(topic);
//        }
//    }
}