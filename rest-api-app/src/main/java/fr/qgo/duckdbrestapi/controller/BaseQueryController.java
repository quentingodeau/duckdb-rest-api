package fr.qgo.duckdbrestapi.controller;

import fr.qgo.duckdbrestapi.service.RequestExecutionService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class BaseQueryController {
    protected final RequestExecutionService requestExecutionService;
}
