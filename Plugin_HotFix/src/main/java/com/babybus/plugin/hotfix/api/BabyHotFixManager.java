package com.babybus.plugin.hotfix.api;

import com.babybus.app.App;
import com.babybus.utils.downloadutils.ApiManager;

/**
 * time:2018/5/4 上午11:36
 * author:maolinnan
 * desc:热修复请求管理
 **/

public class BabyHotFixManager {

    private static BabyHotFixService instance;
    private static BabyHotFixService stringInstance;

    public static BabyHotFixService get(){
        if (instance == null){
            synchronized (BabyHotFixManager.class){
                if (instance == null){
                    instance = ApiManager.get().create(BabyHotFixService.class);
                }
            }
        }
        return instance;
    }

    public static BabyHotFixService getStringInstance(){
        if (stringInstance == null){
            synchronized (BabyHotFixManager.class){
                if (stringInstance == null){
                    stringInstance = ApiManager.getStringInstance().create(BabyHotFixService.class);
                }
            }
        }
        return stringInstance;
    }

    /**
     * 活动信息接口
     * @return
     */
    public static String getHotFixRequestUrl() {
        String url;
        if (App.get().debug) {
            url = "http://app-zh.beta.baby-bus.com/api.php/v4/get_hotfix";
        } else {
            url = "http://app-zh.babybus.com/api.php/v4/get_hotfix";
        }
        return url;
    }
}
