package com.example.agv.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.dto.ai.AiChatRequest;
import com.example.agv.dto.ai.AiChatResponse;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvTaskMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 负责把当前 AGV 任务、故障、传感器和联动记录组织成 Dify 可理解的上下文。
 * 目标不是让 AI 复述页面数据，而是让 AI 做上传判断、复核建议、故障研判和备注生成。
 */
@Service
public class AgvAiService {

    @Resource
    private DifyService difyService;

    @Resource
    private AgvTaskMapper agvTaskMapper;

    @Resource
    private AgvFlawMapper agvFlawMapper;

    @Resource
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Resource
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    public AiChatResponse chat(AiChatRequest request) {
        if (request == null) {
            request = new AiChatRequest();
        }
        String query = buildInteractivePrompt(request);
        return difyService.chat(query, request.getConversationId());
    }

    public AiChatResponse taskReview(Long taskId, AiChatRequest request) {
        if (request == null) {
            request = new AiChatRequest();
        }
        request.setTaskId(taskId);
        String context = buildContext(taskId, request.getFlawId(), request.getContext());
        String query = """
                用户要求：
                请对当前 AGV 巡检任务进行 AI 辅助判断。不要只整理页面信息，要重点告诉运维人员下一步该怎么处理。

                当前任务上下文：
                %s

                回答要求：
                请按照“总体判断、关键风险、异常关联分析、下一步操作建议、系统处理建议”回答。若存在疑似裂缝或待确认故障，默认建议人工复核后再上传。
                """.formatted(context);
        return difyService.chat(query, request.getConversationId());
    }

    public AiChatResponse flawReview(Long flawId, AiChatRequest request) {
        if (request == null) {
            request = new AiChatRequest();
        }
        request.setFlawId(flawId);
        AgvFlaw flaw = agvFlawMapper.selectById(flawId);
        Long taskId = request.getTaskId();
        if (taskId == null && flaw != null) {
            taskId = flaw.getTaskId();
        }
        String context = buildContext(taskId, flawId, request.getContext());
        String query = """
                用户要求：
                请对当前选中的故障进行 AI 故障研判。重点判断它更像真实异常、疑似异常还是可能误报，并告诉我下一步如何处理。

                当前故障上下文：
                %s

                回答要求：
                请按照“故障判断、判断依据、误报可能、处理建议、建议备注”回答。建议备注要能直接复制到系统备注栏。
                """.formatted(context);
        return difyService.chat(query, request.getConversationId());
    }

    private String buildInteractivePrompt(AiChatRequest request) {
        String question = safe(request.getQuestion());
        if (question.isEmpty()) {
            question = "请根据当前任务状态给出处理建议。";
        }
        String context = buildContext(request.getTaskId(), request.getFlawId(), request.getContext());
        return """
                用户问题：
                %s

                当前页面上下文：
                %s

                回答要求：
                不要逐项复述页面字段。请像现场运维助手一样，优先给出“处理结论、关键原因、下一步操作”。
                如果用户要求生成备注，只输出一条可复制备注；如果用户追问为什么，要解释异常之间的关系。
                """.formatted(question, context);
    }

    private String buildContext(Long taskId, Long flawId, String extraContext) {
        StringBuilder sb = new StringBuilder();

        AgvTask task = taskId == null ? null : agvTaskMapper.selectById(taskId);
        AgvFlaw selectedFlaw = flawId == null ? null : agvFlawMapper.selectById(flawId);

        if (task == null && selectedFlaw != null && selectedFlaw.getTaskId() != null) {
            task = agvTaskMapper.selectById(selectedFlaw.getTaskId());
            taskId = selectedFlaw.getTaskId();
        }

        appendTask(sb, task);
        appendSummary(sb, taskId);
        appendSelectedFlaw(sb, selectedFlaw);
        appendFlaws(sb, taskId);
        appendSensorRecords(sb, taskId);
        appendIotActionRecords(sb, taskId);

        if (extraContext != null && !extraContext.trim().isEmpty()) {
            sb.append("\n前端补充上下文：\n").append(extraContext.trim()).append("\n");
        }
        return sb.toString().trim();
    }

    private void appendTask(StringBuilder sb, AgvTask task) {
        sb.append("任务信息：\n");
        if (task == null) {
            sb.append("- 未指定或未查询到任务。\n");
            return;
        }
        sb.append("- 任务ID：").append(value(task.getId())).append("\n");
        sb.append("- 任务编号：").append(value(task.getTaskCode())).append("\n");
        sb.append("- 任务名称：").append(value(task.getTaskName())).append("\n");
        sb.append("- 起始地点：").append(value(task.getStartPos())).append("\n");
        sb.append("- 巡检路线/距离：").append(value(task.getTaskTrip())).append("\n");
        sb.append("- 执行人：").append(value(task.getExecutor())).append("\n");
        sb.append("- 任务状态：").append(value(task.getTaskStatus())).append("\n");
        sb.append("- 是否已上传：").append(Integer.valueOf(1).equals(task.getUploaded()) ? "是" : "否").append("\n");
        sb.append("- 任务备注：").append(value(task.getRemark())).append("\n");
    }

    private void appendSummary(StringBuilder sb, Long taskId) {
        if (taskId == null) {
            return;
        }
        List<AgvFlaw> flaws = selectFlawsByTask(taskId, 100);
        long unconfirmedCount = flaws.stream().filter(f -> !Integer.valueOf(1).equals(f.getConfirmed())).count();
        long abnormalSensorCount = selectSensorRecordsByTask(taskId, 100).stream()
                .filter(s -> s.getStatus() != null && !"正常".equals(s.getStatus()) && !"normal".equalsIgnoreCase(s.getStatus()))
                .count();
        sb.append("\n任务风险概览：\n");
        sb.append("- 故障/异常记录数：").append(flaws.size()).append("\n");
        sb.append("- 待确认故障数：").append(unconfirmedCount).append("\n");
        sb.append("- 传感器异常记录数：").append(abnormalSensorCount).append("\n");
        sb.append("- 上传建议基础规则：存在待确认故障时，不建议直接上传，应先人工复核。\n");
    }

    private void appendSelectedFlaw(StringBuilder sb, AgvFlaw flaw) {
        sb.append("\n当前选中故障：\n");
        if (flaw == null) {
            sb.append("- 未选中具体故障。\n");
            return;
        }
        sb.append("- 故障ID：").append(value(flaw.getId())).append("\n");
        sb.append("- 类型：").append(value(flaw.getFlawType())).append("\n");
        sb.append("- 名称：").append(value(flaw.getFlawName())).append("\n");
        sb.append("- 描述：").append(value(flaw.getFlawDesc())).append("\n");
        sb.append("- 距离：").append(value(flaw.getFlawDistance())).append("\n");
        sb.append("- 等级：").append(value(flaw.getLevel())).append("\n");
        sb.append("- 来源：").append(value(flaw.getSource())).append("\n");
        sb.append("- 是否确认：").append(Integer.valueOf(1).equals(flaw.getConfirmed()) ? "已确认" : "待确认").append("\n");
        sb.append("- 是否上传：").append(Integer.valueOf(1).equals(flaw.getUploaded()) ? "已上传" : "未上传").append("\n");
        sb.append("- 备注：").append(value(flaw.getRemark())).append("\n");
    }

    private void appendFlaws(StringBuilder sb, Long taskId) {
        sb.append("\n故障列表（最多展示10条）：\n");
        if (taskId == null) {
            sb.append("- 未指定任务，无法查询故障列表。\n");
            return;
        }
        List<AgvFlaw> flaws = selectFlawsByTask(taskId, 10);
        if (flaws.isEmpty()) {
            sb.append("- 暂无故障记录。\n");
            return;
        }
        for (AgvFlaw flaw : flaws) {
            sb.append("- ")
                    .append(value(flaw.getFlawName()))
                    .append("；类型：").append(value(flaw.getFlawType()))
                    .append("；距离：").append(value(flaw.getFlawDistance()))
                    .append("；状态：").append(Integer.valueOf(1).equals(flaw.getConfirmed()) ? "已确认" : "待确认")
                    .append("；备注：").append(value(flaw.getRemark()))
                    .append("\n");
        }
    }

    private void appendSensorRecords(StringBuilder sb, Long taskId) {
        sb.append("\n传感器记录（最多展示10条）：\n");
        if (taskId == null) {
            sb.append("- 未指定任务，无法查询传感器记录。\n");
            return;
        }
        List<AgvSensorRecord> records = selectSensorRecordsByTask(taskId, 10);
        if (records.isEmpty()) {
            sb.append("- 暂无传感器记录。\n");
            return;
        }
        for (AgvSensorRecord record : records) {
            sb.append("- ")
                    .append(value(record.getSensorName()))
                    .append("；类型：").append(value(record.getSensorType()))
                    .append("；数值：").append(value(record.getSensorValue()))
                    .append("；状态：").append(value(record.getStatus()))
                    .append("；动作：").append(value(record.getAction()))
                    .append("；备注：").append(value(record.getRemark()))
                    .append("\n");
        }
    }

    private void appendIotActionRecords(StringBuilder sb, Long taskId) {
        sb.append("\nAIoT联动记录（最多展示10条）：\n");
        if (taskId == null) {
            sb.append("- 未指定任务，无法查询联动记录。\n");
            return;
        }
        List<AgvIotActionRecord> records = selectIotActionRecordsByTask(taskId, 10);
        if (records.isEmpty()) {
            sb.append("- 暂无联动记录。\n");
            return;
        }
        for (AgvIotActionRecord record : records) {
            sb.append("- 触发：").append(value(record.getTriggerType()))
                    .append("；设备：").append(value(record.getDeviceName()))
                    .append("；动作：").append(value(record.getAction()))
                    .append("；结果：").append(value(record.getResult()))
                    .append("；反馈：").append(value(record.getFeedback()))
                    .append("\n");
        }
    }

    private List<AgvFlaw> selectFlawsByTask(Long taskId, int limit) {
        QueryWrapper<AgvFlaw> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0).eq("task_id", taskId).orderByDesc("create_time").last("LIMIT " + limit);
        return agvFlawMapper.selectList(wrapper);
    }

    private List<AgvSensorRecord> selectSensorRecordsByTask(Long taskId, int limit) {
        QueryWrapper<AgvSensorRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0).eq("task_id", taskId).orderByDesc("create_time").last("LIMIT " + limit);
        return agvSensorRecordMapper.selectList(wrapper);
    }

    private List<AgvIotActionRecord> selectIotActionRecordsByTask(Long taskId, int limit) {
        QueryWrapper<AgvIotActionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0).eq("task_id", taskId).orderByDesc("create_time").last("LIMIT " + limit);
        return agvIotActionRecordMapper.selectList(wrapper);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String value(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty() ? "-" : String.valueOf(value);
    }
}
