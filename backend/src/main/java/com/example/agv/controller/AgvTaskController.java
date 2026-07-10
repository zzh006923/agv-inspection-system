package com.example.agv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.common.AjaxResult;
import com.example.agv.common.TableDataInfo;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvTask;
import com.example.agv.entity.AgvUploadRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvTaskMapper;
import com.example.agv.mapper.AgvUploadRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.service.CloudUploadService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agv/task")
@CrossOrigin
public class AgvTaskController {

    @Resource
    private AgvTaskMapper agvTaskMapper;

    @Resource
    private AgvFlawMapper agvFlawMapper;

    @Resource
    private AgvUploadRecordMapper agvUploadRecordMapper;

    @Resource
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Resource
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @Resource
    private CloudUploadService cloudUploadService;

    /**
     * 查询任务列表
     */
    @GetMapping("/list")
    public TableDataInfo listTask(
            @RequestParam(required = false) String taskCode,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false) String executor,
            @RequestParam(required = false) String taskStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        QueryWrapper<AgvTask> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0);

        if (taskCode != null && !taskCode.trim().isEmpty()) {
            wrapper.like("task_code", taskCode.trim());
        }
        if (creator != null && !creator.trim().isEmpty()) {
            wrapper.like("creator", creator.trim());
        }
        if (executor != null && !executor.trim().isEmpty()) {
            wrapper.like("executor", executor.trim());
        }
        if (taskStatus != null && !taskStatus.trim().isEmpty()) {
            wrapper.eq("task_status", taskStatus.trim());
        }

        wrapper.orderByDesc("create_time");
        List<AgvTask> list = agvTaskMapper.selectList(wrapper);

        int total = list.size();
        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<AgvTask> rows = fromIndex >= total ? new java.util.ArrayList<>() : list.subList(fromIndex, toIndex);

        return TableDataInfo.success(rows, (long) total);
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/{id}")
    public AjaxResult getTask(@PathVariable Long id) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null || Integer.valueOf(1).equals(task.getDeleteFlag())) {
            return AjaxResult.error("任务不存在");
        }
        return AjaxResult.success(task);
    }

    /**
     * 获取下一个任务编号（前端新增弹窗时调用）
     * 格式: TASK + yyyyMMdd + 4位序号
     */
    @GetMapping("/next-code")
    public AjaxResult getNextCode() {
        return AjaxResult.success(generateNextTaskCode());
    }

    /**
     * 新增任务（自动生成编号）
     */
    @PostMapping
    public AjaxResult addTask(@RequestBody AgvTask task) {
        if (task.getTaskCode() == null || task.getTaskCode().trim().isEmpty()) {
            task.setTaskCode(generateNextTaskCode());
        }
        task.setTaskStatus("待巡视");
        task.setUploaded(0);
        task.setDeleteFlag(0);
        task.setCreateTime(LocalDateTime.now());
        if (task.getRound() == null) {
            task.setRound(1);
        }
        agvTaskMapper.insert(task);
        return AjaxResult.success(task);
    }

    /**
     * 启动任务
     */
    @PostMapping("/start/{id}")
    public AjaxResult startTask(@PathVariable Long id) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null || Integer.valueOf(1).equals(task.getDeleteFlag())) {
            return AjaxResult.error("任务不存在");
        }
        if (!"待巡视".equals(task.getTaskStatus())) {
            return AjaxResult.error("只有待巡视任务可以启动");
        }

        task.setTaskStatus("巡视中");
        task.setExecTime(LocalDateTime.now());
        agvTaskMapper.updateById(task);
        return AjaxResult.success(task);
    }

    /**
     * 结束任务
     */
    @PostMapping("/end/{id}")
    public AjaxResult endTask(@PathVariable Long id,
                              @RequestParam(defaultValue = "false") Boolean isAbort) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null || Integer.valueOf(1).equals(task.getDeleteFlag())) {
            return AjaxResult.error("任务不存在");
        }
        if (!"巡视中".equals(task.getTaskStatus())) {
            return AjaxResult.error("只有巡视中的任务可以结束");
        }

        task.setTaskStatus("待上传");
        task.setEndTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(isAbort)) {
            task.setRemark("任务被终止");
        }
        agvTaskMapper.updateById(task);
        return AjaxResult.success(task);
    }

    /**
     * 上传前检查：返回任务、故障、上传记录以及是否允许上传。
     */
    @GetMapping("/preupload/{id}")
    public AjaxResult preUploadTask(@PathVariable Long id) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null || Integer.valueOf(1).equals(task.getDeleteFlag())) {
            return AjaxResult.error("任务不存在");
        }

        List<AgvFlaw> flaws = selectFlawsByTask(id);
        List<AgvUploadRecord> uploadRecords = selectUploadRecordsByTask(id);
        long unconfirmedCount = flaws.stream()
                .filter(flaw -> !Integer.valueOf(1).equals(flaw.getConfirmed()))
                .count();
        long notUploadedFlawCount = flaws.stream()
                .filter(flaw -> !Integer.valueOf(1).equals(flaw.getUploaded()))
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("taskId", id);
        summary.put("taskStatus", task.getTaskStatus());
        summary.put("flawCount", flaws.size());
        summary.put("unconfirmedCount", unconfirmedCount);
        summary.put("notUploadedFlawCount", notUploadedFlawCount);
        summary.put("canUpload", "待上传".equals(task.getTaskStatus()) && unconfirmedCount == 0);

        Map<String, Object> data = new HashMap<>();
        data.put("task", task);
        data.put("flaws", flaws);
        data.put("uploadRecords", uploadRecords);
        data.put("summary", summary);
        return AjaxResult.success(data);
    }

    /**
     * 上传任务。
     * 1. 检查任务状态与故障确认情况；
     * 2. 将任务改为已完成；
     * 3. 将任务下故障改为已上传；
     * 4. 写入/更新上传记录。
     */
    @PostMapping("/upload/{id}")
    public AjaxResult uploadTask(@PathVariable Long id) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null || Integer.valueOf(1).equals(task.getDeleteFlag())) {
            return AjaxResult.error("任务不存在");
        }
        if (!"待上传".equals(task.getTaskStatus())) {
            return AjaxResult.error("只有待上传任务可以上传");
        }

        List<AgvFlaw> flaws = selectFlawsByTask(id);
        long unconfirmedCount = flaws.stream()
                .filter(flaw -> !Integer.valueOf(1).equals(flaw.getConfirmed()))
                .count();
        if (unconfirmedCount > 0) {
            return AjaxResult.error("存在未确认故障/异常，请先确认或标记误报后再上传");
        }

        LocalDateTime now = LocalDateTime.now();

        List<AgvUploadRecord> recordsBeforeUpload = selectUploadRecordsByTask(id);
        List<AgvSensorRecord> sensorRecords = selectSensorRecordsByTask(id);
        List<AgvIotActionRecord> iotActionRecords = selectIotActionRecordsByTask(id);
        CloudUploadService.UploadResult uploadResult;
        try {
            uploadResult = cloudUploadService.uploadTaskData(task, flaws, recordsBeforeUpload, sensorRecords, iotActionRecords);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }

        task.setTaskStatus("已完成");
        task.setUploaded(1);
        if (task.getEndTime() == null) {
            task.setEndTime(now);
        }
        agvTaskMapper.updateById(task);

        for (AgvFlaw flaw : flaws) {
            flaw.setUploaded(1);
            agvFlawMapper.updateById(flaw);
        }

        List<AgvUploadRecord> records = recordsBeforeUpload;
        if (records.isEmpty()) {
            insertUploadRecord(id, task.getTaskCode(), "任务", "已上传", now, "任务信息上传成功");
            for (AgvFlaw flaw : flaws) {
                insertUploadRecord(id, flaw.getFlawName(), "故障", "已上传", now, "故障/异常记录上传成功");
            }
        } else {
            for (AgvUploadRecord record : records) {
                record.setStatus("已上传");
                record.setProgress(100);
                record.setUploadResult(uploadResult.getMessage());
                record.setUploadTime(now);
                agvUploadRecordMapper.updateById(record);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("task", agvTaskMapper.selectById(id));
        data.put("flaws", selectFlawsByTask(id));
        data.put("uploadRecords", selectUploadRecordsByTask(id));
        data.put("sensorRecords", sensorRecords);
        data.put("iotActionRecords", iotActionRecords);
        data.put("uploadResult", uploadResult);
        return AjaxResult.success(data);
    }

    /**
     * 更新任务
     */
    @PutMapping
    public AjaxResult updateTask(@RequestBody AgvTask task) {
        if (task.getId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }
        AgvTask existing = agvTaskMapper.selectById(task.getId());
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleteFlag())) {
            return AjaxResult.error("任务不存在");
        }
        if (!"待巡视".equals(existing.getTaskStatus())) {
            return AjaxResult.error("只有待巡视任务可以修改");
        }
        agvTaskMapper.updateById(task);
        return AjaxResult.success(agvTaskMapper.selectById(task.getId()));
    }

    /**
     * 删除任务（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public AjaxResult delTask(@PathVariable Long id) {
    	AgvTask existing = agvTaskMapper.selectById(id);
    	if (existing == null || Integer.valueOf(1).equals(existing.getDeleteFlag())) {
    	    return AjaxResult.error("任务不存在");
    	}
    	if (!"待巡视".equals(existing.getTaskStatus())) {
    	    return AjaxResult.error("只有待巡视任务可以删除");
    	}
    	int rows = agvTaskMapper.deleteById(id);
        if (rows == 0) {
            return AjaxResult.error("任务不存在");
        }
        return AjaxResult.success(null);
    }

    // ─── 私有 helper ─────────────────────────────────────────

    /**
     * 生成下一个任务编号：TASK + yyyyMMdd + 4位序号
     */
    private String generateNextTaskCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TASK" + today;

        QueryWrapper<AgvTask> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0);
        wrapper.likeRight("task_code", prefix);
        wrapper.orderByDesc("task_code");
        wrapper.last("LIMIT 1");

        AgvTask last = agvTaskMapper.selectOne(wrapper);

        int seq = 1;
        if (last != null && last.getTaskCode() != null && last.getTaskCode().length() > prefix.length()) {
            String seqStr = last.getTaskCode().substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {
                // 非数字后缀则从头开始
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private List<AgvFlaw> selectFlawsByTask(Long taskId) {
        QueryWrapper<AgvFlaw> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0)
                .eq("task_id", taskId)
                .orderByAsc("flaw_distance")
                .orderByAsc("create_time");
        return agvFlawMapper.selectList(wrapper);
    }

    private List<AgvUploadRecord> selectUploadRecordsByTask(Long taskId) {
        QueryWrapper<AgvUploadRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0)
                .eq("task_id", taskId)
                .orderByAsc("id");
        return agvUploadRecordMapper.selectList(wrapper);
    }

    private List<AgvSensorRecord> selectSensorRecordsByTask(Long taskId) {
        QueryWrapper<AgvSensorRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0)
                .eq("task_id", taskId)
                .orderByAsc("create_time");
        return agvSensorRecordMapper.selectList(wrapper);
    }

    private List<AgvIotActionRecord> selectIotActionRecordsByTask(Long taskId) {
        QueryWrapper<AgvIotActionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0)
                .eq("task_id", taskId)
                .orderByAsc("create_time");
        return agvIotActionRecordMapper.selectList(wrapper);
    }

    private void insertUploadRecord(Long taskId, String info, String type, String status,
                                    LocalDateTime uploadTime, String remark) {
        AgvUploadRecord record = new AgvUploadRecord();
        record.setTaskId(taskId);
        record.setInfo(info);
        record.setType(type);
        record.setStatus(status);
        record.setUploadTime(uploadTime);
        record.setProgress("已上传".equals(status) ? 100 : 0);
        record.setUploadResult(remark);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        record.setDeleteFlag(0);
        agvUploadRecordMapper.insert(record);
    }
}
