package com.example.agv.common;

public class AjaxResult {

    private Integer code;
    private String msg;
    private Object data;

    public static AjaxResult success(Object data) {
        AjaxResult result = new AjaxResult();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    public static AjaxResult error(String msg) {
        AjaxResult result = new AjaxResult();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
