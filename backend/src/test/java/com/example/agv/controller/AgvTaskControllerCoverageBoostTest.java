package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.common.TableDataInfo;
import com.example.agv.entity.*;
import com.example.agv.mapper.*;
import com.example.agv.service.CloudUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvTaskControllerCoverageBoostTest {

    @Mock private AgvTaskMapper agvTaskMapper;
    @Mock private AgvFlawMapper agvFlawMapper;
    @Mock private AgvUploadRecordMapper agvUploadRecordMapper;
    @Mock private AgvSensorRecordMapper agvSensorRecordMapper;
    @Mock private AgvIotActionRecordMapper agvIotActionRecordMapper;
    @Mock private CloudUploadService cloudUploadService;

    @InjectMocks
    private AgvTaskController controller;

    @Test
    void listTaskShouldReturnEmptyPageWhenPageIsOutOfRangeAndApplyFilters() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        when(agvTaskMapper.selectList(any())).thenReturn(List.of(task));

        TableDataInfo result = controller.listTask(" TASK ", " 张三 ", " 李四 ", "待巡视", 5, 10);

        assertEquals(200, result.getCode());
        assertEquals(1L, result.getTotal());
        assertTrue(((List<?>) result.getRows()).isEmpty());
        verify(agvTaskMapper).selectList(any());
    }

    @Test
    void getTaskShouldReturnExistingTask() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setDeleteFlag(0);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AjaxResult result = controller.getTask(1L);

        assertEquals(200, result.getCode());
        assertSame(task, result.getData());
    }

    @Test
    void nextCodeShouldIncrementLatestNumericSuffixAndResetInvalidSuffix() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        AgvTask last = new AgvTask();
        last.setTaskCode("TASK" + today + "0009");
        when(agvTaskMapper.selectOne(any())).thenReturn(last);
        assertEquals("TASK" + today + "0010", controller.getNextCode().getData());

        AgvTask invalid = new AgvTask();
        invalid.setTaskCode("TASK" + today + "ABCD");
        when(agvTaskMapper.selectOne(any())).thenReturn(invalid);
        assertEquals("TASK" + today + "0001", controller.getNextCode().getData());
    }

    @Test
    void addTaskShouldKeepExistingCodeAndRound() {
        AgvTask task = new AgvTask();
        task.setTaskCode("CUSTOM-001");
        task.setRound(3);

        AjaxResult result = controller.addTask(task);

        assertEquals(200, result.getCode());
        assertEquals("CUSTOM-001", task.getTaskCode());
        assertEquals(3, task.getRound());
        assertEquals("待巡视", task.getTaskStatus());
        verify(agvTaskMapper).insert(task);
    }

    @Test
    void startTaskShouldHandleMissingDeletedAndSuccessBranches() {
        when(agvTaskMapper.selectById(1L)).thenReturn(null);
        assertEquals("任务不存在", controller.startTask(1L).getMsg());

        AgvTask deleted = new AgvTask();
        deleted.setDeleteFlag(1);
        when(agvTaskMapper.selectById(2L)).thenReturn(deleted);
        assertEquals("任务不存在", controller.startTask(2L).getMsg());

        AgvTask waiting = new AgvTask();
        waiting.setTaskStatus("待巡视");
        when(agvTaskMapper.selectById(3L)).thenReturn(waiting);
        AjaxResult success = controller.startTask(3L);
        assertEquals(200, success.getCode());
        assertEquals("巡视中", waiting.getTaskStatus());
        assertNotNull(waiting.getExecTime());
        verify(agvTaskMapper).updateById(waiting);
    }

    @Test
    void endTaskShouldHandleMissingSuccessAndAbortRemark() {
        when(agvTaskMapper.selectById(1L)).thenReturn(null);
        assertEquals("任务不存在", controller.endTask(1L, false).getMsg());

        AgvTask running = new AgvTask();
        running.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(2L)).thenReturn(running);
        AjaxResult result = controller.endTask(2L, true);
        assertEquals(200, result.getCode());
        assertEquals("待上传", running.getTaskStatus());
        assertEquals("任务被终止", running.getRemark());
        assertNotNull(running.getEndTime());
        verify(agvTaskMapper).updateById(running);
    }

    @Test
    void preUploadTaskShouldRejectMissingTaskAndBlockUnconfirmedFlaws() {
        when(agvTaskMapper.selectById(1L)).thenReturn(null);
        assertEquals("任务不存在", controller.preUploadTask(1L).getMsg());

        AgvTask task = new AgvTask();
        task.setTaskStatus("待上传");
        when(agvTaskMapper.selectById(2L)).thenReturn(task);
        AgvFlaw unconfirmed = new AgvFlaw();
        unconfirmed.setConfirmed(0);
        unconfirmed.setUploaded(0);
        AgvFlaw uploaded = new AgvFlaw();
        uploaded.setConfirmed(1);
        uploaded.setUploaded(1);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(unconfirmed, uploaded));
        when(agvUploadRecordMapper.selectList(any())).thenReturn(List.of(new AgvUploadRecord()));

        AjaxResult result = controller.preUploadTask(2L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        Map<?, ?> summary = (Map<?, ?>) data.get("summary");
        assertEquals(2, summary.get("flawCount"));
        assertEquals(1L, summary.get("unconfirmedCount"));
        assertEquals(1L, summary.get("notUploadedFlawCount"));
        assertEquals(false, summary.get("canUpload"));
    }

    @Test
    void uploadTaskShouldHandleMissingWrongStatusAndCloudFailure() {
        when(agvTaskMapper.selectById(1L)).thenReturn(null);
        assertEquals("任务不存在", controller.uploadTask(1L).getMsg());

        AgvTask wrongStatus = new AgvTask();
        wrongStatus.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(2L)).thenReturn(wrongStatus);
        assertEquals("只有待上传任务可以上传", controller.uploadTask(2L).getMsg());

        AgvTask waitingUpload = new AgvTask();
        waitingUpload.setTaskStatus("待上传");
        when(agvTaskMapper.selectById(3L)).thenReturn(waitingUpload);
        AgvFlaw confirmed = new AgvFlaw();
        confirmed.setConfirmed(1);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(confirmed));
        when(agvUploadRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());
        when(cloudUploadService.uploadTaskData(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("云端失败"));
        AjaxResult cloudFail = controller.uploadTask(3L);
        assertEquals(500, cloudFail.getCode());
        assertEquals("云端失败", cloudFail.getMsg());
        verify(agvTaskMapper, never()).updateById(waitingUpload);
    }

    @Test
    void uploadTaskShouldUpdateExistingUploadRecords() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("待上传");
        task.setTaskCode("TASK-1");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvFlaw confirmed = new AgvFlaw();
        confirmed.setConfirmed(1);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(confirmed));
        AgvUploadRecord record = new AgvUploadRecord();
        record.setId(5L);
        when(agvUploadRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());
        when(cloudUploadService.uploadTaskData(any(), any(), any(), any(), any()))
                .thenReturn(CloudUploadService.UploadResult.localOnly("本地完成"));

        AjaxResult result = controller.uploadTask(1L);

        assertEquals(200, result.getCode());
        assertEquals("已完成", task.getTaskStatus());
        assertEquals(1, task.getUploaded());
        assertEquals("已上传", record.getStatus());
        assertEquals(100, record.getProgress());
        assertEquals("本地完成", record.getUploadResult());
        verify(agvUploadRecordMapper).updateById(record);
    }

    @Test
    void updateTaskShouldValidateIdExistenceAndSuccess() {
        assertEquals("任务ID不能为空", controller.updateTask(new AgvTask()).getMsg());

        AgvTask update = new AgvTask();
        update.setId(1L);
        when(agvTaskMapper.selectById(1L)).thenReturn(null);
        assertEquals("任务不存在", controller.updateTask(update).getMsg());

        AgvTask existing = new AgvTask();
        existing.setTaskStatus("待巡视");
        when(agvTaskMapper.selectById(2L)).thenReturn(existing, update);
        update.setId(2L);
        AjaxResult result = controller.updateTask(update);
        assertEquals(200, result.getCode());
        verify(agvTaskMapper).updateById(update);
        assertSame(update, result.getData());
    }

    @Test
    void deleteTaskShouldHandleDeletedZeroRowsAndSuccess() {
        AgvTask deleted = new AgvTask();
        deleted.setDeleteFlag(1);
        when(agvTaskMapper.selectById(1L)).thenReturn(deleted);
        assertEquals("任务不存在", controller.delTask(1L).getMsg());

        AgvTask waiting = new AgvTask();
        waiting.setTaskStatus("待巡视");
        when(agvTaskMapper.selectById(2L)).thenReturn(waiting);
        when(agvTaskMapper.deleteById(2L)).thenReturn(0);
        assertEquals("任务不存在", controller.delTask(2L).getMsg());

        AgvTask waiting2 = new AgvTask();
        waiting2.setTaskStatus("待巡视");
        when(agvTaskMapper.selectById(3L)).thenReturn(waiting2);
        when(agvTaskMapper.deleteById(3L)).thenReturn(1);
        AjaxResult success = controller.delTask(3L);
        assertEquals(200, success.getCode());
        assertNull(success.getData());
    }

    @Test
    void uploadTaskShouldInsertTaskAndFlawUploadRecordsWithExpectedFields() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskCode("TASK-001");
        task.setTaskStatus("待上传");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);
        AgvFlaw flaw = new AgvFlaw();
        flaw.setFlawName("裂缝");
        flaw.setConfirmed(1);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));
        when(agvUploadRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());
        when(cloudUploadService.uploadTaskData(any(), any(), any(), any(), any()))
                .thenReturn(CloudUploadService.UploadResult.localOnly("本地完成"));

        controller.uploadTask(1L);

        ArgumentCaptor<AgvUploadRecord> captor = ArgumentCaptor.forClass(AgvUploadRecord.class);
        verify(agvUploadRecordMapper, times(2)).insert(captor.capture());
        List<AgvUploadRecord> records = captor.getAllValues();
        assertEquals("任务", records.get(0).getType());
        assertEquals("故障", records.get(1).getType());
        assertEquals(100, records.get(0).getProgress());
        assertEquals(0, records.get(0).getDeleteFlag());
    }
}
