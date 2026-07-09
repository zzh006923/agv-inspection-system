package com.example.agv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.common.AjaxResult;
import com.example.agv.common.TableDataInfo;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.mapper.AgvFlawMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 故障/缺陷管理控制器
 *
 * 对应任务详情页：故障历史、故障图片、故障确认、误报标记、备注填写。
 */
@RestController
@RequestMapping("/agv/flaw")
@CrossOrigin
public class AgvFlawController {

    @Resource
    private AgvFlawMapper agvFlawMapper;

    /**
     * 获取缺陷列表
     * 支持按任务、缺陷类型、名称、等级、来源、确认状态、上传状态过滤。
     */
    @GetMapping("/list")
    public TableDataInfo listFlaw(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String flawType,
            @RequestParam(required = false) String flawName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer confirmed,
            @RequestParam(required = false) Integer uploaded,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        QueryWrapper<AgvFlaw> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0);

        if (taskId != null) {
            wrapper.eq("task_id", taskId);
        }
        if (flawType != null && !flawType.trim().isEmpty()) {
            wrapper.like("flaw_type", flawType.trim());
        }
        if (flawName != null && !flawName.trim().isEmpty()) {
            wrapper.like("flaw_name", flawName.trim());
        }
        if (level != null && !level.trim().isEmpty()) {
            wrapper.eq("level", level.trim());
        }
        if (source != null && !source.trim().isEmpty()) {
            wrapper.eq("source", source.trim());
        }
        if (confirmed != null) {
            wrapper.eq("confirmed", confirmed);
        }
        if (uploaded != null) {
            wrapper.eq("uploaded", uploaded);
        }

        wrapper.orderByAsc("flaw_distance").orderByDesc("create_time");
        List<AgvFlaw> list = agvFlawMapper.selectList(wrapper);

        int total = list.size();
        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<AgvFlaw> rows = fromIndex >= total ? new java.util.ArrayList<>() : list.subList(fromIndex, toIndex);

        return TableDataInfo.success(rows, (long) total);
    }

    /**
     * 获取缺陷详情
     */
    @GetMapping("/{id}")
    public AjaxResult getFlaw(@PathVariable Long id) {
        AgvFlaw flaw = agvFlawMapper.selectById(id);
        if (flaw == null || Integer.valueOf(1).equals(flaw.getDeleteFlag())) {
            return AjaxResult.error("故障/缺陷不存在");
        }
        return AjaxResult.success(flaw);
    }

    /**
     * 新增缺陷。用于人工录入、图像识别结果写入，也可用于 AIoT 异常自动生成记录。
     */
    @PostMapping
    public AjaxResult addFlaw(@RequestBody AgvFlaw flaw) {
        if (flaw.getTaskId() == null) {
            return AjaxResult.error("所属任务ID不能为空");
        }
        if (flaw.getRound() == null) flaw.setRound(1);
        if (flaw.getShown() == null) flaw.setShown(0);
        if (flaw.getConfirmed() == null) flaw.setConfirmed(0);
        if (flaw.getUploaded() == null) flaw.setUploaded(0);
        if (flaw.getCountNum() == null) flaw.setCountNum(1);
        if (flaw.getDeleteFlag() == null) flaw.setDeleteFlag(0);
        if (flaw.getCreateTime() == null) flaw.setCreateTime(LocalDateTime.now());
        agvFlawMapper.insert(flaw);
        return AjaxResult.success(flaw);
    }

    /**
     * 更新缺陷。
     * 前端可通过该接口完成：确认故障、标记误报、填写备注、修改等级等操作。
     */
    @PutMapping
    public AjaxResult updateFlaw(@RequestBody AgvFlaw flaw) {
        if (flaw.getId() == null) {
            return AjaxResult.error("故障ID不能为空");
        }
        AgvFlaw existing = agvFlawMapper.selectById(flaw.getId());
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleteFlag())) {
            return AjaxResult.error("故障/缺陷不存在");
        }
        agvFlawMapper.updateById(flaw);
        return AjaxResult.success(agvFlawMapper.selectById(flaw.getId()));
    }

    /**
     * 删除缺陷，采用逻辑删除。
     */
    @DeleteMapping("/{id}")
    public AjaxResult delFlaw(@PathVariable Long id) {
        int rows = agvFlawMapper.deleteById(id);
        if (rows == 0) {
            return AjaxResult.error("故障/缺陷不存在");
        }
        return AjaxResult.success(null);
    }

    /**
     * 任务巡视页轮询实时故障。
     * 返回当前任务尚未弹窗提示的故障/异常记录，并把 shown 改为 1，避免重复弹窗。
     */
    @GetMapping("/live/{taskId}")
    public AjaxResult liveInfo(@PathVariable Long taskId) {
        QueryWrapper<AgvFlaw> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0)
                .eq("task_id", taskId)
                .eq("shown", 0)
                .orderByAsc("flaw_distance")
                .orderByAsc("create_time");

        List<AgvFlaw> list = agvFlawMapper.selectList(wrapper);
        for (AgvFlaw flaw : list) {
            flaw.setShown(1);
            agvFlawMapper.updateById(flaw);
        }
        return AjaxResult.success(list);
    }

    /**
     * 检查某个任务下的故障是否全部确认。
     * 用于上传前检查。
     */
    @GetMapping("/check/{taskId}")
    public AjaxResult checkAllConfirmed(@PathVariable Long taskId) {
        QueryWrapper<AgvFlaw> allWrapper = new QueryWrapper<>();
        allWrapper.eq("delete_flag", 0).eq("task_id", taskId);
        Long total = agvFlawMapper.selectCount(allWrapper);

        QueryWrapper<AgvFlaw> unconfirmedWrapper = new QueryWrapper<>();
        unconfirmedWrapper.eq("delete_flag", 0)
                .eq("task_id", taskId)
                .and(w -> w.isNull("confirmed").or().ne("confirmed", 1));
        Long unconfirmed = agvFlawMapper.selectCount(unconfirmedWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("total", total);
        data.put("unconfirmed", unconfirmed);
        data.put("allConfirmed", unconfirmed == 0);
        return AjaxResult.success(data);
    }
}
