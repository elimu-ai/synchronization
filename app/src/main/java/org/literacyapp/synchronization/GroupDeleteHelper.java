package org.literacyapp.synchronization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.net.wifi.p2p.WifiP2pManager.Channel;

public class GroupDeleteHelper   {
	
	private Context ctx;
	private Channel mChannel;
	private WifiP2pManager mManager;
	
	public GroupDeleteHelper(Context context) {
		ctx = context;
	}
	
	public void deleteGroups() {
		mManager = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(ctx, ctx.getMainLooper(), null);
		deletePersistentInfo();
	}



	private void deletePersistentInfo() {
		try {

			Class persistentInterface = null;

			//Iterate and get class PersistentGroupInfoListener
			for (Class<?> classR : WifiP2pManager.class.getDeclaredClasses()) {
				if (classR.getName().contains("PersistentGroupInfoListener")) {
					persistentInterface = classR;
					break;
				}

			}

			final Method deletePersistentGroupMethod = WifiP2pManager.class.getDeclaredMethod("deletePersistentGroup", new Class[]{Channel.class, int.class, WifiP2pManager.ActionListener.class});

			//anonymous class to implement PersistentGroupInfoListener which has a method, onPersistentGroupInfoAvailable
			Object persitentInterfaceObject =
					java.lang.reflect.Proxy.newProxyInstance(persistentInterface.getClassLoader(),
							new java.lang.Class[]{persistentInterface},
							new java.lang.reflect.InvocationHandler() {
								@Override
								public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws java.lang.Throwable {
									String method_name = method.getName();

									if (method_name.equals("onPersistentGroupInfoAvailable")) {
										Class wifiP2pGroupListClass =  Class.forName("android.net.wifi.p2p.WifiP2pGroupList");
										Object wifiP2pGroupListObject = wifiP2pGroupListClass.cast(args[0]);

										Collection<WifiP2pGroup> wifiP2pGroupList = (Collection<WifiP2pGroup>) wifiP2pGroupListClass.getMethod("getGroupList", null).invoke(wifiP2pGroupListObject, null);
										for (WifiP2pGroup group : wifiP2pGroupList) {
											deletePersistentGroupMethod.invoke(mManager, mChannel, (Integer) WifiP2pGroup.class.getMethod("getNetworkId").invoke(group, null), new WifiP2pManager.ActionListener() {
												@Override
												public void onSuccess() {
													Log.i(P.Tag, "Persistent Groups deleted");
												}

												@Override
												public void onFailure(int i) {
													Log.e(P.Tag, "Persistent Groups not deleted");
												}
											});
										}
									}

									return null;
								}
							});

			Method requestPersistentGroupMethod =
					WifiP2pManager.class.getDeclaredMethod("requestPersistentGroupInfo", new Class[]{Channel.class, persistentInterface});

			requestPersistentGroupMethod.invoke(mManager, mChannel, persitentInterfaceObject);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}





}
