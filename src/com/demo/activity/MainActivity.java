package com.demo.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.demo.adapter.FriendsAdapter;
import com.demo.service.UserServiceImpl;
import com.demo.xmppchat.R;

public class MainActivity extends Activity {

	private XMPPConnection connection;
	private ArrayList<String> messages = new ArrayList<String>();
	private Handler mHandler = new Handler();
	private ListView listview;
	private Button btn_OK_PLAY;
	private Button btn_Cancel;
	private ListView friendlistView;
	private PopupWindow popFriends;
	private PopupWindow popStreamingLink;
	private FriendsAdapter friendsAdapter;
	private List<Map<String, String>> listMap;
	private ArrayList<String> selectedListMap = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		String username = getIntent().getStringExtra("username");
		String password = getIntent().getStringExtra("password");

		// popup all the friends
		popupContactList(username, password);
		connection = GetConnection(username, password);
		// Set the status to available
		Presence presence = new Presence(Presence.Type.available);
		connection.sendPacket(presence);
		// get message listener
		setListenerConnection(connection);

	}

	// Select contact function

	private void popupContactList(String username, String password) {

		final View v = getLayoutInflater().inflate(R.layout.friendlist, null,
				false);
		int h = getWindowManager().getDefaultDisplay().getHeight();
		int w = getWindowManager().getDefaultDisplay().getWidth();

		listMap = new ArrayList<Map<String, String>>();

		popFriends = new PopupWindow(v, w - 10, (int) (((2.8) * h) / 4));
		popFriends.setAnimationStyle(R.style.MyDialogStyleBottom);
		popFriends.setFocusable(true);
		popFriends.setBackgroundDrawable(new BitmapDrawable());
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				popFriends.showAtLocation(v, Gravity.BOTTOM, 0, 0);
			}
		}, 1000L);

		friendlistView = (ListView) v.findViewById(R.id.friendlist);
		friendlistView.setItemsCanFocus(true);
		friendsAdapter = new FriendsAdapter(username, password, this, listMap);

		friendlistView.setAdapter(friendsAdapter);
		friendlistView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long arg3) {
				// TODO Auto-generated method stub
				TextView name = (TextView) v.findViewById(R.id.friend_username);
				CheckBox checkbox = (CheckBox) v.findViewById(R.id.check_box);
				checkbox.toggle();

				friendsAdapter.getIsSelected().put(position,
						checkbox.isChecked());

				if (checkbox.isChecked()) {
					selectedListMap.add(listMap.get(position).get("username"));

				}
			}
		});

		btn_OK_PLAY = (Button) v.findViewById(R.id.btn_play);
		btn_OK_PLAY.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String to = "user11@myria";
				String text = "http://129.128.184.46:8554/live.sdp";

				System.out.println(selectedListMap.size());
				for (int i = 0; i < selectedListMap.size(); i++) {
					Log.i("XMPPChatDemoActivity", "Sending text " + text
							+ " to " + selectedListMap.get(i));
					Message msg = new Message(selectedListMap.get(i),
							Message.Type.chat);
					msg.setBody(text);
					if (connection != null) {
						connection.sendPacket(msg);
						messages.add(connection.getUser() + ":");
						messages.add(text);
						popFriends.dismiss();
					}
				}
			}
		});
	}

	private XMPPConnection GetConnection(String username, String passwd) {
		try {
			if (null == connection || !connection.isAuthenticated()) {
				XMPPConnection.DEBUG_ENABLED = true;

				ConnectionConfiguration config = new ConnectionConfiguration(
						UserServiceImpl.SERVER_HOST,
						UserServiceImpl.SERVER_PORT,
						UserServiceImpl.SERVER_NAME);
				config.setReconnectionAllowed(true);
				config.setSendPresence(true);
				config.setSASLAuthenticationEnabled(true);
				connection = new XMPPConnection(config);
				connection.connect();
				connection.login(username, passwd);

				return connection;
			}
		} catch (XMPPException xe) {
			Log.e("XMPPChatDemoActivity", xe.toString());
		}
		return null;
	}

	public void setListenerConnection(XMPPConnection connection) {
		this.connection = connection;
		if (connection != null) {
			// Add a packet listener to get messages sent to us
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			connection.addPacketListener(new PacketListener() {
				@Override
				public void processPacket(Packet packet) {
					Message message = (Message) packet;
					if (message.getBody() != null) {
						String fromName = StringUtils.parseBareAddress(message
								.getFrom());
						Log.i("XMPPChatDemoActivity", "Text Recieved "
								+ message.getBody() + " from " + fromName);
						messages.add(fromName + ":");
						messages.add(message.getBody());
						final String msg = message.getBody().toString();
						// Add the incoming message to the list view
						mHandler.post(new Runnable() {
							public void run() {
								popupReceiveStreamingLinkMessage(msg);
							}
						});
					}
				}
			}, filter);
		}
	}

	private void popupReceiveStreamingLinkMessage(String message) {

		final View v = getLayoutInflater().inflate(R.layout.streaminglink,
				null, false);

		int h = getWindowManager().getDefaultDisplay().getHeight();
		int w = getWindowManager().getDefaultDisplay().getWidth();

		popStreamingLink = new PopupWindow(v, w - 10, (int) (((2.8) * h) / 4));
		popStreamingLink.setAnimationStyle(R.style.MyDialogStyleBottom);
		popStreamingLink.setFocusable(true);
		popStreamingLink.setBackgroundDrawable(new BitmapDrawable());
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				popStreamingLink.showAtLocation(v, Gravity.BOTTOM, 0, 0);
			}
		}, 100L);

		TextView stramingLink = (TextView) v.findViewById(R.id.streaming_link);
		stramingLink.setText(message);
		btn_OK_PLAY = (Button) v.findViewById(R.id.btn_play_streaming);
		btn_OK_PLAY.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				popStreamingLink.dismiss();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (connection != null)
				connection.disconnect();
		} catch (Exception e) {

		}
	}
}
