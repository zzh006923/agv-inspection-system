package com.example.agv.common;

/**
 * 分页列表统一返回结构。
 *
 * 与老师接口文档中的 TableDataInfo 保持一致：
 * code / msg / total / rows 位于返回 JSON 顶层。
 */
public class TableDataInfo {

    private Integer code;
    private String msg;
    private Long total;
    private Object rows;

    public static TableDataInfo success(Object rows, Long total) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(200);
        result.setMsg("查询成功");
        result.setRows(rows);
        result.setTotal(total == null ? 0L : total);
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

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Object getRows() {
        return rows;
    }

    public void setRows(Object rows) {
        this.rows = rows;
    }
}
