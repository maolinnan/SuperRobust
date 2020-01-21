package com.babybus.plugin.hotfix.robust;

import com.babybus.analytics.AiolosAnalytics;
import com.babybus.plugin.hotfix.HotFixAiolosKey;
import com.babybus.plugin.hotfix.PatchManger;
import com.babybus.utils.LogUtil;
import com.meituan.robust.Patch;
import com.meituan.robust.RobustCallBack;

import java.util.List;


/**
 * 补丁状态回调
 */
public class HotFixRobustCallBack implements RobustCallBack {
    private boolean isFaile = false;
    @Override
    public void onPatchListFetched(boolean result, boolean isNet, List<Patch> patches) {//无用
    }
    @Override
    public void onPatchFetched(boolean result, boolean isNet, Patch patch) {//无用
    }

    @Override
    public void onPatchApplied(boolean result, Patch patch) {
        //应用补丁包成功/失败
        if (result){//补丁应用成功
            PatchManger.get().validPatch = patch;
            LogUtil.e("=====hotfix=====","应用补丁成功");
            if (isFaile){//应用补丁包出现异常result也会是true,这里排除掉
                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_APPLIED,"java-失败",patch.getMd5());
            }else {
                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_APPLIED, "java-成功", patch.getMd5());
            }
        }else{//补丁应用失败
            LogUtil.e("=====hotfix=====","应用补丁失败");
            AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_APPLIED,"java-失败",patch.getMd5());
        }
        isFaile = false;
    }

    @Override
    public void logNotify(String log, String where) {
        //校验补丁包失败
        AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_APPLIED_FAILE,"java-校验失败",log);
        isFaile = true;
    }

    @Override
    public void exceptionNotify(Throwable throwable, String where) {
        //应用补丁包出现异常
        LogUtil.e("=====hotfix=====","应用补丁包出现异常" );
        AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_APPLIED_FAILE,"java-应用失败",throwable.getMessage());
        isFaile = true;
    }
}