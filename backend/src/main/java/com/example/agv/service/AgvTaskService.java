package com.example.agv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvTaskMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgvTaskService {

    @Resource
    private AgvTaskMapper agvTaskMapper;

    public List<AgvTask> listTask(String taskCode, String creator, String executor, String taskStatus) {
        QueryWrapper<AgvTask> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0);

        if (taskCode != null && !taskCode.trim().isEmpty()) {
            wrapper.like("task_code", taskCode);
        }
        if (creator != null && !creator.trim().isEmpty()) {
            wrapper.like("creator", creator);
        }
        if (executor != null && !executor.trim().isEmpty()) {
            wrapper.like("executor", executor);
        }
        if (taskStatus != null && !taskStatus.trim().isEmpty()) {
            wrapper.eq("task_status", taskStatus);
        }

        wrapper.orderByDesc("create_time");
        return agvTaskMapper.selectList(wrapper);
    }

    public AgvTask getTask(Long id) {
        return agvTaskMapper.selectById(id);
    }

    public AgvTask addTask(AgvTask task) {
        task.setTaskStatus("待巡视");
        task.setUploaded(0);
        task.setDeleteFlag(0);
        task.setCreateTime(LocalDateTime.now());
        agvTaskMapper.insert(task);
        return task;
    }

    public AgvTask startTask(Long id) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null) {
            return null;
        }
        if (!"待巡视".equals(task.getTaskStatus())) {
            return null;
        }
        task.setTaskStatus("巡视中");
        task.setExecTime(LocalDateTime.now());
        agvTaskMapper.updateById(task);
        return task;
    }

    public AgvTask endTask(Long id, boolean isAbort) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null) {
            return null;
        }
        task.setTaskStatus("待上传");
        task.setEndTime(LocalDateTime.now());
        if (isAbort) {
            task.setRemark("任务被终止");
        }
        agvTaskMapper.updateById(task);
        return task;
    }

    public AgvTask uploadTask(Long id) {
        AgvTask task = agvTaskMapper.selectById(id);
        if (task == null) {
            return null;
        }
        if (!"待上传".equals(task.getTaskStatus())) {
            return null;
        }
        task.setTaskStatus("已完成");
        task.setUploaded(1);
        agvTaskMapper.updateById(task);
        return task;
    }
}
