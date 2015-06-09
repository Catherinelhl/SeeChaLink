package com.home.mainactivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.android.service.SharePreferenceUtil;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.home.listener.CommanTitle_Right_Listener;
import com.home.service.BackgroundService;
import com.home.adapter.SceneListAdapter;
import com.home.application.BaseApp;
import com.home.constants.Configer;
import com.home.db.AllSceneDB;
import com.home.util.MQTTClientUtil;
import com.home.view.CommonTitleView;
import com.nineoldandroids.view.ViewHelper;

/**
 * 
 * MQTT连接到中控（中转控制器），然后通过pub topic来控制设备的状态
 * 
 * @see IOT
 * 
 *      {@code 中控的ID：192.168.1.1
 *      first:connect the device
 *      Sec:pub topic to control the device}
 * 
 * 
 * */
public class MainActivity extends FragmentActivity {
	String TAG = "MainActivity";
	private SharePreferenceUtil preferens;
	SharedPreferences sp = null;

	MQTTClientUtil mcu = null;
	CommonTitleView commantitleView = null;

	GridView grid_Scene = null;
	SceneListAdapter adapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main_layout);

		preferens = new SharePreferenceUtil(this);
		preferens.setDeviceId();

	}

	@Override
	protected void onResume() {
		// 获取数据
		super.onResume();
		Log.i(TAG, "onResume");
		initDB();

	}

	private DrawerLayout mDrawerLayout;
	ArrayList<Map<String, Object>> SceneList = null;
	// String[] Scene_array = { "客房场景", "卧室场景", "起床场景", "添加场景" };
	ArrayList<String> sceneName = new ArrayList<String>();
	/**
	 * 查询数据库，然后得到所有的场景
	 * */
	AllSceneDB SceneDB = null;

	public void initDB() {
		Log.d(TAG, "initDB");
		SceneList = new ArrayList<Map<String, Object>>();
		if (null == SceneDB) {
			SceneDB = new AllSceneDB(MainActivity.this);
		}
		SelectSceneDB();
		initView();
		initEvents();
	}

	public void SelectSceneDB() {
		sceneName.clear();
		SceneList.clear();
		Cursor cursor = SceneDB.select();
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String name = cursor.getString(cursor
					.getColumnIndex(SceneDB.s_NAME));
			sceneName.add(name);
			Log.d(TAG, "initDB场景有====>" + name);
		}
		for (int i = 0; i < sceneName.size(); i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", sceneName.get(i).toString());
			map.put("image", R.drawable.home_addimg_bg);
			SceneList.add(map);
		}
	}

	public void initView() {
		BaseApp.getInstance().addActivity(this);
		Configer.PAGER = 1;
		commantitleView = (CommonTitleView) findViewById(R.id.toplayout);
		commantitleView.initData(MainActivity.this, RightListener, "智能家控");
		mDrawerLayout = (DrawerLayout) findViewById(R.id.id_drawerLayout);

		grid_Scene = (GridView) findViewById(R.id.grid_scene);

		adapter = new SceneListAdapter(this, SceneList);
		grid_Scene.setAdapter(adapter);
		// mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
		// Gravity.RIGHT);
		// mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
		// Gravity.LEFT);
		grid_Scene.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == SceneList.size()) {
					startActivity(new Intent().setClass(MainActivity.this,
							AddSceneActivity.class));
				} else {
					TOINTETTN(position, sceneName.get(position).toString());

				}

			}
		});
		grid_Scene.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == SceneList.size()) {
					startActivity(new Intent().setClass(MainActivity.this,
							AddSceneActivity.class));
				} else {
					ToShowDialog(position, SceneList.get(position).get("name")
							.toString());
				}
				return false;
			}
		});
		// 弹出上下文
		// registerForContextMenu(grid_Scene);
	}

	int position = 0;
	String deleteName = "";

	// 显示对话框
	private void ToShowDialog(int pos, String name) {
		position = pos;
		deleteName = name;
		final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
				.create();
		dialog.show();
		Window window = dialog.getWindow();
		// 设置布局
		window.setContentView(R.layout.content_view_opera);
		// 设置宽高
		window.setLayout(LayoutParams.FILL_PARENT, 350);
		window.setGravity(Gravity.BOTTOM);
		// 设置弹出的动画效果
		window.setWindowAnimations(R.style.AnimBottom);
		// 设置监听
		final Button btn_edit = (Button) window.findViewById(R.id.btn_edit);
		final Button btn_start_ontime = (Button) window
				.findViewById(R.id.btn_start_ontime);
		final Button btn_delete = (Button) window.findViewById(R.id.btn_delete);

		Button btn_cancel = (Button) window.findViewById(R.id.btn_cancel);
		btn_edit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				TOToast("编辑");
				btn_edit.setTextColor(0xFF14a1e3);
				btn_edit.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.btn_edit_icon2, 0, 0);

				dialog.cancel();

				TOINTETTN(position, deleteName);

			}
		});
		btn_start_ontime.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				TOToast("定时启动");
				btn_start_ontime.setTextColor(0xFF14a1e3);
				btn_delete.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.btn_timer_icon2, 0, 0);
				dialog.cancel();
			}
		});
		btn_delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				TOToast("删除");
				btn_delete.setTextColor(0xFF14a1e3);
				btn_delete.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.btn_delete_icon2, 0, 0);
				SceneDB.delete(deleteName);
				SelectSceneDB();
				dialog.cancel();

				grid_Scene.postInvalidate();

				adapter.notifyDataSetChanged();

			}
		});
		btn_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
		});
		// 因为我们用的是windows的方法，所以不管ok活cancel都要加上“dialog.cancel()”这句话，
		// 不然有程序崩溃的可能，仅仅是一种可能，但我们还是要排除这一点，对吧？
		// 用AlertDialog的两个Button，即使监听里什么也不写，点击后也是会吧dialog关掉的，不信的同学可以去试下

	}

	public void TOToast(String str) {
		Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
	}

	CommanTitle_Right_Listener RightListener = new CommanTitle_Right_Listener() {

		@Override
		public void DotRight(boolean isDot) {
			if (isDot) {
				OpenRightMenu();
			}
		}

		@Override
		public void DotLeft(boolean isDot) {
			if (isDot) {
				OpenLeftMenu();
			}
		}

		@Override
		public void DotRightEdit(boolean isEdit) {
			// TODO Auto-generated method stub

		}

		@Override
		public void DotRightFinish(boolean isFinish) {
			// TODO Auto-generated method stub

		}
	};

	private void initEvents() {
		mDrawerLayout.setDrawerListener(new DrawerListener() {
			@Override
			public void onDrawerStateChanged(int newState) {
				System.out.println("the newstate is===>" + newState);
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				View mContent = mDrawerLayout.getChildAt(0);
				View mMenu = drawerView;
				float scale = 1 - slideOffset;
				float rightScale = 0.8f + scale * 0.2f;

				if (drawerView.getTag().equals("LEFT")) {

					float leftScale = 1 - 0.3f * scale;

					ViewHelper.setScaleX(mMenu, leftScale);
					ViewHelper.setScaleY(mMenu, leftScale);
					ViewHelper.setAlpha(mMenu, 0.6f + 0.4f * (1 - scale));
					ViewHelper.setTranslationX(mContent,
							mMenu.getMeasuredWidth() * (1 - scale));
					ViewHelper.setPivotX(mContent, 0);
					ViewHelper.setPivotY(mContent,
							mContent.getMeasuredHeight() / 2);
					mContent.invalidate();
					ViewHelper.setScaleX(mContent, rightScale);
					ViewHelper.setScaleY(mContent, rightScale);
				} else {
					ViewHelper.setTranslationX(mContent,
							-mMenu.getMeasuredWidth() * slideOffset);
					ViewHelper.setPivotX(mContent, mContent.getMeasuredWidth());
					ViewHelper.setPivotY(mContent,
							mContent.getMeasuredHeight() / 2);
					mContent.invalidate();
					ViewHelper.setScaleX(mContent, rightScale);
					ViewHelper.setScaleY(mContent, rightScale);
				}

			}

			@Override
			public void onDrawerOpened(View drawerView) {

			}

			@Override
			public void onDrawerClosed(View drawerView) {
				mDrawerLayout.setDrawerLockMode(
						DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
			}
		});
	}

	public void OpenRightMenu() {
		mDrawerLayout.openDrawer(Gravity.RIGHT);
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
				Gravity.RIGHT);
	}

	public void OpenLeftMenu() {
		mDrawerLayout.openDrawer(Gravity.LEFT);
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
				Gravity.LEFT);
	}

	Intent intent = null;

	public boolean isConnect(Context context) {
		// 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
		try {
			ConnectivityManager connectivity = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				// 获取网络连接管理的对象
				NetworkInfo info = connectivity.getActiveNetworkInfo();
				if (info != null && info.isConnected()) {
					// 判断当前网络是否已经连接
					if (info.getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			Log.v("isconnect error", e.toString());
		}
		return false;
	}

	public void TOINTETTN(int id, String name) {
		intent = new Intent();
		intent.setClass(MainActivity.this, SceneActivity.class);
		intent.putExtra("id", id);
		intent.putExtra("name", name);
		// intent.put
		startActivity(intent);
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		// if (Configer.ISUNREGISTER) {
		// Log.i(TAG, "ISUNREGISTER=true");
		// } else {
		// Log.i(TAG, "ISUNREGISTER=false");
		if (isConnect(getBaseContext())) {
			// Configer.ISUNREGISTER = true;
			Intent intent = new Intent(this, BackgroundService.class);
			stopService(intent);
			// }
		}
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_HOME) {
			if (isConnect(getBaseContext())) {
				Intent intent = new Intent(this, BackgroundService.class);
				stopService(intent);
			}
			// finish();
			BaseApp.getInstance().exit();
			return false;
		} else {

			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("操作场景");
		// 添加菜单项
		menu.add(0, Menu.FIRST, 0, "编辑");
		menu.add(0, Menu.FIRST + 1, 0, "删除");
		menu.add(0, Menu.FIRST + 2, 0, "定时开启");
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Toast.makeText(MainActivity.this,
				"content" + item.getItemId() + info.position, Toast.LENGTH_LONG)
				.show();
		return super.onContextItemSelected(item);
	}
}
