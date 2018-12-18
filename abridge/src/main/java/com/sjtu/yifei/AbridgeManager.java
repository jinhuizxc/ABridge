package com.sjtu.yifei;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

;
import com.sjtu.yifei.aidl.IReceiverAidlInterface;
import com.sjtu.yifei.aidl.ISenderAidlInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * 类描述：
 * 创建人：yifei
 * 创建时间：2018/12/18
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
final class AbridgeManager {
    private static final String BIND_SERVICE_ACTION = "android.intent.action.ICALL_AIDL_YIFEI";
    private static final String BIND_SERVICE_COMPONENT_NAME_CLS = "com.sjtu.yifei.service.ABridgeService";
    private static AbridgeManager instance;

    private Application sApplication;
    private String sServicePkgName;
    private Handler sHandler;
    private List<AbridgeCallBack> sList;

    private AbridgeManager() {
        sList = new ArrayList<>();
    }

    public static AbridgeManager getInstance() {
        if (instance == null) {
            synchronized (AbridgeManager.class) {
                if (instance == null) {
                    instance = new AbridgeManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param sApplication
     * @param sServicePkgName
     */
    public void init(Application sApplication, String sServicePkgName) {
        this.sApplication = sApplication;
        this.sServicePkgName = sServicePkgName;
        sHandler = new Handler(sApplication.getMainLooper());
    }

    public void registerRemoteCallBack(AbridgeCallBack callBack) {
        if (sList != null) {
            sList.add(callBack);
        }
    }

    public void uRegisterRemoteCallBack(AbridgeCallBack callBack) {
        if (sList != null && callBack != null) {
            sList.remove(callBack);
        }
    }

    public void callRemote(String message) {
        if (!TextUtils.isEmpty(message)) {
            try {
                iSenderAidlInterface.sendMessage(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private IBinder sBinder = new Binder();
    private ISenderAidlInterface iSenderAidlInterface;

    private IReceiverAidlInterface iReceiverAidlInterface = new IReceiverAidlInterface.Stub() {

        @Override
        public void receiveMessage(final String json) throws RemoteException {
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (AbridgeCallBack medium : sList) {
                        medium.receiveMessage(json);
                    }
                }
            });
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            iSenderAidlInterface = ISenderAidlInterface.Stub.asInterface(iBinder);
            try {
                iSenderAidlInterface.join(sBinder);
                iSenderAidlInterface.registerCallback(iReceiverAidlInterface);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            iSenderAidlInterface = null;
        }
    };

    /**
     * 启动服务
     */
    public void startAndBindService() {
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(BIND_SERVICE_ACTION);
        serviceIntent.setComponent(new ComponentName(sServicePkgName, BIND_SERVICE_COMPONENT_NAME_CLS));
        sApplication.startService(serviceIntent);
        sApplication.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 关闭服务
     */
    public void unBindService() {
        if (iSenderAidlInterface != null) {
            try {
                iSenderAidlInterface.unregisterCallback(iReceiverAidlInterface);
                iSenderAidlInterface.leave(sBinder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            sApplication.unbindService(serviceConnection);
        }
    }

}
