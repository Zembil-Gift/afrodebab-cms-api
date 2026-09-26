package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Interview;
import jakarta.validation.constraints.NotNull;

public record InterviewStatusRequest(@NotNull Interview.Status status) {}
