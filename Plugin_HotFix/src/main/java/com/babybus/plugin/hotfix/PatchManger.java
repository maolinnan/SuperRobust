package com.babybus.plugin.hotfix;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import com.babybus.analytics.AiolosAnalytics;
import com.babybus.app.App;
import com.babybus.app.C;
import com.babybus.managers.ThreadManager;
import com.babybus.bean.BaseDownloadInfo;
import com.babybus.listeners.DownloadListener;
import com.babybus.plugin.hotfix.api.BabyHotFixManager;
import com.babybus.plugin.hotfix.api.BabyHotFixResponseBean;
import com.babybus.plugin.hotfix.robust.HotFixRobustCallBack;
import com.babybus.plugin.hotfix.robust.PatchManipulateImp;
import com.babybus.plugins.pao.DownloadManagerPao;
import com.babybus.utils.BBFileUtil;
import com.babybus.utils.SpUtil;
import com.babybus.utils.ZipUtil;
import com.meituan.robust.Patch;
import com.meituan.robust.PatchExecutor;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * time:2018/11/26 上午9:27
 * author:maolinnan
 * desc:补丁管理类
 */

public class PatchManger {
    private static final int TYPE_APP_PATCH = 1;//应用层补丁
    private static final int TYPE_GAME2D_PATCH = 2;//2d游戏层补丁
    private static final int TYPE_GAME3D_PATCH = 3;//3d游戏层补丁

    //反射补丁中的列表类
    private final static String PATCHES_INFO_IMPL_CLASS = "com.babybus.plugin.hotfix.patch.PatchesInfoImpl";
    //debug补丁目录路径
    public final static String DEBUG_PATCH_PATH = C.Path.PUBLIC_PATH+"/patch/";
    //release补丁目录路径
    public final static String RELEASE_PATCH_PATH = App.get().getFilesDir().getPath() + "/patch/";
    //补丁文件名
    private final static String PATCH_NAME = "patch.jar";
    //补丁MD5值配置
    public final static String PATCH_MD5_CONFIG = "patch_md5_config";
    //当前生效的补丁
    public Patch validPatch;

    //游戏层补丁
    public final static String GAME2D_DEBUG_PATCH_PATH = C.Path.PUBLIC_PATH+"/patch/2d/";
    public final static String GAME3D_DEBUG_PATCH_PATH = C.Path.PUBLIC_PATH+"/patch/3d/";
    public final static String GAME2D_RELEASE_PATCH_PATH = App.get().getFilesDir().getPath()+"/patch/2d/";
    public final static String GAME3D_RELEASE_PATCH_PATH = App.get().getFilesDir().getPath()+"/patch/3d/";
    public final static String PATCH_GAME2D_MD5_CONFIG = "patch_game2d_md5_config";
    public final static String PATCH_GAME3D_MD5_CONFIG = "patch_game3d_md5_config";


    //单例
    private static PatchManger patchManger = new PatchManger();
    private PatchManger(){}
    public static PatchManger get(){
        return patchManger;
    }

    /**
     * 获取补丁，会从两个地方找，一个是sd卡，一个是私有目录，其中SD卡的是用来我们测试补丁
     * @return
     */
    public Patch getPatch(){
        String patchPath = DEBUG_PATCH_PATH;
        File file;
        try {
            file = new File(patchPath);
            if (file.exists()){//sd卡存在补丁文件，说明是测试模式，优先应用该补丁
                File[] childFiles = file.listFiles();
                for(File childFile : childFiles){
                    if (childFile.getName().endsWith(".jar")){//sd卡有补丁包，进行解压操作
                        String zipPath = childFile.getAbsolutePath();
                        String patchDirName = childFile.getName().substring(0,childFile.getName().length()-4);
                        String outputPath = patchPath + patchDirName;
                        Patch patch = new Patch();
                        patch.setName(patchDirName);
                        patch.setLocalPath(outputPath + "/"+PATCH_NAME);
                        patch.setMd5(patchDirName);
                        patch.setPatchesInfoImplClassFullName(PATCHES_INFO_IMPL_CLASS);
                        if (BBFileUtil.checkFile(outputPath + "/"+PATCH_NAME)){
                            return patch;
                        }
                        ZipUtil.unzip(zipPath, outputPath);
                        return patch;
                    }
                }
            }
        }catch (Exception e){//某些手机会因为权限问题崩溃，故捕获。该功能本来就是用来给内部测试用的。
            e.printStackTrace();
        }
        String md5Value = SpUtil.getString(PatchManger.PATCH_MD5_CONFIG,"");
        if (TextUtils.isEmpty(md5Value)){
            return null;
        }
        patchPath = RELEASE_PATCH_PATH + md5Value + "/" + PATCH_NAME;
        file = new File(patchPath);
        if (file.exists()){//私有目录存在补丁文件
            Patch patch = new Patch();
            patch.setName(md5Value);
            patch.setLocalPath(patchPath);
            patch.setMd5(md5Value);
            patch.setPatchesInfoImplClassFullName(PATCHES_INFO_IMPL_CLASS);
            return patch;
        }
        return null;
    }

    /**
     * 获取当前补丁中资源补丁路径
     * @return
     */
    public String getPatchResPath(){
        if (validPatch == null){
            return "";
        }
        if (validPatch.getLocalPath() != null && validPatch.getLocalPath().startsWith(DEBUG_PATCH_PATH)){
            return DEBUG_PATCH_PATH + validPatch.getName() + "/res/";
        }
        return RELEASE_PATCH_PATH + validPatch.getName() + "/res/";
    }


    /**
     * 开始打补丁
     */
    public void runRobust(){
        if (validPatch != null){//当前已经有生效的补丁啦，不再打了
            return;
        }
        if (App.get().getPackageName().equals(getCurrentProcessName())) {//主进程才执行热修复
            new PatchExecutor(App.get(), new PatchManipulateImp(), new HotFixRobustCallBack()).start();
        }
    }

    /**
     * 获取当前进程名
     */
    private String getCurrentProcessName() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) App.get().getSystemService
                (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

    /**
     * 开始请求服务端热修复信息
     */
    public void startRequestServiceHotFixInfo(){
        //接口请求参数
        String version2d = "";
        String version3d = "";
        if (App.get().u3d){
            version3d = App.getAppInfo().getVersionCode()+"";
        }else{
            version2d = App.getAppInfo().getVersionCode()+"";
        }
        BabyHotFixManager.get().getHotFixList(BabyHotFixManager.getHotFixRequestUrl(),version2d,version3d).enqueue(new Callback<BabyHotFixResponseBean>() {
            @Override
            public void onResponse(Call<BabyHotFixResponseBean> call, Response<BabyHotFixResponseBean> response) {
                if (response == null){
                    return;
                }
                BabyHotFixResponseBean responseBean = response.body();
                if (responseBean == null){
                    return;
                }
                if (!"1".equals(responseBean.getStatus())){//状态不对返回
                    return;
                }
                List<BabyHotFixResponseBean.HotFixBean> hotFixList = responseBean.getData();
                if (hotFixList == null || hotFixList.size() == 0){
                    return;
                }
                /**下载并解压补丁，只认第一个补丁*/
                //执行应用层修复补丁
                for(final BabyHotFixResponseBean.HotFixBean bean : hotFixList){
                    if (bean.getPatchType() == TYPE_APP_PATCH){//应用层补丁
                        ThreadManager.getInstance().run(new Runnable() {
                            @Override
                            public void run() {
                                disposeHotFixInfo(bean);
                            }
                        });
                        break;
                    }
                }
                //下载解压2D层补丁
                for(final BabyHotFixResponseBean.HotFixBean bean : hotFixList){
                    if (bean.getPatchType() == TYPE_GAME2D_PATCH){
                        ThreadManager.getInstance().run(new Runnable() {
                            @Override
                            public void run() {
                                disposeGameHotFixInfo(bean);
                            }
                        });
                        break;
                    }
                }
                //下载解压3D层补丁
                for(final BabyHotFixResponseBean.HotFixBean bean : hotFixList){
                    if (bean.getPatchType() == TYPE_GAME3D_PATCH){
                        ThreadManager.getInstance().run(new Runnable() {
                            @Override
                            public void run() {
                                disposeGameHotFixInfo(bean);
                            }
                        });
                        break;
                    }
                }
            }
            @Override
            public void onFailure(Call<BabyHotFixResponseBean> call, Throwable t) {

            }
        });
    }

    /**
     * 处理游戏层补丁数据
     * @param bean
     */
    private void disposeGameHotFixInfo(final BabyHotFixResponseBean.HotFixBean bean){
        if (bean == null || TextUtils.isEmpty(bean.getPatchHash()) || TextUtils.isEmpty(bean.getDownloadUrl())){
            return;
        }
        //判断补丁是否存在
        String patchPath = "";
        if (bean.getPatchType() == TYPE_GAME2D_PATCH){//2d补丁
            patchPath = GAME2D_RELEASE_PATCH_PATH + bean.getPatchHash();
            File file = new File(patchPath);
            if (file.exists() && file.listFiles().length > 0){
                SpUtil.putString(PatchManger.PATCH_GAME2D_MD5_CONFIG,bean.getPatchHash());
                return;
            }
        }else{//3d补丁
            patchPath = GAME3D_RELEASE_PATCH_PATH + bean.getPatchHash();
            File file = new File(patchPath);
            if (file.exists() && file.listFiles().length > 0){
                SpUtil.putString(PatchManger.PATCH_GAME3D_MD5_CONFIG,bean.getPatchHash());
                return;
            }
        }

        //删除旧数据
        if (bean.getPatchType() == TYPE_GAME2D_PATCH) {//2d补丁
            patchPath = GAME2D_RELEASE_PATCH_PATH;
        }else{//3d补丁
            patchPath = GAME3D_RELEASE_PATCH_PATH;
        }
        File file = new File(patchPath);
        if (!file.exists()){
            file.mkdirs();
        }else{
            BBFileUtil.removeDirectory(patchPath + bean.getPatchHash());
        }
        final String downloadPatchPath = patchPath;
        //下载并解压游戏层补丁
        String filePath = DownloadManagerPao.getFilePath(bean.getDownloadUrl(),DownloadManagerPao.TYPE_ZIP,bean.getPatchHash(),downloadPatchPath);
        DownloadManagerPao.startSimpleDownload(bean.getDownloadUrl(), filePath, false, new DownloadListener() {
            @Override
            public void onStart(BaseDownloadInfo downloadInfo) {
                if (bean.getPatchType() == TYPE_GAME2D_PATCH){
                    AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"lua-开始",bean.getPatchHash());
                }else{
                    AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"u3d-开始",bean.getPatchHash());
                }
            }

            @Override
            public void onDownloading(BaseDownloadInfo downloadInfo) {

            }

            @Override
            public void onPause(BaseDownloadInfo downloadInfo) {

            }

            @Override
            public void onCompleted(BaseDownloadInfo downloadInfo) {
                if (bean.getPatchType() == TYPE_GAME2D_PATCH){
                    AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"lua-成功",bean.getPatchHash());
                }else{
                    AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"u3d-成功",bean.getPatchHash());
                }

                ThreadManager.getInstance().run(new Runnable() {
                    @Override
                    public void run() {
                        String zipPath = downloadPatchPath + bean.getPatchHash() + DownloadManagerPao.TYPE_ZIP;
                        String outpath = downloadPatchPath + bean.getPatchHash();
                        try {
                            //判断下载文件的md5
                            File file = new File(zipPath);
                            if (!file.exists()){
                                return;
                            }else {
                                String zipMd5 = BBFileUtil.getFileMD5(file);
                                if (!zipMd5.equals(bean.getPatchHash())){
                                    return;
                                }
                            }
                            ZipUtil.unzip(zipPath, outpath);
                            if (bean.getPatchType() == TYPE_GAME2D_PATCH){
                                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_UNZIP,"lua-成功",bean.getPatchHash());
                            }else{
                                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_UNZIP,"u3d-成功",bean.getPatchHash());
                            }
                            //解压完成
                            BBFileUtil.deleteFile(zipPath);//删除掉zip包
                            //保存补丁md5值
                            if (bean.getPatchType() == TYPE_GAME2D_PATCH) {//2d补丁
                                SpUtil.putString(PatchManger.PATCH_GAME2D_MD5_CONFIG,bean.getPatchHash());
                            }else{
                                SpUtil.putString(PatchManger.PATCH_GAME3D_MD5_CONFIG,bean.getPatchHash());
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            //解压失败
                            if (bean.getPatchType() == TYPE_GAME2D_PATCH){
                                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_UNZIP,"lua-失败",bean.getPatchHash());
                            }else{
                                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_UNZIP,"u3d-失败",bean.getPatchHash());
                            }

                        }
                    }
                });
            }

            @Override
            public void onFailed(BaseDownloadInfo downloadInfo, int errorCode, String message) {
                if (bean.getPatchType() == TYPE_GAME2D_PATCH){
                    AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"lua-失败",bean.getPatchHash());
                }else{
                    AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"u3d-失败",bean.getPatchHash());
                }
            }
        });
    }


    /**
     * 处理服务端获取的数据
     * @param bean
     */
    private void disposeHotFixInfo(final BabyHotFixResponseBean.HotFixBean bean){
        if (bean == null || TextUtils.isEmpty(bean.getPatchHash()) || TextUtils.isEmpty(bean.getDownloadUrl())){
            return;
        }
        //判断本地是否有该补丁
        String patchPath = RELEASE_PATCH_PATH + bean.getPatchHash() + "/" + PATCH_NAME;
        if (BBFileUtil.checkFile(patchPath)){//文件存在
            //保存补丁md5值
            SpUtil.putString(PatchManger.PATCH_MD5_CONFIG,bean.getPatchHash());
            if (validPatch == null){//没有应用过补丁
                runRobust();
            }
            return;
        }
        //本地没补丁，去下载
        File file = new File(RELEASE_PATCH_PATH);
        if (!file.exists()){
            file.mkdirs();
        }else{
            BBFileUtil.removeDirectory(RELEASE_PATCH_PATH + bean.getPatchHash());
        }
        String filePath = DownloadManagerPao.getFilePath(bean.getDownloadUrl(),DownloadManagerPao.TYPE_ZIP,bean.getPatchHash(), RELEASE_PATCH_PATH);
        DownloadManagerPao.startSimpleDownload(bean.getDownloadUrl(), filePath, false, new DownloadListener() {
            @Override
            public void onStart(BaseDownloadInfo downloadInfo) {
                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"java-开始",bean.getPatchHash());
            }

            @Override
            public void onDownloading(BaseDownloadInfo downloadInfo) {

            }

            @Override
            public void onPause(BaseDownloadInfo downloadInfo) {

            }

            @Override
            public void onCompleted(BaseDownloadInfo downloadInfo) {
                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"java-成功",bean.getPatchHash());
                ThreadManager.getInstance().run(new Runnable() {
                    @Override
                    public void run() {
                        String zipPath = RELEASE_PATCH_PATH + bean.getPatchHash() + DownloadManagerPao.TYPE_ZIP;
                        String outpath = RELEASE_PATCH_PATH + bean.getPatchHash();
                        try {
                            //判断下载文件的md5
                            File file = new File(zipPath);
                            if (!file.exists()){
                                return;
                            }else {
                                String zipMd5 = BBFileUtil.getFileMD5(file);
                                if (!zipMd5.equals(bean.getPatchHash())){
                                    return;
                                }
                            }
                            ZipUtil.unzip(zipPath, outpath);
                            AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_UNZIP,"java-成功",bean.getPatchHash());
                            //解压完成
                            BBFileUtil.deleteFile(zipPath);//删除掉zip包
                            //保存补丁md5值
                            SpUtil.putString(PatchManger.PATCH_MD5_CONFIG,bean.getPatchHash());
                            if (validPatch == null){//没有应用过补丁
                                runRobust();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            //解压失败
                            AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_UNZIP,"java-失败",bean.getPatchHash());
                        }
                    }
                });
            }

            @Override
            public void onFailed(BaseDownloadInfo downloadInfo, int errorCode, String message) {
                AiolosAnalytics.get().recordEvent(HotFixAiolosKey.HOTFIX_DOWNLOAD,"java-失败",bean.getPatchHash());
            }
        });
    }
}
