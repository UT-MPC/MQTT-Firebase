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
    private Button btn_send;
    private SwitchCompat mSwitchCompat;
    private TextView token;
    private TextView MQTTtoken;

    MqttAndroidClient client;
    private RequestQueue requestQueue;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_send, container, false);
        in_message = (EditText) root.findViewById(R.id.message);
        btn_send = (Button) root.findViewById(R.id.btn_send);
        token = getActivity().findViewById(R.id.tokenView);
//        MQTTtoken = getActivity().findViewById(R.id.MQTTtokenView);

        client = ((MainActivity)getActivity()).getClient();

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
                                    //String SenderToken = MQTTtoken.getText().toString();
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
                                        ((MainActivity)getActivity()).sendMQTT(strData);

                                    }
                                    catch (JSONException e){

                                    }
                                }
                            });
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
}