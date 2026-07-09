package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.service.AgvMovementStateService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/agv/movement")
@CrossOrigin
public class AgvMovementController {

    @Resource
    private AgvMovementStateService agvMovementStateService;

    @GetMapping("/heartbeat")
    public AjaxResult heartbeat() {
        try {
            return AjaxResult.success(agvMovementStateService.heartbeat());
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/forward")
    public AjaxResult forward() {
        try {
            return AjaxResult.success(agvMovementStateService.forward());
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/stop")
    public AjaxResult stop() {
        try {
            return AjaxResult.success(agvMovementStateService.stop());
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/backward")
    public AjaxResult backward() {
        try {
            return AjaxResult.success(agvMovementStateService.backward());
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
