package com.babybus.plugin.hotfix.robust;

import android.content.Context;
import android.text.TextUtils;

import com.babybus.plugin.hotfix.PatchManger;
import com.babybus.utils.BBFileUtil;
import com.babybus.utils.Base64Utils;
import com.babybus.utils.LogUtil;
import com.babybus.utils.SpUtil;
import com.meituan.robust.Patch;
import com.meituan.robust.PatchManipulate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 补丁列表适配类
 */
public class PatchManipulateImp extends PatchManipulate {
    /***
     * 获取补丁列表
     * @param context
     * @return
     */
    @Override
    protected List<Patch> fetchPatchList(Context context) {
        List patches = new ArrayList<Patch>();
        Patch patch = PatchManger.get().getPatch();
        if (patch != null){
            patches.add(patch);
        }
        return patches;
    }

    /**
     * 校验补丁
     * @param context
     * @param patch
     * @return
     */
    @Override
    protected boolean verifyPatch(Context context, Patch patch) {
        if (patch == null) {
            return false;
        }
        boolean flag = extendVerifyPatch(patch);
        if (flag){
            LogUtil.e("=====hotfix=====","校验通过");
        }else{
            LogUtil.e("=====hotfix=====","校验失败");
            File file = new File(patch.getLocalPath());
            File parentFile = file.getParentFile();
            if (parentFile.exists()){
                BBFileUtil.removeFile(parentFile.getAbsolutePath()+"/info");
                BBFileUtil.removeFile(parentFile.getAbsolutePath()+"/patch.jar");
                BBFileUtil.removeDirectory(parentFile.getAbsolutePath()+"/res");
            }
        }
        return flag;
    }
    /**
     * 确保补丁存在
     * @param patch
     * @return
     */
    @Override
    protected boolean ensurePatchExist(Patch patch) {
        if (patch == null){
            return false;
        }
        if (TextUtils.isEmpty(patch.getLocalPath())){
            return false;
        }
        File file = new File(patch.getLocalPath());
        if (file.exists()){
            return true;
        }
        return false;
    }

    /**
     * 扩展校验
     * @param patch
     * @return
     */
    private boolean extendVerifyPatch(Patch patch){
        String md5Value = SpUtil.getString(PatchManger.PATCH_MD5_CONFIG,"");
        if (patch.getLocalPath().startsWith(PatchManger.DEBUG_PATCH_PATH)){//sd卡目录
            md5Value = patch.getMd5();
        }
        if (TextUtils.isEmpty(md5Value)){//补丁不存在就不该打补丁
            return false;
        }
        if (!md5Value.equals(patch.getMd5())){
            return false;
        }

        File file = new File(patch.getLocalPath());
        if (!file.exists()){
            return false;
        }
        String verifyFilePath = file.getParentFile().getAbsolutePath() + "/info";
        File verifyFile = new File(verifyFilePath);
        if (!verifyFile.exists()){
            return false;
        }
        /*验证所有文件，验证文件格式eg:
        *   {
        *        "patchMd5": "aaaaaaaaaaaaaaaa",
        *        "resList": [
        *            "aaa.png",
        *            "bbb.png"
        *        ]
        *    }
        *
        *    原始json串做个base64，然后加上0
        *
        */
        try {
            FileInputStream fileInputStream = new FileInputStream(verifyFile);
            byte[] buffer = new byte[fileInputStream.available()];
            fileInputStream.read(buffer);
            fileInputStream.close();
            String verifyInfo = new String(buffer);
            if (verifyInfo.length() <= 1 || !verifyInfo.endsWith("0")){
                return false;
            }
            //去除混淆串0
            verifyInfo = verifyInfo.substring(0,verifyInfo.length() - 1);
            verifyInfo = new String(Base64Utils.decode(verifyInfo),"utf-8");
            JSONObject jsonObject = new JSONObject(verifyInfo);
            String patchMd5 = jsonObject.getString("patchMd5");
            if (!BBFileUtil.getFileMD5(file).equals(patchMd5)){//补丁包Md5校验不通过
                return false;
            }
            JSONArray jsonArray = jsonObject.getJSONArray("resList");
            for(int i = 0 ; i < jsonArray.length() ; i ++){
                String resPath = (String)jsonArray.get(i);
                if (!BBFileUtil.checkFile(file.getParentFile().getAbsolutePath() + "/res/" + resPath)){
                    return false;
                }
            }
            //资源和补丁包都验证通过
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
