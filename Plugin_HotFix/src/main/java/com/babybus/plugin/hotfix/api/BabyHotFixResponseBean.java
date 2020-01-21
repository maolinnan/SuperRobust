package com.babybus.plugin.hotfix.api;

import java.util.List;

/**
 * time:2018/5/4 上午11:41
 * author:maolinnan
 * desc:服务端请求返回结果的Javabean
 **/

public class BabyHotFixResponseBean {
    private String status;
    private String info;
    private List<HotFixBean> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public List<HotFixBean> getData() {
        return data;
    }

    public void setData(List<HotFixBean> data) {
        this.data = data;
    }

    public static class HotFixBean{
        private String patchHash;//补丁包md5
        private String downloadUrl;//补丁包下载地址
        private int patchType;//补丁类型

        public String getPatchHash() {
            return patchHash;
        }

        public void setPatchHash(String patchHash) {
            this.patchHash = patchHash;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public int getPatchType() {
            return patchType;
        }

        public void setPatchType(int patchType) {
            this.patchType = patchType;
        }
    }
}
