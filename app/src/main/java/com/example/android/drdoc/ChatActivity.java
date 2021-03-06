package com.example.android.drdoc;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Locale;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

public class ChatActivity extends Activity {
    private static final String TAG = "ChatActivity";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;
    private boolean side = false;
    TextToSpeech tts;

    private WebSocketConnection mWebSocketClient;
    private boolean mWebSocketIsOpen = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        buttonSend = (Button) findViewById(R.id.send);

        listView = (ListView) findViewById(R.id.msgview);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.right);
        listView.setAdapter(chatArrayAdapter);

        chatText = (EditText) findViewById(R.id.msg);
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    SendWebsocketMessage("send", chatText.getText().toString());
                    return sendChatMessage(true, chatText.getText().toString());
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                SendWebsocketMessage("send", chatText.getText().toString());
                sendChatMessage(true, chatText.getText().toString());
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });

        connectWebSocket();
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });

        sendChatMessage(false, "Hello there! I'm Doctor Doc. I received your pay slip and I am analyzing it. Feel free to ask questions.");
        //tts.speak("Hello World", TextToSpeech.QUEUE_FLUSH, null);

    }

    private void connectWebSocket() {

        //mConnectTrySkip = 3;
        mWebSocketClient = new WebSocketConnection();
        try {
            mWebSocketClient.connect("ws://130.211.184.58:8002", new WebSocketHandler() {
                @Override
                public void onOpen() {
                    Log.d("WEBSOCKETS", "Connected to server!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    mWebSocketIsOpen = true;
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.i("WEBSOCKETS", payload);

                    try {
                        JSONObject reader = new JSONObject(payload);
                        String cmd  = reader.getString("cmd");
                        String data = reader.getString("data");

                        Log.i("WEBSOCKETS", data);
                        if (cmd.equalsIgnoreCase("receive")) {
                            sendChatMessage(false, data);
                        }

                    } catch (JSONException e) {

                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    // Debug
                    Log.d("WEBSOCKETS", "Connection lost.");
                }
            });
        } catch(WebSocketException wse) {
            Log.d("WEBSOCKETS", wse.getMessage());
        }


        // for some reason this work in debug mode only
        //mWebSocketClient.connect();

    }

    private void SendWebsocketMessage(String key, String value) {
        if ((!mWebSocketClient.isConnected()) || !mWebSocketIsOpen) return;

        try
        {
            JSONObject jasonData = new JSONObject();
            jasonData.put("cmd", key);
            jasonData.put("data", value);
            mWebSocketClient.sendTextMessage(jasonData.toString());
        }
        catch(JSONException e)
        {
            //some exception handler code.
        }
    }

    private boolean sendChatMessage(boolean userTalks, String text) {
        chatArrayAdapter.add(new ChatMessage(userTalks, text));
        chatText.setText("");
        side = !side;
        return true;
    }
}