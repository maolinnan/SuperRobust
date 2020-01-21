package com.babybus.plugin.hotfix;

import android.text.TextUtils;

import com.babybus.app.App;
import com.babybus.base.BasePlugin;
import com.babybus.managers.ThreadManager;
import com.babybus.plugins.interfaces.IHotFix;
import com.babybus.utils.ApkUtil;
import com.babybus.utils.NetUtil;
import com.babybus.utils.SpUtil;

import java.io.File;

import jonathanfinerty.once.Once;

/**
 * time:2018/11/20 上午9:20
 * author:maolinnan
 * desc:热修复组件
 */

public class PluginHotFix extends BasePlugin implements IHotFix{
    //新版本标识
    private static final String HOTFIX_APP_NEW_VERSION = "hotfix_app_new_version";

    @Override
    public void onApplicationCreate() {
        super.onApplicationCreate();
        ThreadManager.getInstance().run(new Runnable() {
            @Override
            public void run() {
                //假如应用升级，清空保存的补丁信息
                if (!Once.beenDone(Once.THIS_APP_VERSION, HOTFIX_APP_NEW_VERSION)) {
                    SpUtil.putString(PatchManger.PATCH_MD5_CONFIG,"");
                    SpUtil.putString(PatchManger.PATCH_GAME2D_MD5_CONFIG,"");
                    SpUtil.putString(PatchManger.PATCH_GAME3D_MD5_CONFIG,"");
                    Once.markDone(HOTFIX_APP_NEW_VERSION);
//                  return; //理论上要退出的，但是为了sd卡补丁测试，所以继续往下走。
                }
                if (PatchManger.get().getPatch() != null) {
                    PatchManger.get().runRobust();
                }
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!ApkUtil.isInternationalApp() && NetUtil.isNetActive()) {
            PatchManger.get().startRequestServiceHotFixInfo();
        }
    }

    /**
     * 获取当前生效补丁补丁资源所在路径
     * @return
     */
    public static String getPatchResPath(){
        return PatchManger.get().getPatchResPath();
    }

    @Override
    public void startRequestServiceHotFixInfo() {

        if (!ApkUtil.isInternationalApp() && NetUtil.isNetActive()) {
            PatchManger.get().startRequestServiceHotFixInfo();
        }
    }

    @Override
    public String getGamePatchPath() {
        if (App.get().u3d){//3D游戏
            //判断sd卡是否有测试补丁包,有则优先应用该补丁
            File debugFile = new File(PatchManger.GAME3D_DEBUG_PATCH_PATH);
            if (debugFile.exists() && debugFile.listFiles().length == 1){
                File patchFile = debugFile.listFiles()[0];
                return patchFile.getPath()+"/";
            }

            String md5Config = SpUtil.getString(PatchManger.PATCH_GAME3D_MD5_CONFIG,"");
            if (TextUtils.isEmpty(md5Config)){
                return "";
            }
            String patchPath = PatchManger.GAME3D_RELEASE_PATCH_PATH + md5Config;
            File file = new File(patchPath);
            if (file.exists() && file.listFiles().length > 0){
                return patchPath+"/";
            }
        }else{//2d游戏
            //判断sd卡是否有测试补丁包,有则优先应用该补丁
            File debugFile = new File(PatchManger.GAME2D_DEBUG_PATCH_PATH);
            if (debugFile.exists() && debugFile.listFiles().length == 1){
                File patchFile = debugFile.listFiles()[0];
                return patchFile.getPath()+"/";
            }

            String md5Config = SpUtil.getString(PatchManger.PATCH_GAME2D_MD5_CONFIG,"");
            if (TextUtils.isEmpty(md5Config)){
                return "";
            }
            String patchPath = PatchManger.GAME2D_RELEASE_PATCH_PATH + md5Config;
            File file = new File(patchPath);
            if (file.exists() && file.listFiles().length > 0){
                return patchPath + "/";
            }
        }
        return "";
    }
}
