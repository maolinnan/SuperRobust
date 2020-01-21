package com.babybus.plugin.hotfix.api;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * time:2018/5/4 上午11:37
 * author:maolinnan
 * desc:this is RePluginService
 **/

public interface BabyHotFixService {
    /**
     * 获取当前应用适配的补丁列表
     * @return
     */
    @FormUrlEncoded
    @POST()
    Call<BabyHotFixResponseBean> getHotFixList(@Url String url,@Field("version2d") String version2d,@Field("version3d") String version3d);
}
